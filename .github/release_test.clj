(ns release-test
  (:require [babashka.fs :as fs]
            [clj-yaml.core :as yaml]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing use-fixtures]]
            [release :as sut]
            [release-jobs-test]
            [release-metadata-repository-test]
            [release-metadata-test]
            [release-record-version-test]
            [release-test-support :as support :refer [cleanup-repositories create-repository exact-message git
                                                      release with-repository]]
            [release-workflow-test]))

(use-fixtures :each cleanup-repositories)

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

(def default-releases
  [(release "v1.2.3")])

(defn run-test-release
  [repo bump & {:keys [releases] :or {releases default-releases}}]
  (with-redefs [sut/published-releases (constantly releases)]
    (with-repository repo #(sut/test-release bump))))

(deftest creates-default-patch-test-release-without-changing-files
  (let [repo (create-repository "1.2.3")
        properties-before (slurp (str (fs/path repo "gradle.properties")))]
    (git repo "config" "core.abbrev" "12")
    (let [sha (git repo "rev-parse" "--short=7" "HEAD")
          tag (str "v1.2.4-test." sha)]
      (run-test-release repo nil)
      (is (= tag (git repo "tag" "--list" tag)))
      (is (= "tag" (git repo "cat-file" "-t" tag)))
      (is (= (git repo "rev-parse" "HEAD") (git repo "rev-parse" (str tag "^{commit}"))))
      (is (= properties-before (slurp (str (fs/path repo "gradle.properties")))))
      (is (= "" (git repo "status" "--short"))))))

(deftest creates-requested-minor-and-major-test-releases
  (doseq [[bump expected] [["minor" "1.3.0"] ["major" "2.0.0"]]]
    (testing bump
      (let [repo (create-repository "1.2.3")
            sha (git repo "rev-parse" "--short=7" "HEAD")]
        (run-test-release repo bump)
        (is (= (str "v" expected "-test." sha)
               (git repo "tag" "--list" (str "v" expected "-test.*"))))))))

(deftest test-release-sha-has-at-least-seven-characters
  (let [repo (create-repository "1.2.3")]
    (git repo "config" "core.abbrev" "4")
    (run-test-release repo nil)
    (is (re-matches #"v1\.2\.4-test\.[0-9a-f]{7,40}"
                    (git repo "tag" "--list" "v1.2.4-test.*")))))

(deftest rejects-test-release-from-dirty-worktree
  (let [repo (create-repository "1.2.3")]
    (spit (str (fs/path repo "dirty.txt")) "dirty")
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"clean git worktree"
                          (run-test-release repo nil)))
    (is (= "" (git repo "tag" "--list" "v1.2.4-test.*")))))

(deftest rejects-duplicate-test-release-tag
  (let [repo (create-repository "1.2.3")]
    (run-test-release repo nil)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Tag already exists"
                          (run-test-release repo nil)))))

(deftest test-release-base-comes-from-highest-stable-release-not-plugin-version
  (let [repo (create-repository "0.0.1")
        sha (git repo "rev-parse" "--short=7" "HEAD")
        releases [(release "v1.2.3")
                  (release "v1.3.0-test.abcdef1" :prerelease true)
                  (release "v1.9.0" :draft true)
                  (release "v1.2.2")]]
    (run-test-release repo nil :releases releases)
    (is (= (str "v1.2.4-test." sha) (git repo "tag" "--list" "v1.*-test.*")))
    (is (= "pluginName = CodeScene
pluginVersion = 0.0.1
"
           (slurp (str (fs/path repo "gradle.properties")))))))

(deftest test-release-base-ignores-stable-tag-order-in-release-list
  (let [repo (create-repository "1.2.3")
        sha (git repo "rev-parse" "--short=7" "HEAD")]
    (run-test-release repo "minor" :releases [(release "v1.9.0") (release "v1.10.0") (release "v1.2.3")])
    (is (= (str "v1.11.0-test." sha) (git repo "tag" "--list" "v*-test.*")))))

(deftest test-release-fails-without-stable-release
  (doseq [releases [[]
                    [(release "v1.3.0-test.abcdef1" :prerelease true)]
                    [(release "v1.2.3" :draft true)]
                    [(release "v1.2.3" :prerelease true)]]]
    (testing (pr-str releases)
      (let [repo (create-repository "1.2.3")]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"No published stable GitHub release"
                              (run-test-release repo nil :releases releases)))
        (is (= "" (git repo "tag" "--list" "v*-test.*")))))))

(deftest test-release-does-not-query-releases-from-dirty-worktree-or-bad-bump
  (let [repo (create-repository "1.2.3")
        queried (atom 0)]
    (spit (str (fs/path repo "dirty.txt")) "dirty")
    (with-redefs [sut/published-releases (fn [] (swap! queried inc) default-releases)]
      (is (thrown? clojure.lang.ExceptionInfo (with-repository repo #(sut/test-release nil))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"make test-release \[BUMP=patch\|minor\|major\]"
                            (with-repository repo #(sut/test-release "banana")))))
    (is (zero? @queried))))

(deftest test-release-tag-message-is-default-notes
  (let [repo (create-repository "1.2.3")
        sha (git repo "rev-parse" "--short=7" "HEAD")
        tag (str "v1.2.4-test." sha)]
    (run-test-release repo nil)
    (is (= (str "Test release 1.2.4-test." sha)
           (git repo "for-each-ref" "--format=%(contents:subject)" (str "refs/tags/" tag))))
    (is (= (sut/test-default-notes (str "1.2.4-test." sha))
           (git repo "for-each-ref" "--format=%(contents)" (str "refs/tags/" tag))))))

(deftest published-releases-queries-github-in-repo-dir
  (let [calls (atom [])
        repo (create-repository "1.2.3")]
    (with-redefs [sut/sh (fn [& args]
                           (swap! calls conj [sut/*repo-dir* (vec args)])
                           "[[{\"tag_name\":\"v1.2.3\"}],[{\"tag_name\":\"v1.2.4\"}]]")]
      (is (= ["v1.2.3" "v1.2.4"]
             (mapv :tag_name (with-repository repo sut/published-releases)))))
    (is (= [[(str repo)
             ["gh" "api" "--paginate" "--slurp" "repos/{owner}/{repo}/releases?per_page=100"]]]
           @calls))))

(deftest release-notes-config-matches-shared-categories
  (let [config (:changelog (yaml/parse-string
                            (slurp (str (fs/path support/github-dir "release.yml")))))]
    (is (= [["Breaking Changes" ["breaking-change" "breaking"]]
            ["Features" ["feature" "enhancement"]]
            ["Bug Fixes" ["bug" "fix" "defect"]]
            ["Security" ["security"]]
            ["Documentation" ["documentation" "docs"]]
            ["Dependencies" ["dependencies"]]
            ["Other Changes" ["*"]]]
           (mapv (juxt :title :labels) (:categories config))))
    (is (= ["ignore-for-release" "skip-changelog"] (:labels (:exclude config))))
    (is (= ["dependabot" "github-actions"] (:authors (:exclude config))))))

(deftest classifies-stable-and-test-tags
  (is (= {:kind :stable :version "1.2.3"} (sut/classify-tag "v1.2.3")))
  (is (= {:kind :stable :version "0.0.0"} (sut/classify-tag "v0.0.0")))
  (is (= {:kind :stable :version "10.20.30"} (sut/classify-tag "v10.20.30")))
  (is (= {:kind :test
          :version "1.2.3-test.abcdef1"
          :base-version "1.2.3"
          :sha "abcdef1"}
         (sut/classify-tag "v1.2.3-test.abcdef1")))
  (let [sha (apply str (repeat 40 "a1b2c3d4e5"))
        sha40 (subs sha 0 40)]
    (is (= {:kind :test
            :version (str "0.6.0-test." sha40)
            :base-version "0.6.0"
            :sha sha40}
           (sut/classify-tag (str "v0.6.0-test." sha40))))))

(deftest rejects-malformed-tags
  (doseq [tag ["v01.2.3" "v1.2" "1.2.3" "v1.2.3-rc.1" "v1.2.3-test." "v1.2.3-test"
               "v1.2.3.4" "" "v01.2.3-test.abcdef1"]]
    (testing tag
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            (exact-message (str "Tag must match vX.Y.Z or vX.Y.Z-test.<hex-sha>; got " tag "."))
                            (sut/classify-tag tag)))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        (exact-message "Tag must match vX.Y.Z or vX.Y.Z-test.<hex-sha>; got .")
                        (sut/classify-tag nil))
      "nil tag is rejected with the general message")
  (doseq [tag ["v1.2.3-test.ABCDEF1" "v1.2.3-test.abc12" "v1.2.3-test.zzzzzzz"
               (str "v1.2.3-test." (apply str (repeat 41 "a")))
               "v1.2.3-test.abcdef1/x" "v1.2.3-test.abcdef1-extra"]]
    (testing tag
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            (exact-message (str "Test tag SHA must be 7-40 lowercase hex characters; got " tag "."))
                            (sut/classify-tag tag))))))

(deftest dispatch-routes-arguments-to-commands-and-rejects-unknown
  (let [received (atom nil)]
    (with-redefs [sut/commands {"stub" (fn [& args] (reset! received (vec args)))
                                "other" (fn [& _] nil)}]
      (sut/dispatch ["stub" "a" "b" "c" "d" "e" "f"])
      (is (= ["a" "b" "c" "d" "e" "f"] @received))
      (sut/dispatch ["stub"])
      (is (= [] @received))
      (doseq [input [["unknown" "x"] [] nil]]
        (testing (pr-str input)
          (let [error (try (sut/dispatch input) nil
                           (catch clojure.lang.ExceptionInfo e e))]
            (is (some? error))
            (is (str/starts-with? (ex-message error) "Usage:"))
            (is (str/includes? (ex-message error) "stub"))
            (is (str/includes? (ex-message error) "other"))))))))

(deftest default-commands-keep-existing-subcommands
  (is (= #{"test" "metadata" "compose-notes" "missing-assets"
           "test-gate" "marketplace-outcome" "promotion" "record-version"}
         (set (keys sut/commands)))))

(deftest compares-versions-numerically
  (doseq [[a b expected] [["1.10.0" "1.9.9" 1]
                          ["1.9.9" "1.10.0" -1]
                          ["1.2.3" "1.2.3" 0]
                          ["2.0.0" "1.99.99" 1]
                          ["0.9.9" "1.0.0" -1]
                          ["1.2.10" "1.2.9" 1]
                          [(sut/parse-version "1.10.0") (sut/parse-version "1.9.9") 1]]]
    (testing (pr-str [a b])
      (is (= expected (sut/compare-versions a b))))))

(deftest parses-flat-and-paginated-release-lists
  (let [flat "[{\"tag_name\":\"v1.0.0\",\"draft\":false,\"prerelease\":false},{\"tag_name\":\"v1.1.0\",\"draft\":true,\"prerelease\":false}]"
        paged "[[{\"tag_name\":\"v1.0.0\",\"id\":1}],[{\"tag_name\":\"v1.1.0\",\"id\":2},{\"tag_name\":\"v1.2.0\",\"id\":3}]]"]
    (is (= [{:tag_name "v1.0.0" :draft false :prerelease false}
            {:tag_name "v1.1.0" :draft true :prerelease false}]
           (sut/parse-releases flat)))
    (is (= ["v1.0.0" "v1.1.0" "v1.2.0"]
           (mapv :tag_name (sut/parse-releases paged))))
    (is (= [] (sut/parse-releases "[]")))
    (is (= [] (sut/parse-releases "[[],[]]")))))

(deftest classifies-stable-and-test-releases
  (doseq [[entry stable? test?] [[(release "v1.2.3") true false]
                                 [(release "v1.2.3" :draft true) false false]
                                 [(release "v1.2.3" :prerelease true) false false]
                                 [(release "v1.2.3-test.abcdef1") false false]
                                 [(release "v01.2.3") false false]
                                 [{:draft false :prerelease false} false false]
                                 [(release "v1.2.3-test.abcdef1" :prerelease true) false true]
                                 [(release "v1.2.3-test.abcdef1" :prerelease true :draft true) false false]
                                 [(release "v1.2.3-test.ABCDEF1" :prerelease true) false false]
                                 [{:prerelease true} false false]]]
    (testing (pr-str entry)
      (is (= [stable? test?] [(boolean (sut/stable-release? entry)) (boolean (sut/test-release? entry))])))))

(deftest selects-highest-stable-release
  (let [releases [(release "v0.5.9")
                  (release "v0.5.10")
                  (release "v0.6.0" :draft true)
                  (release "v0.7.0" :prerelease true)
                  (release "v0.8.0-test.abcdef1" :prerelease true)
                  (release "v0.9.0-test.abcdef1")
                  (release "v01.0.0")
                  (release "latest")
                  (release "v0.5.2")]]
    (is (= "v0.5.10" (:tag_name (sut/highest-stable-release releases))))
    (is (= "v0.5.9" (:tag_name (sut/highest-stable-release releases :exclude "v0.5.10"))))
    (is (= "v0.5.10" (:tag_name (sut/highest-stable-release releases :exclude "v9.9.9"))))
    (is (nil? (sut/highest-stable-release [(release "v0.5.9")] :exclude "v0.5.9"))))
  (is (nil? (sut/highest-stable-release [])))
  (is (nil? (sut/highest-stable-release [(release "v1.0.0" :draft true)
                                         (release "v1.0.0-test.abcdef1" :prerelease true)]))))

(let [{:keys [fail error]} (run-tests 'release-test
                                     'release-metadata-test
                                     'release-metadata-repository-test
                                     'release-jobs-test
                                     'release-record-version-test
                                     'release-workflow-test)]
  (when (pos? (+ fail error))
    (System/exit 1)))
