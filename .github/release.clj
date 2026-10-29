#!/usr/bin/env bb
(ns release
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [release-github :as github]))

(def ^:dynamic *repo-dir* (str (fs/parent (fs/parent *file*))))
(def stable-tag-pattern #"^v(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)$")
(def test-tag-pattern #"^v((?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*))-test\.(.+)$")
(def test-sha-pattern #"[0-9a-f]{7,40}")
(def general-tag-message "Tag must match vX.Y.Z or vX.Y.Z-test.<hex-sha>; got ")
(def sha-format-message "Test tag SHA must be 7-40 lowercase hex characters; got ")
(def supported-bumps #{"patch" "minor" "major"})

(defn gradle-properties-path []
  (fs/path *repo-dir* "gradle.properties"))

(defn fail [message]
  (throw (ex-info message {})))

(defn process-message [result]
  (let [stderr (or (:err result) "")
        stdout (or (:out result) "")]
    (if (seq stderr) stderr stdout)))

(defn sh
  [& args]
  (let [result (apply shell {:dir *repo-dir* :out :string :err :string :continue true} args)]
    (if (zero? (:exit result))
      (str/trimr (or (:out result) ""))
      (fail (str (process-message result) "\ncommand failed: " (str/join " " args))))))

(defn split-lines [content]
  (vec (str/split-lines content)))

(defn ensure-clean-worktree []
  (when (seq (sh "git" "status" "--short"))
    (fail "Release flow requires a clean git worktree.")))

(defn ensure-tag-missing [tag]
  (when (seq (sh "git" "tag" "--list" tag))
    (fail (str "Tag already exists: " tag))))

(defn update-plugin-version [version]
  (let [path (gradle-properties-path)
        content (slurp (str path))
        updated (str/replace content
                             #"(?m)^pluginVersion\s*=\s*.*$"
                             (str "pluginVersion = " version))]
    (when (= content updated)
      (fail "Could not update pluginVersion in gradle.properties."))
    (spit (str path) updated)))

(defn stable-release-tag-name? [tag]
  (boolean (and (string? tag) (re-matches stable-tag-pattern tag))))

(defn tag-ref-exists? [tag]
  (let [ref (str "refs/tags/" tag)
        result (shell {:dir *repo-dir* :out :string :err :string :continue true}
                      "git" "rev-parse" "--verify" ref)]
    (zero? (:exit result))))

(defn test-default-notes [version]
  (str "Test release " version))

(defn tag-test-release [version tag-message]
  (sh "git" "tag" "-a" (str "v" version) "-m" tag-message))

(defn short-sha []
  (sh "git" "rev-parse" "--short=7" "HEAD"))

(defn base-plugin-version []
  (or (second (re-find #"(?m)^pluginVersion\s*=\s*(.+)$"
                       (slurp (str (gradle-properties-path)))))
      (fail "Could not read pluginVersion from gradle.properties.")))

(defn parse-version [version]
  (if-let [[_ major minor patch] (re-matches #"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)" version)]
    [(Long/parseLong major) (Long/parseLong minor) (Long/parseLong patch)]
    (fail (str "Expected base plugin version in x.y.z format, got " version "."))))

(defn increment-version [version bump]
  (let [[major minor patch] (parse-version version)]
    (case bump
      "major" (str (inc major) ".0.0")
      "minor" (str major "." (inc minor) ".0")
      "patch" (str major "." minor "." (inc patch))
      (fail "Expected bump in patch|minor|major format."))))

(defn tag-classification [tag]
  (if-let [[_ base sha] (and (string? tag) (re-matches test-tag-pattern tag))]
    (if (re-matches test-sha-pattern sha)
      {:kind :test
       :version (str base "-test." sha)
       :base-version base
       :sha sha}
      {:error (str sha-format-message tag ".")})
    (if (and (string? tag) (re-matches stable-tag-pattern tag))
      {:kind :stable :version (subs tag 1)}
      {:error (str general-tag-message tag ".")})))

(defn classify-tag [tag]
  (let [classification (tag-classification tag)]
    (if-let [message (:error classification)]
      (fail message)
      classification)))

(defn version-tuple [version]
  (if (string? version) (parse-version version) version))

(defn compare-versions [a b]
  (Integer/signum (compare (version-tuple a) (version-tuple b))))

(defn parse-releases [json-text]
  (let [parsed (json/parse-string json-text true)
        pages (if (map? parsed) [parsed] parsed)]
    (vec (mapcat #(if (sequential? %) % [%]) pages))))

(defn stable-release? [release]
  (and (not (:draft release))
       (not (:prerelease release))
       (stable-release-tag-name? (:tag_name release))))

(defn test-release? [release]
  (and (not (:draft release))
       (boolean (:prerelease release))
       (= :test (:kind (tag-classification (:tag_name release))))))

(defn highest-stable-release [releases & {:keys [exclude]}]
  (->> releases
       (filter stable-release?)
       (remove #(= exclude (:tag_name %)))
       (sort-by #(subs (:tag_name %) 1) compare-versions)
       last))

(defn tag-ref [tag]
  (str "refs/tags/" tag))

(defn tag-object-type [tag]
  (sh "git" "cat-file" "-t" (tag-ref tag)))

(defn tag-commit [tag]
  (let [result (shell {:dir *repo-dir* :out :string :err :string :continue true}
                      "git" "rev-parse" "--verify" "--quiet" (str (tag-ref tag) "^{commit}"))]
    (when (zero? (:exit result))
      (str/trim (or (:out result) "")))))

(defn commit-ancestor? [ancestor descendant]
  (let [result (shell {:dir *repo-dir* :out :string :err :string :continue true}
                      "git" "merge-base" "--is-ancestor" ancestor descendant)]
    (case (:exit result)
      0 true
      1 false
      (fail (str (process-message result)
                 "\ncommand failed: git merge-base --is-ancestor " ancestor " " descendant)))))

(defn release-version [release]
  (subs (:tag_name release) 1))

(defn base-version-of [classification]
  (or (:base-version classification) (:version classification)))

(defn stable-tag? [{:keys [classification]}]
  (= :stable (:kind classification)))

(defn test-tag? [{:keys [classification]}]
  (= :test (:kind classification)))

(defn release-exists? [releases tag]
  (boolean (some #(and (not (:draft %)) (= tag (:tag_name %))) releases)))

(defn missing-tag-message [tag]
  (str "Tag does not exist: " tag "."))

(defn check-annotated [{:keys [tag annotated?]}]
  (when-not annotated?
    (str "Release tag " tag " must be annotated.")))

(defn check-points-to-commit [{:keys [tag commit]}]
  (when-not commit
    (str "Tag " tag " does not point to a commit.")))

(defn check-dispatch-ref [{:keys [event run-ref]}]
  (when (and (= "workflow_dispatch" event) (not= "refs/heads/master" run-ref))
    (str "Manual runs must be dispatched from refs/heads/master; got " run-ref ".")))

(defn check-on-master [{:keys [tag commit master-ref ancestor?] :as context}]
  (when (and (stable-tag? context) commit (not (ancestor? commit master-ref)))
    (str "Stable tag " tag " must point to a commit on " master-ref ".")))

(defn check-stable-ordering [{:keys [tag releases classification] :as context}]
  (when (and (stable-tag? context) (not (release-exists? releases tag)))
    (when-let [highest (highest-stable-release releases :exclude tag)]
      (when-not (pos? (compare-versions (:version classification) (release-version highest)))
        (str "Stable version " (:version classification)
             " must be newer than the highest stable release " (:tag_name highest) ".")))))

(defn check-sha-prefix [{:keys [commit classification] :as context}]
  (when (and (test-tag? context) commit (not (str/starts-with? commit (:sha classification))))
    (str "Test tag SHA " (:sha classification) " does not identify tagged commit " commit ".")))

(defn check-test-base [{:keys [releases classification] :as context}]
  (when (test-tag? context)
    (when-let [highest (highest-stable-release releases)]
      (let [base (:base-version classification)
            current (release-version highest)]
        (when-not (some #(= base (increment-version current %)) supported-bumps)
          (str "Test base version " base " is not the next patch, minor, or major version after "
               current "."))))))

(def admission-checks
  [check-annotated
   check-points-to-commit
   check-dispatch-ref
   check-on-master
   check-stable-ordering
   check-sha-prefix
   check-test-base])

(defn admission-errors [{:keys [tag exists?] :as context}]
  (if exists?
    (vec (keep #(% context) admission-checks))
    [(missing-tag-message tag)]))

(defn previous-stable-release [{:keys [releases commit ancestor? tag-commit]} base-version]
  (->> releases
       (filter stable-release?)
       (filter #(neg? (compare-versions (release-version %) base-version)))
       (sort-by release-version #(compare-versions %2 %1))
       (some (fn [release]
               (when-let [release-commit (tag-commit (:tag_name release))]
                 (when (ancestor? release-commit commit)
                   release))))))

(defn previous-test-release [{:keys [tag releases commit ancestor? tag-commit]} base-version]
  (->> releases
       (filter test-release?)
       (remove #(= tag (:tag_name %)))
       (filter #(= base-version (:base-version (classify-tag (:tag_name %)))))
       (sort-by :created_at #(compare %2 %1))
       (some (fn [release]
               (let [{:keys [sha]} (classify-tag (:tag_name release))
                     release-commit (tag-commit (:tag_name release))]
                 (when (and release-commit
                            (str/starts-with? release-commit sha)
                            (ancestor? release-commit commit))
                   release))))))

(defn notes-start-tag [context base-version previous-tag]
  (or (when (test-tag? context)
        (:tag_name (previous-test-release context base-version)))
      previous-tag))

(defn metadata-outputs [{:keys [tag commit classification] :as context}]
  (let [base-version (base-version-of classification)
        previous-tag (or (:tag_name (previous-stable-release context base-version)) "")]
    [["tag" tag]
     ["version" (:version classification)]
     ["is_test" (str (test-tag? context))]
     ["notes_start_tag" (notes-start-tag context base-version previous-tag)]
     ["stable_tag" previous-tag]
     ["sha" commit]]))

(defn decide-metadata [facts]
  (let [context (assoc facts :classification (classify-tag (:tag facts)))
        errors (admission-errors context)]
    (if (seq errors)
      {:errors errors}
      {:outputs (metadata-outputs context)})))

(defn gather-metadata-facts [tag event run-ref master-ref releases]
  (let [exists? (tag-ref-exists? tag)]
    (cond-> {:tag tag
             :event event
             :run-ref run-ref
             :master-ref master-ref
             :exists? exists?
             :releases releases
             :ancestor? commit-ancestor?
             :tag-commit tag-commit}
      exists? (assoc :annotated? (= "tag" (tag-object-type tag))
                     :commit (tag-commit tag)))))

(def metadata-usage
  "Usage: bb --classpath .github -m release metadata <tag> <event> <run-ref> <master-ref> <server-url> <repository> < releases.json")

(defn metadata [& [tag event run-ref master-ref _server-url _repository]]
  (when (some str/blank? [tag event run-ref master-ref])
    (fail metadata-usage))
  (classify-tag tag)
  (let [releases (parse-releases (slurp *in*))
        decision (decide-metadata (gather-metadata-facts tag event run-ref master-ref releases))]
    (if (seq (:errors decision))
      (fail (str/join "\n" (:errors decision)))
      (doseq [[key value] (:outputs decision)]
        (print (str key "=" value "\n"))))))

(def test-gate-usage
  "Usage: bb --classpath .github -m release test-gate <sha> < build-runs.json")

(def marketplace-outcome-usage
  "Usage: bb --classpath .github -m release marketplace-outcome <exit-code> <log-file>")

(def promotion-usage
  "Usage: bb --classpath .github -m release promotion <tag> [<latest-tag>]")

(def already-published-marker "already contains version")
(def log-tail-lines 20)

(defn successful-run? [run]
  (and (= "completed" (:status run))
       (= "success" (:conclusion run))))

(defn parse-runs [json-text]
  (let [parsed (try
                 (json/parse-string json-text true)
                 (catch Exception _
                   (fail (str "Build runs must be a JSON array. " test-gate-usage))))]
    (if (sequential? parsed)
      (vec parsed)
      (fail (str "Build runs must be a JSON array. " test-gate-usage)))))

(defn run-conclusions [runs]
  (->> runs
       (map #(or (:conclusion %) (:status %) "unknown"))
       (str/join ", ")))

(defn gate-failure-message [sha runs]
  (cond
    (empty? runs)
    (str "No Build run found for commit " sha ". Stable tags belong on a merge commit that has a Build run, "
         "not on the bot commit.")

    (some #(not= "completed" (:status %)) runs)
    (str "Build is still running for commit " sha ". Re-run the release once Build finishes.")

    :else
    (str "Build did not succeed for commit " sha " (" (run-conclusions runs) "). "
         "Re-run Build until it passes, then re-run the release.")))

(defn decide-test-gate [sha runs]
  (if-let [run (first (filter successful-run? runs))]
    {:url (:html_url run)}
    {:error (gate-failure-message sha runs)}))

(defn test-gate [& [sha]]
  (when (str/blank? sha)
    (fail test-gate-usage))
  (let [decision (decide-test-gate sha (parse-runs (slurp *in*)))]
    (if-let [message (:error decision)]
      (fail message)
      (print (str (:url decision) "\n")))))

(defn log-tail [text]
  (->> (split-lines text)
       (take-last log-tail-lines)
       (str/join "\n")))

(defn marketplace-outcome-for [exit-code log-text]
  (cond
    (zero? exit-code) {:outcome "published"}
    (str/includes? log-text already-published-marker) {:outcome "already-published"}
    :else {:error (str "Marketplace publish failed with exit code " exit-code ".\n" (log-tail log-text))}))

(defn parse-exit-code [value]
  (if (and (string? value) (re-matches #"-?\d+" value))
    (Long/parseLong value)
    (fail marketplace-outcome-usage)))

(defn marketplace-outcome [& [exit-code log-file]]
  (when (or (str/blank? exit-code) (str/blank? log-file))
    (fail marketplace-outcome-usage))
  (let [code (parse-exit-code exit-code)
        log-text (if (fs/exists? log-file) (slurp log-file) "")
        decision (marketplace-outcome-for code log-text)]
    (if-let [message (:error decision)]
      (fail message)
      (print (str (:outcome decision) "\n")))))

(defn require-stable-tag [tag]
  (when-not (stable-release-tag-name? tag)
    (fail (str "Promotion requires a stable tag vX.Y.Z; got " tag "."))))

(defn promotion-action [tag latest-tag]
  (require-stable-tag tag)
  (if (str/blank? latest-tag)
    "promote"
    (do
      (require-stable-tag latest-tag)
      (case (compare-versions (subs tag 1) (subs latest-tag 1))
        1 "promote"
        0 "unchanged"
        "superseded"))))

(defn promotion [& [tag latest-tag]]
  (when (str/blank? tag)
    (fail promotion-usage))
  (print (str (promotion-action tag latest-tag) "\n")))

(def record-version-usage
  "Usage: bb --classpath .github -m release record-version <version>")

(def record-version-max-attempts 5)
(def bot-name "github-actions[bot]")
(def bot-email "41898282+github-actions[bot]@users.noreply.github.com")

(defn record-version-message [version]
  (str "chore(release): set pluginVersion to " version))

(defn ensure-only-version-file-changed [changed-files-text]
  (let [changed (vec (remove str/blank? (map str/trim (str/split-lines (or changed-files-text "")))))]
    (when-not (= ["gradle.properties"] changed)
      (fail (str "Expected only gradle.properties to change, got: "
                 (if (seq changed) (str/join ", " changed) "no changes") ".")))))

(defn push-master []
  (sh "git" "push" "origin" "HEAD:refs/heads/master"))

(defn commit-recorded-version [version]
  (update-plugin-version version)
  (ensure-only-version-file-changed (sh "git" "diff" "--name-only"))
  (sh "git" "add" "gradle.properties")
  (sh "git" "-c" (str "user.name=" bot-name) "-c" (str "user.email=" bot-email)
      "commit" "-m" (record-version-message version)))

(defn checkout-master-tip []
  (sh "git" "fetch" "origin" "master")
  (sh "git" "checkout" "-B" "master" "origin/master"))

(defn push-recorded-version [version]
  (try
    (push-master)
    true
    (catch clojure.lang.ExceptionInfo error
      (binding [*out* *err*]
        (println (str "Push of pluginVersion " version " failed: " (ex-message error))))
      false)))

(defn record-version-attempt [version]
  (checkout-master-tip)
  (let [current (base-plugin-version)]
    (cond
      (not (neg? (compare-versions current version)))
      (do (println (str "pluginVersion on master is " current ", already at or above " version
                        "; nothing to record."))
          :done)

      (do (commit-recorded-version version)
          (push-recorded-version version))
      (do (println (str "Recorded pluginVersion " version " on master."))
          :done)

      :else :retry)))

(defn recorded-within-attempts? [version]
  (some (fn [_] (= :done (record-version-attempt version)))
        (range record-version-max-attempts)))

(defn record-version [& [version]]
  (when (str/blank? version)
    (fail record-version-usage))
  (parse-version version)
  (when-not (recorded-within-attempts? version)
    (fail (str "Could not record pluginVersion " version " on master after "
               record-version-max-attempts " attempts."))))

(defn test-bump [bump]
  (let [value (if (str/blank? bump) "patch" bump)]
    (if (contains? supported-bumps value)
      value
      (fail "Usage: make test-release [BUMP=patch|minor|major]"))))

(def releases-api-path "repos/{owner}/{repo}/releases?per_page=100")

(defn published-releases []
  (parse-releases (sh "gh" "api" "--paginate" "--slurp" releases-api-path)))

(defn test-release-base-version []
  (if-let [release (highest-stable-release (published-releases))]
    (release-version release)
    (fail "No published stable GitHub release found to base the test release on.")))

(defn test-release [bump]
  (let [requested-bump (test-bump bump)]
    (ensure-clean-worktree)
    (let [version (str (increment-version (test-release-base-version) requested-bump)
                       "-test."
                       (short-sha))
          tag (str "v" version)]
      (ensure-tag-missing tag)
      (tag-test-release version (test-default-notes version))
      (println (str "Created test release tag " tag "."))
      (println (str "Push with: git push origin " tag)))))

(def commands
  {"test" (fn [& [bump]] (test-release bump))
   "metadata" metadata
   "compose-notes" github/compose-notes
   "missing-assets" github/missing-assets
   "test-gate" test-gate
   "marketplace-outcome" marketplace-outcome
   "promotion" promotion
   "record-version" record-version})

(defn usage-message []
  (str "Usage: bb --classpath .github -m release <"
       (str/join "|" (sort (keys commands)))
       "> [args...]
"
       "       make test-release [BUMP=patch|minor|major]"))

(defn dispatch [[command & args]]
  (if-let [handler (get commands command)]
    (apply handler args)
    (fail (usage-message))))

(defn -main [& args]
  (try
    (dispatch args)
    (catch Exception error
      (binding [*out* *err*]
        (println (or (ex-message error) (str error))))
      (System/exit 1))))
