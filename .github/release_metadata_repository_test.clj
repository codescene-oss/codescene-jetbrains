(ns release-metadata-repository-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [release :as sut]
            [release-test-support :refer [annotate! cleanup-repositories commit! create-repository git release
                                          test-entry with-repository]]))

(use-fixtures :each cleanup-repositories)

(defn run-metadata
  [repo tag releases & {:keys [event run-ref master-ref]
                        :or {event "push" master-ref "master"}}]
  (let [out (java.io.StringWriter.)
        error (atom nil)
        json-text (json/generate-string releases)]
    (binding [*out* out]
      (try
        (with-repository repo
          #(with-in-str json-text
             (sut/dispatch ["metadata" tag event (or run-ref (str "refs/tags/" tag)) master-ref
                            "https://github.com" "codescene-oss/codescene-jetbrains"])))
        (catch clojure.lang.ExceptionInfo e
          (reset! error (ex-message e)))))
    {:out (str out)
     :error @error
     :values (into {} (map #(str/split % #"=" 2)) (remove str/blank? (str/split-lines (str out))))}))

(deftest admits-stable-tag-on-master-in-repository
  (let [repo (create-repository "1.2.3")
        sha (commit! repo "feat: change")]
    (annotate! repo "v1.2.4")
    (let [{:keys [error values]} (run-metadata repo "v1.2.4" [(release "v1.2.3")])]
      (is (nil? error))
      (is (= {"tag" "v1.2.4" "version" "1.2.4" "is_test" "false"
              "notes_start_tag" "v1.2.3" "stable_tag" "v1.2.3" "sha" sha}
             values)))))

(deftest rejects-missing-tag
  (let [repo (create-repository "1.2.3")]
    (doseq [tag ["v1.2.4" "v1.2.4-test.abcdef1"]]
      (testing tag
        (let [{:keys [out error]} (run-metadata repo tag [(release "v1.2.3")])]
          (is (= (str "Tag does not exist: " tag ".") error))
          (is (= "" out)))))))

(deftest rejects-lightweight-tag
  (let [repo (create-repository "1.2.3")]
    (commit! repo "feat: change")
    (git repo "tag" "v1.2.4")
    (let [{:keys [out error]} (run-metadata repo "v1.2.4" [(release "v1.2.3")])]
      (is (= "Release tag v1.2.4 must be annotated." error))
      (is (= "" out)))))

(deftest rejects-dispatch-from-non-master-ref-in-repository
  (let [repo (create-repository "1.2.3")]
    (commit! repo "feat: change")
    (annotate! repo "v1.2.4")
    (let [{:keys [error]} (run-metadata repo "v1.2.4" [(release "v1.2.3")]
                                        :event "workflow_dispatch" :run-ref "refs/heads/feature")]
      (is (= "Manual runs must be dispatched from refs/heads/master; got refs/heads/feature." error)))
    (let [{:keys [error values]} (run-metadata repo "v1.2.4" [(release "v1.2.3")]
                                               :event "workflow_dispatch" :run-ref "refs/heads/master")]
      (is (nil? error))
      (is (= "v1.2.4" (get values "tag"))))))

(defn run-metadata-for-side-branch-tag
  [tag-side-commit!]
  (let [repo (create-repository "1.2.3")]
    (git repo "checkout" "-b" "side")
    (commit! repo "feat: side change")
    (tag-side-commit! repo)
    (git repo "checkout" "master")
    (run-metadata repo "v1.2.4" [(release "v1.2.3")])))

(deftest rejects-stable-tag-not-on-master-in-repository
  (let [{:keys [out error]} (run-metadata-for-side-branch-tag #(annotate! % "v1.2.4"))]
    (is (= "Stable tag v1.2.4 must point to a commit on master." error))
    (is (= "" out))))

(deftest admits-test-tag-from-side-branch-with-stable-baseline
  (let [repo (create-repository "1.2.3")]
    (git repo "checkout" "-b" "side")
    (let [sha (commit! repo "feat: side change")
          tag (str "v1.2.4-test." (git repo "rev-parse" "--short=7" "HEAD"))]
      (annotate! repo tag)
      (let [{:keys [error values]} (run-metadata repo tag [(release "v1.2.3")])]
        (is (nil? error))
        (is (= {"tag" tag "version" (subs tag 1) "is_test" "true"
                "notes_start_tag" "v1.2.3" "stable_tag" "v1.2.3" "sha" sha}
               values))))))

(deftest resolves-previous-same-base-test-release-from-repository
  (let [repo (create-repository "1.2.3")
        first-tag (do (commit! repo "feat: one")
                      (str "v1.2.4-test." (git repo "rev-parse" "--short=7" "HEAD")))
        _ (annotate! repo first-tag)
        second-tag (do (commit! repo "feat: two")
                       (str "v1.2.4-test." (git repo "rev-parse" "--short=7" "HEAD")))
        _ (annotate! repo second-tag)
        {:keys [error values]} (run-metadata repo second-tag
                                             [(release "v1.2.3")
                                              (test-entry first-tag "2026-01-01T00:00:00Z")
                                              (test-entry second-tag "2026-01-02T00:00:00Z")])]
    (is (nil? error))
    (is (= first-tag (get values "notes_start_tag")))
    (is (= "v1.2.3" (get values "stable_tag")))))

(deftest rejects-test-tag-sha-mismatch-in-repository
  (let [repo (create-repository "1.2.3")
        sha (commit! repo "feat: change")
        tag "v1.2.4-test.0000000"]
    (annotate! repo tag)
    (let [{:keys [out error]} (run-metadata repo tag [(release "v1.2.3")])]
      (is (= (str "Test tag SHA 0000000 does not identify tagged commit " sha ".") error))
      (is (= "" out)))))

(deftest rejects-test-base-that-is-not-next-version-in-repository
  (let [repo (create-repository "1.2.3")]
    (commit! repo "feat: change")
    (let [tag (str "v1.2.5-test." (git repo "rev-parse" "--short=7" "HEAD"))]
      (annotate! repo tag)
      (let [{:keys [error]} (run-metadata repo tag [(release "v1.2.3")])]
        (is (= "Test base version 1.2.5 is not the next patch, minor, or major version after 1.2.3."
               error))))))

(deftest reports-every-failed-check-in-repository
  (let [{:keys [out error]} (run-metadata-for-side-branch-tag #(git % "tag" "v1.2.4"))]
    (is (= ["Release tag v1.2.4 must be annotated."
            "Stable tag v1.2.4 must point to a commit on master."]
           (str/split-lines error)))
    (is (= "" out))))

(deftest metadata-cli-prints-outputs
  (let [repo (create-repository "1.2.3")
        sha (commit! repo "feat: change")
        releases [(release "v1.2.3") (release "v1.2.0")]
        expected (str "tag=v1.2.4\nversion=1.2.4\nis_test=false\n"
                      "notes_start_tag=v1.2.3\nstable_tag=v1.2.3\nsha=" sha "\n")]
    (annotate! repo "v1.2.4")
    (doseq [[shape input] [["flat" releases]
                           ["paginated" [[(first releases)] [(second releases)]]]]]
      (testing shape
        (let [out (with-out-str
                    (with-repository repo
                      #(with-in-str (json/generate-string input)
                         (sut/dispatch ["metadata" "v1.2.4" "push" "refs/tags/v1.2.4" "master"
                                        "https://github.com" "codescene-oss/codescene-jetbrains"]))))]
          (is (= expected out)))))))

(deftest metadata-cli-requires-all-leading-arguments
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"^Usage: .* metadata <tag>"
                        (with-in-str "[]" (sut/dispatch ["metadata" "v1.2.4"])))))

