(ns release-metadata-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [release :as sut]
            [release-test-support :refer [cleanup-repositories create-repository exact-message git release
                                          test-entry with-repository]]))

(use-fixtures :each cleanup-repositories)

(defn full-sha
  [prefix]
  (str prefix (apply str (repeat (- 40 (count prefix)) "0"))))

(defn linear-ancestry
  [commits]
  (let [index (zipmap commits (range))]
    (fn [a b]
      (boolean (and (contains? index a) (contains? index b) (<= (index a) (index b)))))))

(defn stable-facts
  [& {:as overrides}]
  (merge {:tag "v1.2.4"
          :event "push"
          :run-ref "refs/tags/v1.2.4"
          :master-ref "origin/master"
          :exists? true
          :annotated? true
          :commit "c4"
          :ancestor? (linear-ancestry ["c2" "c3" "c4" "origin/master"])
          :tag-commit {"v1.2.3" "c3" "v1.2.2" "c2"}
          :releases [(release "v1.2.3")]}
         overrides))

(def test-commit (full-sha "ccccccc"))

(defn test-facts
  [& {:as overrides}]
  (merge {:tag "v1.2.4-test.ccccccc"
          :event "push"
          :run-ref "refs/tags/v1.2.4-test.ccccccc"
          :master-ref "origin/master"
          :exists? true
          :annotated? true
          :commit test-commit
          :ancestor? (linear-ancestry [(full-sha "1111111")
                                       (full-sha "aaaaaaa")
                                       (full-sha "bbbbbbb")
                                       (full-sha "eeeeeee")
                                       (full-sha "fffffff")
                                       test-commit])
          :tag-commit {"v1.2.3" (full-sha "1111111")}
          :releases [(release "v1.2.3")]}
         overrides))

(defn outputs-of
  [decision]
  (into {} (:outputs decision)))

(deftest admits-stable-tag-and-resolves-notes-range-from-previous-stable
  (let [decision (sut/decide-metadata (stable-facts))]
    (is (nil? (:errors decision)))
    (is (= [["tag" "v1.2.4"]
            ["version" "1.2.4"]
            ["is_test" "false"]
            ["notes_start_tag" "v1.2.3"]
            ["stable_tag" "v1.2.3"]
            ["sha" "c4"]]
           (:outputs decision)))))

(deftest admits-stable-tag-that-skips-versions
  (let [decision (sut/decide-metadata (stable-facts :tag "v1.4.0"))]
    (is (nil? (:errors decision)))
    (is (= "v1.2.3" (get (outputs-of decision) "notes_start_tag")))))

(deftest admits-first-stable-tag-when-no-stable-release-exists
  (let [decision (sut/decide-metadata (stable-facts :releases [] :tag-commit {}))]
    (is (nil? (:errors decision)))
    (is (= "" (get (outputs-of decision) "notes_start_tag")))
    (is (= "" (get (outputs-of decision) "stable_tag")))))

(deftest rejects-stable-tag-not-newer-than-highest-stable-release
  (doseq [[tag releases message] [["v1.2.3" [(release "v1.2.2") (release "v1.2.4")]
                                   "Stable version 1.2.3 must be newer than the highest stable release v1.2.4."]
                                  ["v1.2.2" [(release "v1.2.3")]
                                   "Stable version 1.2.2 must be newer than the highest stable release v1.2.3."]
                                  ["v0.9.9" [(release "v1.0.0")]
                                   "Stable version 0.9.9 must be newer than the highest stable release v1.0.0."]]]
    (testing (str tag " against " (mapv :tag_name releases))
      (let [decision (sut/decide-metadata (stable-facts :tag tag :releases releases))]
        (is (= [message] (:errors decision)))
        (is (nil? (:outputs decision)))))))

(deftest counts-unpromoted-stable-release-for-ordering-and-notes
  (let [facts (stable-facts :releases [(release "v1.2.3") (release "v1.2.4")]
                            :tag-commit {"v1.2.3" "c3" "v1.2.4" "c4"}
                            :ancestor? (linear-ancestry ["c3" "c4" "c5" "origin/master"]))
        blocked (sut/decide-metadata (assoc facts :tag "v1.2.2" :commit "c4"))
        next (sut/decide-metadata (assoc facts :tag "v1.2.5" :commit "c5"))]
    (is (= ["Stable version 1.2.2 must be newer than the highest stable release v1.2.4."]
           (:errors blocked)))
    (is (nil? (:errors next)))
    (is (= "v1.2.4" (get (outputs-of next) "notes_start_tag")))
    (is (= "v1.2.4" (get (outputs-of next) "stable_tag")))))

(deftest waives-ordering-when-release-for-tag-exists
  (let [decision (sut/decide-metadata
                   (stable-facts :releases [(release "v1.2.3") (release "v1.2.4") (release "v1.3.0")]
                                 :tag-commit {"v1.2.3" "c3" "v1.2.4" "c4" "v1.3.0" "c5"}))]
    (is (nil? (:errors decision)))
    (is (= "v1.2.3" (get (outputs-of decision) "notes_start_tag")))
    (is (= "v1.2.3" (get (outputs-of decision) "stable_tag")))))

(deftest draft-release-for-tag-does-not-waive-ordering
  (let [decision (sut/decide-metadata
                   (stable-facts :releases [(release "v1.2.3") (release "v1.2.4" :draft true) (release "v1.3.0")]))]
    (is (= ["Stable version 1.2.4 must be newer than the highest stable release v1.3.0."]
           (:errors decision)))))

(deftest ignores-prereleases-and-drafts-for-stable-ordering
  (let [decision (sut/decide-metadata
                   (stable-facts :releases [(release "v1.2.3")
                                            (release "v1.5.0" :prerelease true)
                                            (release "v1.6.0" :draft true)
                                            (release "v1.4.0-test.abcdef1" :prerelease true)]))]
    (is (nil? (:errors decision)))
    (is (= "v1.2.3" (get (outputs-of decision) "notes_start_tag")))))

(deftest notes-range-skips-stable-releases-that-are-not-ancestors
  (let [facts (stable-facts :releases [(release "v1.2.1") (release "v1.2.2") (release "v1.2.3")]
                            :tag-commit {"v1.2.1" "c1" "v1.2.2" "c2" "v1.2.3" "c3"}
                            :ancestor? (linear-ancestry ["c1" "c2" "c4" "origin/master"]))
        decision (sut/decide-metadata facts)]
    (is (nil? (:errors decision)))
    (is (= "v1.2.2" (get (outputs-of decision) "notes_start_tag")))
    (is (= "v1.2.2" (get (outputs-of decision) "stable_tag")))))

(deftest notes-range-skips-stable-releases-without-local-tag
  (let [decision (sut/decide-metadata
                   (stable-facts :releases [(release "v1.2.2") (release "v1.2.3")]
                                 :tag-commit {"v1.2.2" "c2"}))]
    (is (= "v1.2.2" (get (outputs-of decision) "notes_start_tag")))))

(deftest notes-range-is-empty-when-no-previous-stable-is-an-ancestor
  (let [decision (sut/decide-metadata
                   (stable-facts :ancestor? (linear-ancestry ["c4" "origin/master"])))]
    (is (nil? (:errors decision)))
    (is (= "" (get (outputs-of decision) "notes_start_tag")))
    (is (= "" (get (outputs-of decision) "stable_tag")))))

(deftest rejects-missing-tag-without-touching-git-facts
  (let [boom (fn [& _] (throw (ex-info "git must not be consulted" {})))]
    (doseq [tag ["v1.2.4" "v1.2.4-test.abcdef1"]]
      (testing tag
        (let [decision (sut/decide-metadata {:tag tag
                                             :event "workflow_dispatch"
                                             :run-ref "refs/heads/feature"
                                             :master-ref "origin/master"
                                             :exists? false
                                             :ancestor? boom
                                             :tag-commit boom
                                             :releases [(release "v9.0.0")]})]
          (is (= [(str "Tag does not exist: " tag ".")] (:errors decision))))))))

(deftest rejects-tag-that-does-not-point-to-a-commit
  (let [decision (sut/decide-metadata (stable-facts :commit nil))]
    (is (= ["Tag v1.2.4 does not point to a commit."] (:errors decision)))))

(deftest rejects-malformed-and-leading-zero-tags
  (let [boom (fn [& _] (throw (ex-info "git must not be consulted" {})))
        facts {:event "push" :master-ref "origin/master" :exists? true :ancestor? boom :tag-commit boom :releases []}]
    (doseq [[tag message] [["v01.2.3" "Tag must match vX.Y.Z or vX.Y.Z-test.<hex-sha>; got v01.2.3."]
                           ["v1.2.3-test.ABCDEF1" "Test tag SHA must be 7-40 lowercase hex characters; got v1.2.3-test.ABCDEF1."]]]
      (testing tag
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              (exact-message message)
                              (sut/decide-metadata (assoc facts :tag tag))))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              (exact-message message)
                              (with-in-str "[]"
                                (sut/dispatch ["metadata" tag "push" "refs/heads/x" "origin/master" "https://github.com" "o/r"]))))))))

(deftest rejects-dispatch-from-non-master-ref
  (let [decision (sut/decide-metadata (stable-facts :event "workflow_dispatch" :run-ref "refs/heads/feature"))]
    (is (= ["Manual runs must be dispatched from refs/heads/master; got refs/heads/feature."]
           (:errors decision)))))

(deftest accepts-dispatch-from-master
  (let [decision (sut/decide-metadata (stable-facts :event "workflow_dispatch" :run-ref "refs/heads/master"))]
    (is (nil? (:errors decision)))
    (is (= "v1.2.4" (get (outputs-of decision) "tag")))))

(deftest ignores-run-ref-for-push-events
  (let [decision (sut/decide-metadata (stable-facts :event "push" :run-ref "refs/tags/v1.2.4"))]
    (is (nil? (:errors decision)))))

(deftest rejects-stable-tag-not-on-master
  (let [decision (sut/decide-metadata (stable-facts :ancestor? (linear-ancestry ["c2" "c3" "c4"])))]
    (is (= ["Stable tag v1.2.4 must point to a commit on origin/master."] (:errors decision)))))

(deftest admits-test-tag-without-master-ancestry-and-stable-baseline
  (let [decision (sut/decide-metadata (test-facts))]
    (is (nil? (:errors decision)))
    (is (= [["tag" "v1.2.4-test.ccccccc"]
            ["version" "1.2.4-test.ccccccc"]
            ["is_test" "true"]
            ["notes_start_tag" "v1.2.3"]
            ["stable_tag" "v1.2.3"]
            ["sha" test-commit]]
           (:outputs decision)))))

(deftest accepts-test-base-that-is-patch-minor-or-major-increment
  (doseq [base ["1.2.4" "1.3.0" "2.0.0"]]
    (testing base
      (let [tag (str "v" base "-test.ccccccc")]
        (is (nil? (:errors (sut/decide-metadata (test-facts :tag tag)))))))))

(deftest admits-test-tag-when-no-stable-release-exists
  (let [decision (sut/decide-metadata (test-facts :releases [] :tag-commit {}))]
    (is (nil? (:errors decision)))
    (is (= "" (get (outputs-of decision) "notes_start_tag")))))

(deftest resolves-previous-same-base-test-release-as-notes-start
  (let [decision (sut/decide-metadata
                   (test-facts :releases [(release "v1.2.3")
                                          (test-entry "v1.2.4-test.aaaaaaa" "2026-01-01T00:00:00Z")
                                          (test-entry "v1.2.4-test.bbbbbbb" "2026-02-01T00:00:00Z")]
                               :tag-commit {"v1.2.3" (full-sha "1111111")
                                            "v1.2.4-test.aaaaaaa" (full-sha "aaaaaaa")
                                            "v1.2.4-test.bbbbbbb" (full-sha "bbbbbbb")}))]
    (is (nil? (:errors decision)))
    (is (= "v1.2.4-test.bbbbbbb" (get (outputs-of decision) "notes_start_tag")))
    (is (= "v1.2.3" (get (outputs-of decision) "stable_tag")))))

(deftest ignores-invalid-or-non-ancestor-test-releases-for-baseline
  (let [tag-commits {"v1.2.3" (full-sha "1111111")
                     "v1.2.4-test.aaaaaaa" (full-sha "aaaaaaa")
                     "v1.2.4-test.ddddddd" (full-sha "eeeeeee")
                     "v1.2.5-test.fffffff" (full-sha "fffffff")
                     "v1.2.4-test.9999999" (full-sha "9999999")
                     "v1.2.4-test.ccccccc" test-commit}
        invalid [(test-entry "v1.2.4-test.ccccccc" "2026-06-01T00:00:00Z")
                 (test-entry "v1.2.4-test.ddddddd" "2026-05-01T00:00:00Z")
                 (test-entry "v1.2.5-test.fffffff" "2026-04-01T00:00:00Z")
                 (test-entry "v1.2.4-test.9999999" "2026-03-01T00:00:00Z")
                 (test-entry "v1.2.4-test.8888888" "2026-02-15T00:00:00Z")
                 (assoc (test-entry "v1.2.4-test.7777777" "2026-02-10T00:00:00Z") :draft true)]
        with-valid (sut/decide-metadata
                     (test-facts :releases (into [(release "v1.2.3")
                                                  (test-entry "v1.2.4-test.aaaaaaa" "2026-01-01T00:00:00Z")]
                                                 invalid)
                                 :tag-commit tag-commits))
        without-valid (sut/decide-metadata
                        (test-facts :releases (into [(release "v1.2.3")] invalid)
                                    :tag-commit tag-commits))]
    (is (= "v1.2.4-test.aaaaaaa" (get (outputs-of with-valid) "notes_start_tag")))
    (is (= "v1.2.3" (get (outputs-of with-valid) "stable_tag")))
    (is (= "v1.2.3" (get (outputs-of without-valid) "notes_start_tag")))))

(deftest rejects-test-tag-sha-mismatch
  (let [decision (sut/decide-metadata (test-facts :commit (full-sha "dddddddd")))]
    (is (= [(str "Test tag SHA ccccccc does not identify tagged commit " (full-sha "dddddddd") ".")]
           (:errors decision)))))

(deftest rejects-test-base-that-is-not-next-version
  (doseq [tag ["v1.2.5-test.ccccccc" "v1.2.3-test.ccccccc" "v1.4.0-test.ccccccc"]]
    (testing tag
      (let [decision (sut/decide-metadata (test-facts :tag tag))]
        (is (= [(str "Test base version " (second (re-find #"^v(\d+\.\d+\.\d+)" tag))
                     " is not the next patch, minor, or major version after 1.2.3.")]
               (:errors decision)))))))

(deftest reports-every-failed-check
  (let [decision (sut/decide-metadata
                   (stable-facts :annotated? false
                                 :event "workflow_dispatch"
                                 :run-ref "refs/heads/feature"
                                 :ancestor? (linear-ancestry ["c2" "c3" "c4"])
                                 :releases [(release "v1.2.3") (release "v1.3.0")]))]
    (is (= ["Release tag v1.2.4 must be annotated."
            "Manual runs must be dispatched from refs/heads/master; got refs/heads/feature."
            "Stable tag v1.2.4 must point to a commit on origin/master."
            "Stable version 1.2.4 must be newer than the highest stable release v1.3.0."]
           (:errors decision)))))

(deftest commit-ancestry-treats-exit-codes-as-answer-or-failure
  (let [repo (create-repository "1.2.3")
        first-commit (git repo "rev-parse" "HEAD")
        second-commit (do (git repo "commit" "--allow-empty" "-m" "second")
                          (git repo "rev-parse" "HEAD"))]
    (with-repository repo
      (fn []
        (is (true? (sut/commit-ancestor? first-commit second-commit)))
        (is (false? (sut/commit-ancestor? second-commit first-commit)))
        (is (thrown? clojure.lang.ExceptionInfo
                     (sut/commit-ancestor? first-commit "no-such-ref")))))))

