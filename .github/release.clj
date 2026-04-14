#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]]
         '[clojure.string :as str])

(def repo-dir (str (fs/parent (fs/parent *file*))))
(def changelog-path (fs/path repo-dir "CHANGELOG.md"))
(def gradle-properties-path (fs/path repo-dir "gradle.properties"))
(def section-header-pattern #"^(##|###) \[")

(defn fail [message]
  (binding [*out* *err*]
    (println message))
  (System/exit 1))

(defn process-message [result]
  (let [stderr (or (:err result) "")
        stdout (or (:out result) "")]
    (if (seq stderr) stderr stdout)))

(defn sh
  [& args]
  (let [result (apply shell {:dir repo-dir :out :string :err :string :continue true} args)]
    (if (zero? (:exit result))
      (str/trimr (or (:out result) ""))
      (fail (str (process-message result) "\ncommand failed: " (str/join " " args))))))

(defn command-available? [command]
  (zero? (:exit (shell {:dir repo-dir :out :string :err :string :continue true} command "--version"))))

(defn trim-blank-lines [lines]
  (->> lines
       (drop-while str/blank?)
       reverse
       (drop-while str/blank?)
       reverse
       vec))

(defn split-lines [content]
  (vec (str/split-lines content)))

(defn join-lines [lines]
  (str (str/join "\n" lines) "\n"))

(defn current-date []
  (str (java.time.LocalDate/now)))

(defn next-section-index [lines start-index]
  (or (some (fn [idx]
              (when (re-find section-header-pattern (nth lines idx))
                idx))
            (range (inc start-index) (count lines)))
      (count lines)))

(defn find-section [lines header-pattern]
  (when-let [start-index (first (keep-indexed (fn [idx line]
                                                (when (re-find header-pattern line)
                                                  idx))
                                              lines))]
    (let [end-index (next-section-index lines start-index)]
      {:start-index start-index
       :end-index end-index
       :body-lines (subvec lines (inc start-index) end-index)})))

(defn changelog-lines []
  (split-lines (slurp (str changelog-path))))

(defn unreleased-section []
  (or (find-section (changelog-lines) #"^## \[Unreleased\]\s*$")
      (fail "CHANGELOG.md is missing the [Unreleased] section.")))

(defn version-section [version]
  (or (find-section (changelog-lines)
                    (re-pattern (str "^### \\[" (java.util.regex.Pattern/quote version) "\\](?: - .+)?$")))
      (fail (str "CHANGELOG.md is missing the section for " version "."))))

(defn section-body [section]
  (trim-blank-lines (:body-lines section)))

(defn section-text [section]
  (let [body (section-body section)]
    (if (seq body)
      (join-lines body)
      "")))

(defn ensure-clean-worktree []
  (when (seq (sh "git" "status" "--short"))
    (fail "Release flow requires a clean git worktree.")))

(defn ensure-tag-missing [tag]
  (when (seq (sh "git" "tag" "--list" tag))
    (fail (str "Tag already exists: " tag))))

(defn update-plugin-version [version]
  (let [updated (str/replace (slurp (str gradle-properties-path))
                             #"(?m)^pluginVersion\s*=\s*.*$"
                             (str "pluginVersion = " version))]
    (spit (str gradle-properties-path) updated)))

(defn changelog-content []
  (slurp (str changelog-path)))

(defn restore-changelog [content]
  (spit (str changelog-path) content))

(defn replace-unreleased [release-lines]
  (let [lines (changelog-lines)
        {:keys [start-index end-index]} (unreleased-section)
        prefix (subvec lines 0 start-index)
        suffix (vec (drop-while str/blank? (subvec lines end-index)))
        new-lines (vec (concat prefix
                               ["## [Unreleased]" ""]
                               release-lines
                               (when (seq release-lines) [""])
                               suffix))]
    (spit (str changelog-path) (join-lines new-lines))))

(defn test-release-tag-name? [tag]
  (boolean (re-find #"-test\." tag)))

(defn latest-non-test-tag []
  (let [result (shell {:dir repo-dir :out :string :err :string :continue true}
                      "git" "tag" "--merged" "HEAD" "--sort=-creatordate")]
    (when (zero? (:exit result))
      (->> (split-lines (str/trim (or (:out result) "")))
           (remove str/blank?)
           (remove test-release-tag-name?)
           first))))

(defn tag-ref-exists? [tag]
  (let [ref (str "refs/tags/" tag)
        result (shell {:dir repo-dir :out :string :err :string :continue true}
                      "git" "rev-parse" "--verify" ref)]
    (zero? (:exit result))))

(defn commit-log-range-arg []
  (when-let [tag (not-empty (str/trim (or (latest-non-test-tag) "")))]
    (when (tag-ref-exists? tag)
      (str "refs/tags/" tag "..HEAD"))))

(defn commit-subjects []
  (let [range-arg (commit-log-range-arg)
        result (if range-arg
                 (shell {:dir repo-dir :out :string :err :string :continue true}
                        "git" "log" "--pretty=format:%s" range-arg)
                 (shell {:dir repo-dir :out :string :err :string :continue true}
                        "git" "log" "--pretty=format:%s"))]
    (if (zero? (:exit result))
      (remove str/blank? (split-lines (or (:out result) "")))
      (fail (str (process-message result) "\ncommand failed: git log --pretty=format:%s")))))

(defn empty-groups []
  {:Added []
   :Fixed []
   :Changed []})

(defn add-entry [groups key value]
  (update groups key conj (str "- " value)))

(defn group-commit-subjects [subjects]
  (reduce
    (fn [groups subject]
      (cond
        (or (str/blank? subject)
            (str/starts-with? subject "Merge"))
        groups

        (re-matches #"^chore(\(.+?\))?:.*" subject)
        groups

        :else
        (if-let [[_ kind _ text] (re-matches #"^(feat|fix|docs|refactor|style|perf|build|ci|test)(\(.+?\))?:\s*(.+)$" subject)]
          (case kind
            "feat" (add-entry groups :Added text)
            "fix" (add-entry groups :Fixed text)
            (add-entry groups :Changed text))
          (add-entry groups :Changed subject))))
    (empty-groups)
    subjects))

(defn group-lines [title entries]
  (when (seq entries)
    (into [(str "- **" title "**")]
          (map #(str "  " %) entries))))

(defn generated-release-lines [version]
  (let [groups (group-commit-subjects (commit-subjects))
        sections (keep identity
                       [(group-lines "Added" (:Added groups))
                        (group-lines "Fixed" (:Fixed groups))
                        (group-lines "Changed" (:Changed groups))])
        body-lines (vec (mapcat (fn [idx section]
                                  (if (zero? idx)
                                    section
                                    (concat [""] section)))
                                (range)
                                sections))]
    (vec (concat [(str "### [" version "] - " (current-date))]
                 body-lines))))

(defn default-notes [version]
  (str "Release " version))

(defn test-default-notes [version]
  (str "Test release " version))

(defn shell-quote [value]
  (if (fs/windows?)
    (str "\"" (str/replace value "\"" "\\\"") "\"")
    (str "'" (str/replace value "'" "'\"'\"'") "'")))

(defn open-in-editor [path]
  (let [editor (or (System/getenv "VISUAL")
                   (System/getenv "EDITOR")
                   (when (command-available? "code") "code --wait"))]
    (when-not editor
      (fail "Set VISUAL or EDITOR, or install `code` on PATH, before running the release command."))
    (let [result (shell {:dir repo-dir :continue true :inherit true :shell true}
                        (str editor " " (shell-quote (str path))))]
      (when-not (zero? (:exit result))
        (fail "Editor command failed.")))))

(defn continue-after-edit? []
  (println "Edit the changelog now, then press Enter to continue (or 'q' to abort)")
  (flush)
  (not= "q" (str/trim (or (read-line) ""))))

(defn commit-and-tag [version tag-message]
  (sh "git" "add" "--" (str gradle-properties-path) (str changelog-path))
  (sh "git" "commit" "-m" (str "chore(release): v" version))
  (sh "git" "tag" "-a" (str "v" version) "-m" tag-message))

(defn tag-test-release [version tag-message]
  (sh "git" "tag" "-a" (str "v" version) "-m" tag-message))

(defn short-sha []
  (sh "git" "rev-parse" "--short" "HEAD"))

(defn base-plugin-version []
  (or (second (re-find #"(?m)^pluginVersion\s*=\s*(.+)$" (slurp (str gradle-properties-path))))
      (fail "Could not read pluginVersion from gradle.properties.")))

(defn parse-version [version]
  (if-let [[_ major minor patch] (re-matches #"(\d+)\.(\d+)\.(\d+)" version)]
    [(Long/parseLong major) (Long/parseLong minor) (Long/parseLong patch)]
    (fail (str "Expected base plugin version in x.y.z format, got " version "."))))

(defn increment-version [version bump]
  (let [[major minor patch] (parse-version version)]
    (case bump
      "major" (str (inc major) ".0.0")
      "minor" (str major "." (inc minor) ".0")
      "patch" (str major "." minor "." (inc patch))
      (fail "Usage: make bump-version BUMP=patch|minor|major"))))

(defn bump-version [bump]
  (let [version (increment-version (base-plugin-version) bump)]
    (update-plugin-version version)
    (println (str "Updated base plugin version to " version ".")))
  (println "Commit gradle.properties when you are ready."))

(defn stable-release []
  (ensure-clean-worktree)
  (let [version (base-plugin-version)
        original-changelog (changelog-content)]
    (ensure-tag-missing (str "v" version))
    (replace-unreleased (generated-release-lines version))
    (open-in-editor changelog-path)
    (if-not (continue-after-edit?)
      (do
        (restore-changelog original-changelog)
        (println "Aborted. Changelog reverted."))
      (let [notes (str/trim (section-text (version-section version)))
            tag-message (if (seq notes) notes (default-notes version))]
        (commit-and-tag version tag-message)
        (println (str "Created release commit and tag v" version "."))
        (println "Push with: git push --follow-tags")))))

(defn test-release []
  (ensure-clean-worktree)
  (let [version (str (base-plugin-version) "-test." (short-sha))
        tag (str "v" version)]
    (ensure-tag-missing tag)
    (let [tag-message (str/trim (section-text (unreleased-section)))]
      (tag-test-release version (if (seq tag-message) tag-message (test-default-notes version)))
      (println (str "Created test release tag " tag "."))
      (println "Push with: git push --follow-tags"))))

(defn print-notes [mode value]
  (case mode
    "unreleased" (print (section-text (unreleased-section)))
    "version" (print (section-text (version-section value)))
    (fail "Usage: bb .github/release.clj notes unreleased | notes version <version>")))

(let [[command arg1 arg2] *command-line-args*]
  (case command
    "bump-version" (if (seq arg1)
                     (bump-version arg1)
                     (fail "Usage: bb .github/release.clj bump-version <patch|minor|major>"))
    "stable" (stable-release)
    "test" (test-release)
    "notes" (print-notes arg1 arg2)
    (fail "Usage: bb .github/release.clj bump-version <patch|minor|major> | stable | test | notes unreleased | notes version <version>")))
