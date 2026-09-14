(ns release-test
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing use-fixtures]]
            [release :as sut]))

(def repositories (atom []))

(defn bash-path [path]
  (let [normalized (str/replace (str path) "\\" "/")]
    (if-let [[_ drive suffix] (re-matches #"(?i)^([a-z]):/(.*)$" normalized)]
      (str "/mnt/" (str/lower-case drive) "/" suffix)
      normalized)))

(def metadata-script
  (bash-path (fs/path (fs/parent *file*) "release_metadata.sh")))

(defn git
  [repo & args]
  (let [result (apply shell {:dir (str repo) :out :string :err :string :continue true}
                      "git" args)]
    (if (zero? (:exit result))
      (str/trim (or (:out result) ""))
      (throw (ex-info (or (:err result) "git command failed") result)))))

(defn write-project-files
  [repo version]
  (spit (str (fs/path repo "gradle.properties"))
        (str "pluginName = CodeScene\npluginVersion = " version "\n"))
  (spit (str (fs/path repo "CHANGELOG.md"))
        (str "# Changelog\n\n"
             "## [Unreleased]\n\n"
             "- Pending change\n\n"
             "### [" version "] - 2026-09-01\n"
             "- Previous release\n")))

(defn create-repository
  [version]
  (let [repo (fs/create-temp-dir {:prefix "codescene-release-test-"})]
    (swap! repositories conj repo)
    (write-project-files repo version)
    (git repo "init")
    (git repo "config" "user.email" "test@example.com")
    (git repo "config" "user.name" "Test User")
    (git repo "add" ".")
    (git repo "commit" "-m" "Initial release")
    (git repo "tag" "-a" (str "v" version) "-m" (str "Release " version))
    repo))

(defn with-repository
  [repo f]
  (binding [sut/*repo-dir* (str repo)]
    (f)))

(defn parse-output [content]
  (into {}
        (map #(str/split % #"=" 2))
        (remove str/blank? (str/split-lines content))))

(defn run-metadata
  [repo tag]
  (let [output-path (fs/create-temp-file {:prefix "codescene-release-output-"})
        result (shell {:dir (str repo)
                       :out :string
                       :err :string
                       :continue true}
                      "bash"
                      metadata-script
                      tag
                      (bash-path output-path)
                      "https://github.com"
                      "codescene/test")
        output (when (fs/exists? output-path)
                 (parse-output (slurp (str output-path))))]
    (fs/delete-if-exists output-path)
    (assoc result :metadata output)))

(defn create-annotated-test-tag [repo version message]
  (let [tag (str "v" version "-test." (git repo "rev-parse" "--short=7" "HEAD"))]
    (git repo "tag" "-a" tag "-m" message)
    tag))

(defn metadata-for [repo tag]
  (let [{:keys [exit metadata]} (run-metadata repo tag)]
    (is (zero? exit))
    metadata))

(use-fixtures
  :each
  (fn [test-fn]
    (try
      (test-fn)
      (finally
        (doseq [repo @repositories]
          (fs/delete-tree repo {:force true}))
        (reset! repositories [])))))

(deftest increments-supported-semantic-versions
  (is (= "1.2.4" (sut/increment-version "1.2.3" "patch")))
  (is (= "1.3.0" (sut/increment-version "1.2.3" "minor")))
  (is (= "2.0.0" (sut/increment-version "1.2.3" "major"))))

(deftest rejects-malformed-versions-and-bumps
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"x\.y\.z"
                        (sut/increment-version "1.2" "patch")))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"x\.y\.z"
                        (sut/increment-version "01.2.3" "patch")))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"patch\|minor\|major"
                        (sut/increment-version "1.2.3" "banana"))))

(deftest creates-default-patch-test-release-without-changing-files
  (let [repo (create-repository "1.2.3")
        properties-before (slurp (str (fs/path repo "gradle.properties")))
        changelog-before (slurp (str (fs/path repo "CHANGELOG.md")))]
    (git repo "config" "core.abbrev" "12")
    (let [sha (git repo "rev-parse" "--short=7" "HEAD")
          tag (str "v1.2.4-test." sha)]
      (with-repository repo #(sut/test-release nil))
      (is (= tag (git repo "tag" "--list" tag)))
      (is (= "tag" (git repo "cat-file" "-t" tag)))
      (is (= properties-before (slurp (str (fs/path repo "gradle.properties")))))
      (is (= changelog-before (slurp (str (fs/path repo "CHANGELOG.md")))))
      (is (= "" (git repo "status" "--short"))))))

(deftest creates-requested-minor-and-major-test-releases
  (doseq [[bump expected] [["minor" "1.3.0"] ["major" "2.0.0"]]]
    (testing bump
      (let [repo (create-repository "1.2.3")
            sha (git repo "rev-parse" "--short=7" "HEAD")]
        (with-repository repo #(sut/test-release bump))
        (is (= (str "v" expected "-test." sha)
               (git repo "tag" "--list" (str "v" expected "-test.*"))))))))

(deftest test-release-sha-has-at-least-seven-characters
  (let [repo (create-repository "1.2.3")]
    (git repo "config" "core.abbrev" "4")
    (with-repository repo #(sut/test-release nil))
    (is (re-matches #"v1\.2\.4-test\.[0-9a-f]{7,40}"
                    (git repo "tag" "--list" "v1.2.4-test.*")))))

(deftest rejects-test-release-from-dirty-worktree
  (let [repo (create-repository "1.2.3")]
    (spit (str (fs/path repo "dirty.txt")) "dirty")
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"clean git worktree"
                          (with-repository repo #(sut/test-release nil))))
    (is (= "" (git repo "tag" "--list" "v1.2.4-test.*")))))

(deftest rejects-duplicate-test-release-tag
  (let [repo (create-repository "1.2.3")]
    (with-repository repo #(sut/test-release nil))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Tag already exists"
                          (with-repository repo #(sut/test-release nil))))))

(deftest rejects-version-that-does-not-match-latest-stable-tag
  (let [repo (create-repository "1.2.3")]
    (write-project-files repo "1.2.4")
    (git repo "add" ".")
    (git repo "commit" "-m" "Move version ahead")
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"latest stable tag"
                          (with-repository repo #(sut/test-release nil))))))

(deftest stable-release-requires-an-explicit-bump
  (let [repo (create-repository "1.2.3")]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"make release BUMP=patch\|minor\|major"
                          (with-repository repo #(sut/stable-release nil))))))

(deftest aborted-stable-release-restores-version-and-changelog
  (let [repo (create-repository "1.2.3")
        properties-before (slurp (str (fs/path repo "gradle.properties")))
        changelog-before (slurp (str (fs/path repo "CHANGELOG.md")))]
    (with-redefs [sut/open-in-editor (constantly nil)
                  sut/continue-after-edit? (constantly false)]
      (with-repository repo #(sut/stable-release "minor")))
    (is (= properties-before (slurp (str (fs/path repo "gradle.properties")))))
    (is (= changelog-before (slurp (str (fs/path repo "CHANGELOG.md")))))
    (is (= "" (git repo "status" "--short")))
    (is (= "" (git repo "tag" "--list" "v1.3.0")))))

(deftest stable-release-updates-version-commits-and-tags
  (let [repo (create-repository "1.2.3")]
    (git repo "commit" "--allow-empty" "-m" "feat: add release support")
    (with-redefs [sut/open-in-editor (constantly nil)
                  sut/continue-after-edit? (constantly true)]
      (with-repository repo #(sut/stable-release "patch")))
    (is (str/includes? (slurp (str (fs/path repo "gradle.properties")))
                       "pluginVersion = 1.2.4"))
    (is (= "chore(release): v1.2.4" (git repo "log" "-1" "--pretty=%s")))
    (is (= "tag" (git repo "cat-file" "-t" "v1.2.4")))
    (is (= "" (git repo "status" "--short")))))

(deftest resolves-previous-stable-tag-for-stable-release-notes
  (let [repo (create-repository "1.2.3")]
    (write-project-files repo "1.2.4")
    (git repo "add" ".")
    (git repo "commit" "-m" "Release 1.2.4")
    (git repo "tag" "-a" "v1.2.4" "-m" "Release 1.2.4")
    (let [{:keys [exit metadata]} (run-metadata repo "v1.2.4")]
      (is (zero? exit))
      (is (= "1.2.4" (metadata "version")))
      (is (= "false" (metadata "is_test")))
      (is (= "v1.2.3" (metadata "notes_start_tag"))))))

(deftest rejects-stable-release-version-jumps
  (let [repo (create-repository "1.2.3")]
    (write-project-files repo "9.9.9")
    (git repo "add" ".")
    (git repo "commit" "-m" "Invalid release jump")
    (git repo "tag" "-a" "v9.9.9" "-m" "Invalid release")
    (let [{:keys [exit err]} (run-metadata repo "v9.9.9")]
      (is (not (zero? exit)))
      (is (str/includes? err "not the next patch, minor, or major version")))))

(deftest rejects-lightweight-release-tags
  (let [repo (create-repository "1.2.3")]
    (write-project-files repo "1.2.4")
    (git repo "add" ".")
    (git repo "commit" "-m" "Release 1.2.4")
    (git repo "tag" "v1.2.4")
    (let [{:keys [exit err]} (run-metadata repo "v1.2.4")]
      (is (not (zero? exit)))
      (is (str/includes? err "must be annotated")))))

(deftest rejects-leading-zero-release-tags
  (let [repo (create-repository "01.2.3")
        {:keys [exit err]} (run-metadata repo "v01.2.3")]
    (is (not (zero? exit)))
    (is (str/includes? err "must match vX.Y.Z"))))

(deftest resolves-latest-stable-tag-for-first-test-release
  (let [repo (create-repository "1.2.3")]
    (git repo "commit" "--allow-empty" "-m" "First test change")
    (let [tag (create-annotated-test-tag repo "1.2.4" "Test")
          metadata (metadata-for repo tag)]
        (is (= "true" (metadata "is_test")))
        (is (= "v1.2.3" (metadata "notes_start_tag")))
        (is (str/includes? (metadata "cumulative_notes")
                           (str "/compare/v1.2.3..." tag))))))

(deftest resolves-previous-same-base-test-tag-for-increments
  (let [repo (create-repository "1.2.3")]
    (git repo "commit" "--allow-empty" "-m" "First test change")
    (let [first-tag (create-annotated-test-tag repo "1.2.4" "First test")]
      (git repo "commit" "--allow-empty" "-m" "Second test change")
      (let [second-tag (create-annotated-test-tag repo "1.2.4" "Second test")
            metadata (metadata-for repo second-tag)]
        (is (= first-tag (metadata "notes_start_tag")))))))

(deftest ignores-invalid-annotated-tags-when-selecting-test-baseline
  (doseq [[label rejected-tag] [["malformed" "v1.2.4-test.invalid"]
                                ["wrong SHA" "v1.2.4-test.deadbee"]]]
    (testing label
      (let [repo (create-repository "1.2.3")]
        (git repo "commit" "--allow-empty" "-m" "Test change")
        (let [tag (create-annotated-test-tag repo "1.2.4" "Valid test")]
          (Thread/sleep 1100)
          (git repo "tag" "-a" rejected-tag "-m" "Rejected test")
          (is (= "v1.2.3" ((metadata-for repo tag) "notes_start_tag"))))))))

(deftest ignores-lightweight-tags-when-selecting-test-baseline
  (let [repo (create-repository "1.2.3")]
    (git repo "commit" "--allow-empty" "-m" "Rejected test")
    (let [rejected-tag (str "v1.2.4-test." (git repo "rev-parse" "--short=7" "HEAD"))]
      (git repo "tag" rejected-tag)
      (git repo "commit" "--allow-empty" "-m" "Valid test")
      (let [tag (create-annotated-test-tag repo "1.2.4" "Valid test")]
        (is (= "v1.2.3" ((metadata-for repo tag) "notes_start_tag")))))))

(deftest rejects-malformed-test-release-metadata
  (let [repo (create-repository "1.2.3")]
    (git repo "commit" "--allow-empty" "-m" "Test change")
    (git repo "tag" "-a" "v1.2.4-test.deadbee" "-m" "Wrong SHA")
    (let [{:keys [exit err]} (run-metadata repo "v1.2.4-test.deadbee")]
      (is (not (zero? exit)))
      (is (str/includes? err "does not identify tagged commit")))))

(deftest rejects-test-base-that-is-not-next-version
  (let [repo (create-repository "1.2.3")]
    (git repo "commit" "--allow-empty" "-m" "Test change")
    (let [tag (str "v1.2.5-test." (git repo "rev-parse" "--short=7" "HEAD"))]
      (git repo "tag" "-a" tag "-m" "Wrong base")
      (let [{:keys [exit err]} (run-metadata repo tag)]
        (is (not (zero? exit)))
        (is (str/includes? err "not the next patch, minor, or major version"))))))

(let [{:keys [fail error]} (run-tests 'release-test)]
  (when (pos? (+ fail error))
    (System/exit 1)))
