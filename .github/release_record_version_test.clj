(ns release-record-version-test
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [release :as sut]
            [release-test-support :as support :refer [cleanup-repositories create-repository git with-repository
                                                      write-project-files]]))

(use-fixtures :each cleanup-repositories)

(defn clone-repository
  [source]
  (let [dir (fs/create-temp-dir {:prefix "codescene-release-clone-"})]
    (swap! support/repositories conj dir)
    (git source "clone" "--quiet" (str source) (str dir))
    (git dir "config" "user.email" "other@example.com")
    (git dir "config" "user.name" "Other User")
    dir))

(defn create-origin-and-checkout
  [version]
  (let [seed (create-repository version)
        origin (fs/create-temp-dir {:prefix "codescene-release-origin-"})]
    (swap! support/repositories conj origin)
    (git seed "clone" "--quiet" "--bare" (str seed) (str origin))
    {:origin origin
     :work (clone-repository origin)}))

(defn push-competing-commit
  [origin & [version]]
  (let [other (clone-repository origin)]
    (when version
      (write-project-files other version))
    (spit (str (fs/path other "competing.txt")) "competing")
    (git other "add" ".")
    (git other "commit" "-m" "Competing change")
    (git other "push" "--quiet" "origin" "HEAD:refs/heads/master")
    (git other "rev-parse" "HEAD")))

(defn origin-master
  [origin]
  (git origin "rev-parse" "refs/heads/master"))

(defn origin-plugin-version
  [origin]
  (second (re-find #"(?m)^pluginVersion = (.+)$"
                   (git origin "show" "refs/heads/master:gradle.properties"))))

(defn run-record-version
  [work & args]
  (with-repository work #(with-out-str (apply sut/record-version args))))

(def bot-identity "github-actions[bot]|41898282+github-actions[bot]@users.noreply.github.com")

(deftest records-higher-version-as-single-file-bot-commit
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        before (origin-master origin)
        output (run-record-version work "1.2.4")]
    (is (= "1.2.4" (origin-plugin-version origin)))
    (is (= before (git origin "rev-parse" "refs/heads/master~1")))
    (is (= (str bot-identity "|chore(release): set pluginVersion to 1.2.4")
           (git origin "log" "-1" "--format=%an|%ae|%s" "refs/heads/master")))
    (is (= bot-identity
           (git origin "log" "-1" "--format=%cn|%ce" "refs/heads/master")))
    (is (= "gradle.properties"
           (git origin "show" "--name-only" "--format=" "refs/heads/master")))
    (is (= "pluginName = CodeScene\npluginVersion = 1.2.4"
           (str/trim (git origin "show" "refs/heads/master:gradle.properties"))))
    (is (= (origin-master origin) (git work "rev-parse" "HEAD")))
    (is (= "master" (git work "rev-parse" "--abbrev-ref" "HEAD")))
    (is (= "" (git work "status" "--short")))
    (is (str/includes? output "1.2.4"))))

(deftest records-onto-newer-remote-tip-when-local-checkout-is-stale
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        competing (push-competing-commit origin)]
    (run-record-version work "1.2.4")
    (is (= competing (git origin "rev-parse" "refs/heads/master~1")))
    (is (= "1.2.4" (origin-plugin-version origin)))
    (is (str/includes? (git origin "ls-tree" "--name-only" "refs/heads/master") "competing.txt"))))

(deftest no-op-when-master-already-at-version
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        before (origin-master origin)
        pushes (atom 0)
        output (with-redefs [sut/push-master #(swap! pushes inc)]
                 (run-record-version work "1.2.3"))]
    (is (= before (origin-master origin)))
    (is (zero? @pushes))
    (is (str/includes? output "nothing to record"))
    (is (= "" (git work "status" "--short")))))

(deftest no-op-when-master-is-newer
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        _ (push-competing-commit origin "1.3.0")
        before (origin-master origin)
        pushes (atom 0)
        output (with-redefs [sut/push-master #(swap! pushes inc)]
                 (run-record-version work "1.2.4"))]
    (is (= before (origin-master origin)))
    (is (= "1.3.0" (origin-plugin-version origin)))
    (is (zero? @pushes))
    (is (str/includes? output "nothing to record"))
    (is (= "1.3.0" (with-repository work sut/base-plugin-version)))))

(deftest compares-versions-numerically-when-deciding-no-op
  (let [{:keys [origin work]} (create-origin-and-checkout "1.9.0")]
    (run-record-version work "1.10.0")
    (is (= "1.10.0" (origin-plugin-version origin)))
    (run-record-version work "1.9.5")
    (is (= "1.10.0" (origin-plugin-version origin)))))

(deftest retries-after-rejected-push
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        real-push sut/push-master
        calls (atom 0)
        competing (atom nil)
        output (with-redefs [sut/push-master (fn []
                                               (when (= 1 (swap! calls inc))
                                                 (reset! competing (push-competing-commit origin)))
                                               (real-push))]
                 (run-record-version work "1.2.4"))]
    (is (= 2 @calls))
    (is (= "1.2.4" (origin-plugin-version origin)))
    (is (= @competing (git origin "rev-parse" "refs/heads/master~1")))
    (is (= "chore(release): set pluginVersion to 1.2.4"
           (git origin "log" "-1" "--format=%s" "refs/heads/master")))
    (is (= "gradle.properties"
           (git origin "show" "--name-only" "--format=" "refs/heads/master")))
    (is (= (origin-master origin) (git work "rev-parse" "HEAD")))
    (is (str/includes? output "Recorded pluginVersion 1.2.4"))))

(deftest retry-becomes-no-op-when-competing-commit-records-the-version
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        real-push sut/push-master
        calls (atom 0)
        competing (atom nil)
        output (with-redefs [sut/push-master (fn []
                                               (when (= 1 (swap! calls inc))
                                                 (reset! competing (push-competing-commit origin "1.2.4")))
                                               (real-push))]
                 (run-record-version work "1.2.4"))]
    (is (= 1 @calls))
    (is (= @competing (origin-master origin)))
    (is (str/includes? output "nothing to record"))))

(deftest fails-after-exhausting-attempts
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        before (origin-master origin)
        calls (atom 0)]
    (with-redefs [sut/push-master (fn []
                                    (swap! calls inc)
                                    (throw (ex-info "rejected" {})))]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"1\.2\.4.* 5 attempts"
                            (run-record-version work "1.2.4"))))
    (is (= 5 @calls))
    (is (= before (origin-master origin)))))

(deftest does-not-retry-failures-other-than-push
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        pushes (atom 0)]
    (push-competing-commit origin)
    (with-redefs [sut/push-master #(swap! pushes inc)
                  sut/update-plugin-version (fn [version]
                                              (spit (str (fs/path work "gradle.properties")) version)
                                              (spit (str (fs/path work "competing.txt")) "changed"))]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Expected only gradle.properties to change"
                            (run-record-version work "1.2.4"))))
    (is (zero? @pushes))))

(deftest rejects-unexpected-changed-files
  (is (nil? (sut/ensure-only-version-file-changed "gradle.properties\n")))
  (doseq [changed ["gradle.properties\nCHANGELOG.md" "CHANGELOG.md" "" "\n"
                   "sub/gradle.properties"]]
    (testing (pr-str changed)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Expected only gradle.properties to change"
                            (sut/ensure-only-version-file-changed changed))))))

(deftest record-version-rejects-bad-versions
  (let [{:keys [origin work]} (create-origin-and-checkout "1.2.3")
        before (origin-master origin)]
    (doseq [args [[] [""] ["v1.2.4"] ["1.2"] ["1.2.4-test.abcdef1"]]]
      (testing (pr-str args)
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"Usage: .* record-version <version>|x\.y\.z"
                              (apply run-record-version work args)))))
    (is (= before (origin-master origin)))))

(deftest record-version-is-a-registered-command
  (is (identical? sut/record-version (get sut/commands "record-version"))))

