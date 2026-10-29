(ns release-jobs-test
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [release :as sut]))

(defn run-compose-notes
  [notes & args]
  (with-out-str
    (with-in-str notes
      (sut/dispatch (into ["compose-notes"] args)))))

(def generated-notes "## What's Changed\n* feat: thing by @dev in #1\n")

(deftest stable-notes-are-passed-through
  (is (= generated-notes
         (run-compose-notes generated-notes "false" "v1.2.3" "v1.2.4"
                            "https://github.com" "codescene-oss/codescene-jetbrains")))
  (is (= generated-notes
         (run-compose-notes generated-notes "false" "" "v1.2.4"
                            "https://github.com" "codescene-oss/codescene-jetbrains"))))

(deftest test-notes-get-compare-link-from-stable
  (is (= (str "[Full changes since v1.2.3]"
              "(https://github.com/codescene-oss/codescene-jetbrains/compare/v1.2.3...v1.2.4-test.abcdef1)"
              "\n\n" generated-notes)
         (run-compose-notes generated-notes "true" "v1.2.3" "v1.2.4-test.abcdef1"
                            "https://github.com" "codescene-oss/codescene-jetbrains"))))

(deftest test-notes-without-stable-tag-are-unchanged
  (is (= generated-notes
         (run-compose-notes generated-notes "true" "" "v1.2.4-test.abcdef1"
                            "https://github.com" "codescene-oss/codescene-jetbrains"))))

(deftest composed-notes-normalise-trailing-newlines
  (doseq [notes ["body" "body\n" "body\n\n\n" "body  \n \n"]]
    (testing (pr-str notes)
      (is (= "body\n" (run-compose-notes notes "false" "v1.2.3" "v1.2.4" "https://github.com" "o/r")))
      (is (= "body\n" (run-compose-notes notes "true" "" "v1.2.4-test.abcdef1" "https://github.com" "o/r")))
      (is (= "[Full changes since v1.2.3](https://github.com/o/r/compare/v1.2.3...v1.2.4-test.abcdef1)\n\nbody\n"
             (run-compose-notes notes "true" "v1.2.3" "v1.2.4-test.abcdef1" "https://github.com" "o/r"))))))

(deftest composed-notes-keep-internal-blank-lines
  (is (= "a\n\nb\n" (run-compose-notes "a\n\nb\n\n" "false" "" "v1.2.4" "https://github.com" "o/r"))))

(deftest test-notes-with-empty-generated-notes-are-only-the-compare-link
  (is (= "[Full changes since v1.2.3](https://github.com/o/r/compare/v1.2.3...v1.2.4-test.abcdef1)\n"
         (run-compose-notes "" "true" "v1.2.3" "v1.2.4-test.abcdef1" "https://github.com" "o/r")))
  (is (= "\n" (run-compose-notes "" "false" "" "v1.2.4" "https://github.com" "o/r"))))

(deftest compose-notes-rejects-bad-arguments
  (doseq [args [[]
                ["false" "v1.2.3" "v1.2.4" "https://github.com"]
                ["maybe" "v1.2.3" "v1.2.4" "https://github.com" "o/r"]
                ["true" "v1.2.3" "" "https://github.com" "o/r"]]]
    (testing (pr-str args)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"^Usage: .* compose-notes <is-test>"
                            (apply run-compose-notes "notes" args))))))

(defn run-missing-assets
  [existing-names & paths]
  (let [dir (fs/create-temp-dir {:prefix "assets-"})
        names-file (str (fs/path dir "names.txt"))
        err (java.io.StringWriter.)]
    (spit names-file existing-names)
    (let [out (binding [*err* err]
                (with-out-str (sut/dispatch (into ["missing-assets" names-file] paths))))]
      {:out out :err (str err)})))

(deftest missing-assets-lists-only-unattached-files
  (let [{:keys [out err]} (run-missing-assets "CodeScene-1.2.4.zip\n"
                                              "dist/CodeScene-1.2.4.zip"
                                              "dist/CodeScene-1.2.4.zip.sha256"
                                              "dist/notes.txt")]
    (is (= "dist/CodeScene-1.2.4.zip.sha256\ndist/notes.txt\n" out))
    (is (= "Asset CodeScene-1.2.4.zip already attached, skipping\n" err))))

(deftest missing-assets-prints-nothing-when-all-attached
  (let [{:keys [out err]} (run-missing-assets "a.zip\nb.zip\n" "x/a.zip" "x/b.zip")]
    (is (= "" out))
    (is (= "Asset a.zip already attached, skipping\nAsset b.zip already attached, skipping\n" err))))

(deftest missing-assets-lists-everything-when-none-attached
  (let [{:keys [out err]} (run-missing-assets "" "x/a.zip" "x/b.zip")]
    (is (= "x/a.zip\nx/b.zip\n" out))
    (is (= "" err))))

(deftest missing-assets-handles-windows-and-posix-separators
  (let [{:keys [out err]} (run-missing-assets "a.zip\r\nb.zip\r\n"
                                              "C:\\work\\dist\\a.zip"
                                              "/work/dist/b.zip"
                                              "C:\\work\\dist\\c.zip"
                                              "dist\\mixed/d.zip")]
    (is (= "C:\\work\\dist\\c.zip\ndist\\mixed/d.zip\n" out))
    (is (= "Asset a.zip already attached, skipping\nAsset b.zip already attached, skipping\n" err))))

(deftest missing-assets-matches-whole-file-names-only
  (let [{:keys [out]} (run-missing-assets "a.zip\n" "x/aa.zip" "x/a.zip.sig" "x/dir/a.zip/other")]
    (is (= "x/aa.zip\nx/a.zip.sig\nx/dir/a.zip/other\n" out))))

(deftest missing-assets-requires-names-file-and-paths
  (doseq [args [["missing-assets"] ["missing-assets" "names.txt"]]]
    (testing (pr-str args)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"^Usage: .* missing-assets <existing-names-file>"
                            (sut/dispatch args))))))

(defn build-run
  [status conclusion event]
  {:status status
   :conclusion conclusion
   :event event
   :html_url (str "https://github.com/o/r/actions/runs/" (name event) "-" status "-" (or conclusion "none"))})

(defn run-test-gate
  [sha runs-json]
  (with-out-str
    (with-in-str runs-json
      (sut/dispatch ["test-gate" sha]))))

(defn gate-error
  [runs]
  (try
    (run-test-gate "abc1234" (json/generate-string runs))
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-message error))))

(deftest gate-passes-on-any-successful-run
  (let [success (build-run "completed" "success" :workflow_dispatch)
        runs [(build-run "completed" "failure" :push)
              (build-run "in_progress" nil :pull_request)
              success
              (build-run "completed" "cancelled" :push)]]
    (is (= (str (:html_url success) "\n")
           (run-test-gate "abc1234" (json/generate-string runs))))))

(deftest gate-passes-regardless-of-event
  (doseq [event [:push :pull_request :workflow_dispatch :schedule]]
    (testing (name event)
      (let [run (build-run "completed" "success" event)]
        (is (= (str (:html_url run) "\n")
               (run-test-gate "abc1234" (json/generate-string [run]))))))))

(deftest gate-ignores-success-conclusion-on-unfinished-run
  (is (some? (gate-error [(build-run "in_progress" "success" :push)]))))

(deftest gate-reports-in-progress
  (let [message (gate-error [(build-run "completed" "failure" :push)
                             (build-run "in_progress" nil :push)])]
    (is (str/includes? message "still running"))
    (is (str/includes? message "abc1234"))
    (is (str/includes? message "Re-run the release once Build finishes"))
    (doseq [status ["queued" "waiting" "requested" "pending"]]
      (is (str/includes? (gate-error [(build-run status nil :push)]) "still running")))))

(deftest gate-reports-failed-or-cancelled
  (let [message (gate-error [(build-run "completed" "failure" :push)
                             (build-run "completed" "cancelled" :pull_request)])]
    (is (str/includes? message "did not succeed"))
    (is (str/includes? message "failure, cancelled"))
    (is (str/includes? message "abc1234"))
    (is (str/includes? message "Re-run Build"))
    (is (not (str/includes? message "still running")))))

(deftest gate-reports-no-run
  (let [message (gate-error [])]
    (is (str/includes? message "No Build run found"))
    (is (str/includes? message "abc1234"))
    (is (str/includes? message "merge commit"))
    (is (str/includes? message "bot commit"))))

(deftest gate-failure-messages-are-distinct
  (let [messages [(gate-error [])
                  (gate-error [(build-run "queued" nil :push)])
                  (gate-error [(build-run "completed" "failure" :push)])]]
    (is (= 3 (count (set messages))))))

(deftest gate-rejects-non-array-input
  (doseq [input ["{\"status\":\"completed\",\"conclusion\":\"success\"}" "null" "\"success\"" "42" "" "not json"]]
    (testing (pr-str input)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"must be a JSON array"
                            (run-test-gate "abc1234" input))))))

(deftest gate-requires-sha
  (doseq [sha [nil ""]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"^Usage: .* test-gate <sha>"
                          (with-in-str "[]" (sut/dispatch (if sha ["test-gate" sha] ["test-gate"])))))))

(defn run-marketplace-outcome
  [exit-code log-text]
  (let [dir (fs/create-temp-dir {:prefix "publish-"})
        log-file (str (fs/path dir "publish.log"))]
    (spit log-file log-text)
    (with-out-str (sut/dispatch ["marketplace-outcome" exit-code log-file]))))

(defn marketplace-error
  [exit-code log-text]
  (try
    (run-marketplace-outcome exit-code log-text)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-message error))))

(deftest marketplace-success
  (is (= "published\n" (run-marketplace-outcome "0" "Uploading plugin\nDone\n")))
  (is (= "published\n" (run-marketplace-outcome "0" "")))
  (is (= "published\n" (run-marketplace-outcome "0" "Version already contains version noise"))))

(deftest marketplace-already-contains-version-is-success
  (is (= "already-published\n"
         (run-marketplace-outcome "1" "Upload failed: Plugin 'CodeScene' already contains version '1.2.4'\n")))
  (is (= "already-published\n"
         (run-marketplace-outcome "255" "x already contains version y"))))

(deftest marketplace-other-failure-fails
  (let [message (marketplace-error "1" "Upload failed: 401 Unauthorized\ninvalid token\n")]
    (is (str/includes? message "exit code 1"))
    (is (str/includes? message "401 Unauthorized"))
    (is (str/includes? message "invalid token"))))

(deftest marketplace-failure-reports-only-the-log-tail
  (let [log (str/join "\n" (map #(str "line " %) (range 1 101)))
        message (marketplace-error "2" log)]
    (is (str/includes? message "exit code 2"))
    (is (str/includes? message "line 100"))
    (is (str/includes? message "line 81"))
    (is (not (str/includes? message "line 80\n")))
    (is (not (str/includes? message "line 1\n")))))

(deftest marketplace-marker-is-case-sensitive-constant
  (is (= "already contains version" sut/already-published-marker))
  (doseq [log ["Already Contains Version 1.2.4" "ALREADY CONTAINS VERSION" "already  contains version" "contains version"]]
    (testing log
      (is (str/includes? (marketplace-error "1" log) "exit code 1")))))

(deftest marketplace-missing-log-file-fails-with-exit-code
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"exit code 3"
                        (sut/dispatch ["marketplace-outcome" "3" "/nonexistent/publish.log"]))))

(deftest marketplace-outcome-rejects-bad-arguments
  (doseq [args [["marketplace-outcome"]
                ["marketplace-outcome" "1"]
                ["marketplace-outcome" "abc" "publish.log"]
                ["marketplace-outcome" "" "publish.log"]]]
    (testing (pr-str args)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"^Usage: .* marketplace-outcome <exit-code>"
                            (sut/dispatch args))))))

(defn run-promotion
  [& args]
  (with-out-str (sut/dispatch (into ["promotion"] args))))

(deftest promotes-when-no-latest
  (is (= "promote\n" (run-promotion "v1.2.4")))
  (is (= "promote\n" (run-promotion "v1.2.4" "")))
  (is (= "promote\n" (run-promotion "v1.2.4" "  "))))

(deftest promotes-over-older-latest
  (is (= "promote\n" (run-promotion "v1.2.4" "v1.2.3")))
  (is (= "promote\n" (run-promotion "v1.10.0" "v1.9.9")))
  (is (= "promote\n" (run-promotion "v2.0.0" "v1.99.99"))))

(deftest unchanged-when-already-latest
  (is (= "unchanged\n" (run-promotion "v1.2.4" "v1.2.4"))))

(deftest superseded-by-newer-latest
  (is (= "superseded\n" (run-promotion "v1.2.4" "v1.2.5")))
  (is (= "superseded\n" (run-promotion "v1.9.9" "v1.10.0")))
  (is (= "superseded\n" (run-promotion "v1.99.99" "v2.0.0"))))

(deftest promotion-rejects-test-tags
  (doseq [args [["v1.2.4-test.abcdef1"]
                ["v1.2.4-test.abcdef1" "v1.2.3"]
                ["v1.2.4" "v1.2.5-test.abcdef1"]
                ["1.2.4"]
                ["v01.2.4" "v1.2.3"]
                ["v1.2.4" "latest"]]]
    (testing (pr-str args)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Promotion requires a stable tag"
                            (apply run-promotion args))))))

(deftest promotion-requires-tag
  (doseq [args [[] [""]]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"^Usage: .* promotion <tag>"
                          (apply run-promotion args)))))

