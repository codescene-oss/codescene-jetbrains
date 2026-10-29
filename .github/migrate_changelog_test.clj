(ns migrate-changelog-test
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing use-fixtures]]
            [migrate-changelog :as sut]
            [release]))

(def repositories (atom []))

(defn git
  [repo & args]
  (let [result (apply shell {:dir (str repo) :out :string :err :string :continue true}
                      "git" args)]
    (if (zero? (:exit result))
      (str/trim (or (:out result) ""))
      (throw (ex-info (or (:err result) "git command failed") result)))))

(defn create-repository []
  (let [repo (fs/create-temp-dir {:prefix "codescene-migrate-test-"})]
    (swap! repositories conj repo)
    (git repo "init" "-b" "master")
    (git repo "config" "user.email" "test@example.com")
    (git repo "config" "user.name" "Test User")
    repo))

(defn commit-file
  [repo name content message]
  (spit (str (fs/path repo name)) content)
  (git repo "add" name)
  (git repo "commit" "-m" message))

(defn with-repository
  [repo f]
  (binding [release/*repo-dir* (str repo)]
    (f)))

(use-fixtures
  :each
  (fn [test-fn]
    (try
      (test-fn)
      (finally
        (doseq [repo @repositories]
          (fs/delete-tree repo {:force true}))
        (reset! repositories [])))))

(def changelog-text
  "# Changelog

## [Unreleased]

### Added
- Not yet released

## [0.5.10] - 2026-10-01

- **Fixed**
  - A fix

### [0.5.9] - 2026-09-01

- Another fix

## [0.5.8] - 2026-08-01
")

(defn release
  [tag & {:keys [id body draft prerelease] :or {draft false prerelease false}}]
  {:id (or id 1) :tag_name tag :body body :draft draft :prerelease prerelease})

(defn versions [sections]
  (mapv :version sections))

(defn body-for [sections version]
  (:body (first (filter #(= version (:version %)) sections))))

(deftest parses-double-and-triple-hash-version-headings
  (let [sections (sut/parse-changelog changelog-text)]
    (is (= ["0.5.10" "0.5.9" "0.5.8"] (versions sections)))
    (is (= "" (body-for sections "0.5.8")))))

(deftest ignores-unreleased-section
  (let [sections (sut/parse-changelog changelog-text)]
    (is (not-any? #(str/includes? (:body %) "Not yet released") sections))
    (is (not-any? #(= "Unreleased" (:version %)) sections))))

(deftest ignores-title-and-text-before-first-version
  (let [sections (sut/parse-changelog "# Changelog\n\nIntro text\n\n## [1.0.0] - 2026-01-01\n\n- One\n")]
    (is (= [{:version "1.0.0" :body "- One"}] sections))))

(deftest fails-on-duplicate-version
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"0\.5\.10"
                        (sut/parse-changelog
                         "## [0.5.10] - 2026-01-01\n\n- a\n\n## [0.5.10] - 2026-01-02\n\n- b\n"))))

(deftest section-ends-at-next-heading
  (let [sections (sut/parse-changelog changelog-text)]
    (is (= "- **Fixed**\n  - A fix" (body-for sections "0.5.10")))
    (is (= "- Another fix" (body-for sections "0.5.9")))))

(deftest section-ends-at-heading-of-any-level-up-to-three
  (let [sections (sut/parse-changelog "## [1.0.0] - 2026-01-01
- One
### Added
- Hidden
")]
    (is (= "- One" (body-for sections "1.0.0")))))

(deftest section-keeps-deeper-headings-and-handles-crlf
  (let [sections (sut/parse-changelog "## [1.0.0] - 2026-01-01\r\n\r\n#### Detail\r\n- One\r\n\r\n## [0.9.0] - 2026-01-01\r\n- Two\r\n")]
    (is (= "#### Detail\n- One" (body-for sections "1.0.0")))
    (is (= "- Two" (body-for sections "0.9.0")))))

(def sections-fixture
  [{:version "1.0.0" :body "- One"}
   {:version "0.9.0" :body "- Nine"}])

(deftest plans-updates-only-for-stable-releases
  (let [releases [(release "v1.0.0" :id 10 :body "Notes")
                  (release "v0.9.0" :id 9 :prerelease true)
                  (release "v0.9.0" :id 8 :draft true)
                  (release "v0.9.0-test.abc1234" :id 7 :prerelease true)]
        plan (sut/plan-migration sections-fixture releases)]
    (is (= ["v1.0.0"] (map :tag (:updates plan))))
    (is (= 10 (:id (first (:updates plan)))))
    (is (= "Notes" (:body (first (:updates plan)))))
    (is (= (sut/apply-changelog-block "Notes" "- One") (:new-body (first (:updates plan)))))
    (is (= ["v0.9.0"] (:unmatched plan)))
    (is (empty? (:up-to-date plan)))
    (is (empty? (:empty plan)))))

(deftest reports-unmatched-versions
  (let [plan (sut/plan-migration sections-fixture [(release "v1.0.0" :body "")])]
    (is (= ["v0.9.0"] (:unmatched plan)))
    (is (= ["v1.0.0"] (map :tag (:updates plan))))))

(deftest reports-empty-sections
  (let [plan (sut/plan-migration [{:version "1.0.0" :body ""}] [(release "v1.0.0" :body "Notes")])]
    (is (= ["v1.0.0"] (:empty plan)))
    (is (empty? (:updates plan)))
    (is (empty? (:unmatched plan)))))

(deftest treats-null-release-body-as-empty
  (let [plan (sut/plan-migration [{:version "1.0.0" :body "- One"}] [(release "v1.0.0" :body nil)])]
    (is (= (sut/apply-changelog-block "" "- One") (:new-body (first (:updates plan)))))
    (is (= "" (:body (first (:updates plan)))))))

(deftest inserts-block-preserving-existing-body
  (let [result (sut/apply-changelog-block "Existing notes\n\nMore notes\n" "- One")]
    (is (= (str "Existing notes\n\nMore notes\n\n"
                sut/start-marker "\n## Changelog\n\n- One\n" sut/end-marker)
           result))))

(deftest inserts-block-into-blank-body
  (is (= (str sut/start-marker "\n## Changelog\n\n- One\n" sut/end-marker)
         (sut/apply-changelog-block "" "- One")))
  (is (= (sut/apply-changelog-block "" "- One") (sut/apply-changelog-block nil "- One"))))

(deftest replaces-existing-block
  (let [old (sut/apply-changelog-block "Existing notes" "- Old")
        result (sut/apply-changelog-block old "- New")]
    (is (str/includes? result "- New"))
    (is (not (str/includes? result "- Old")))
    (is (= 1 (count (re-seq (re-pattern (java.util.regex.Pattern/quote sut/start-marker)) result))))
    (is (str/starts-with? result "Existing notes\n\n"))))

(deftest keeps-content-after-replaced-block
  (let [body (str "Before\n\n" (sut/changelog-block "- Old") "\n\nAfter")
        result (sut/apply-changelog-block body "- New")]
    (is (str/includes? result "Before"))
    (is (str/includes? result "After"))
    (is (not (str/includes? result "- Old")))
    (is (str/ends-with? result sut/end-marker))))

(deftest second-application-leaves-body-unchanged
  (let [first-plan (sut/plan-migration sections-fixture [(release "v1.0.0" :body "Notes")])
        migrated (:new-body (first (:updates first-plan)))
        second-plan (sut/plan-migration sections-fixture [(release "v1.0.0" :body migrated)])]
    (is (= migrated (sut/apply-changelog-block migrated "- One")))
    (is (= ["v1.0.0"] (:up-to-date second-plan)))
    (is (empty? (:updates second-plan)))))

(deftest reads-changelog-from-commit-before-deletion
  (let [repo (create-repository)]
    (commit-file repo "CHANGELOG.md" "first version\n" "add changelog")
    (commit-file repo "CHANGELOG.md" "second version\n" "modify changelog")
    (git repo "rm" "CHANGELOG.md")
    (git repo "commit" "-m" "remove changelog")
    (commit-file repo "other.txt" "x" "later commit")
    (with-repository repo
      #(is (= "second version" (sut/read-changelog nil))))))

(deftest fails-when-no-deletion-in-history
  (let [repo (create-repository)]
    (commit-file repo "CHANGELOG.md" "still here\n" "add changelog")
    (with-repository repo
      #(is (thrown-with-msg? clojure.lang.ExceptionInfo #"--file" (sut/read-changelog nil))))))

(deftest reads-changelog-from-file-option
  (let [dir (fs/create-temp-dir {:prefix "codescene-migrate-file-"})
        file (str (fs/path dir "changelog.md"))]
    (swap! repositories conj dir)
    (spit file "from file")
    (is (= "from file" (sut/read-changelog file)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"not found"
                          (sut/read-changelog (str (fs/path dir "missing.md")))))))

(defn migrate-with-fakes
  [args releases]
  (let [updates (atom [])
        output (with-redefs [sut/list-releases (fn [_repo] releases)
                             sut/update-release-body (fn [repo id body] (swap! updates conj [repo id body]))]
                 (with-out-str (sut/migrate args)))]
    {:updates @updates :output output}))

(defn changelog-file []
  (let [dir (fs/create-temp-dir {:prefix "codescene-migrate-file-"})
        file (str (fs/path dir "CHANGELOG.md"))]
    (swap! repositories conj dir)
    (spit file changelog-text)
    file))

(def migration-releases
  [(release "v0.5.10" :id 510 :body "Notes 10")
   (release "v0.5.9" :id 59 :body "Notes 9")])

(deftest dry-run-never-updates
  (let [{:keys [updates output]} (migrate-with-fakes ["--file" (changelog-file)] migration-releases)]
    (is (empty? updates))
    (is (str/includes? output "Would update v0.5.10"))
    (is (str/includes? output "Would update v0.5.9"))
    (is (not (str/includes? output "Updating")))
    (is (str/includes? output sut/start-marker))
    (is (str/includes? output "--apply"))
    (is (str/includes? output "v0.5.8"))))

(deftest apply-updates-each-planned-release
  (let [{:keys [updates output]} (migrate-with-fakes
                                  ["--file" (changelog-file) "--repo" "acme/widgets" "--apply"]
                                  migration-releases)]
    (is (= [["acme/widgets" 510 (sut/apply-changelog-block "Notes 10" "- **Fixed**\n  - A fix")]
            ["acme/widgets" 59 (sut/apply-changelog-block "Notes 9" "- Another fix")]]
           updates))
    (is (str/includes? output "Updating v0.5.10"))
    (is (not (str/includes? output "Would update")))
    (is (not (str/includes? output "Pass --apply")))))

(deftest apply-skips-up-to-date-releases
  (let [file (changelog-file)
        migrated (sut/apply-changelog-block "Notes 10" "- **Fixed**\n  - A fix")
        {:keys [updates output]} (migrate-with-fakes
                                  ["--file" file "--apply"]
                                  [(release "v0.5.10" :id 510 :body migrated)])]
    (is (empty? updates))
    (is (str/includes? output "Already up to date: 1"))))

(deftest uses-default-repo-when-not-given
  (let [repos (atom [])]
    (with-redefs [sut/list-releases (fn [repo] (swap! repos conj repo) [])]
      (with-out-str (sut/migrate ["--file" (changelog-file)])))
    (is (= ["codescene-oss/codescene-jetbrains"] @repos))))

(deftest rejects-unknown-option
  (testing "unknown option"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown option: --bogus(?s:.*)Usage"
                          (sut/parse-args ["--bogus"]))))
  (testing "positional argument"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown option: stray"
                          (sut/parse-args ["stray"])))))

(deftest rejects-missing-option-values
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing value for --file(?s:.*)Usage" (sut/parse-args ["--file"])))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Missing value for --repo" (sut/parse-args ["--repo" "--apply"]))))

(deftest parses-valid-options
  (is (= {:repo "codescene-oss/codescene-jetbrains" :apply false} (sut/parse-args [])))
  (is (= {:repo "a/b" :apply true :file "x.md"} (sut/parse-args ["--file" "x.md" "--repo" "a/b" "--apply"]))))

(let [{:keys [fail error]} (run-tests 'migrate-changelog-test)]
  (when (pos? (+ fail error))
    (System/exit 1)))
