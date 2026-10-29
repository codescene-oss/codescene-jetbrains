(ns release-workflow-test
  (:require [babashka.fs :as fs]
            [clj-yaml.core :as yaml]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [release :as sut]
            [release-test-support :as support]))

(defn read-yaml
  [& path]
  (yaml/parse-string (slurp (str (apply fs/path support/github-dir path)))))

(def workflow (delay (read-yaml "workflows" "release.yml")))

(def expected-needs
  {:metadata #{}
   :build #{"metadata"}
   :create-release #{"metadata"}
   :upload #{"metadata" "build" "create-release"}
   :test-gate #{"metadata" "upload"}
   :publish #{"metadata" "test-gate"}
   :record-version #{"metadata" "publish"}
   :promote #{"metadata" "publish" "record-version"}})

(def stable-only-jobs #{:test-gate :publish :record-version :promote})
(def write-jobs #{:create-release :upload :record-version :promote})
(def publish-secrets ["PUBLISH_TOKEN" "CERTIFICATE_CHAIN" "PRIVATE_KEY" "PRIVATE_KEY_PASSWORD"])
(def stable-only-condition "needs.metadata.outputs.is_test == 'false'")
(def strict-shell-header "set -euo pipefail")

(defn workflow-jobs
  []
  (:jobs @workflow))

(defn job-needs
  [job]
  (let [needs (:needs job)]
    (set (cond (nil? needs) [] (string? needs) [needs] :else needs))))

(defn job-steps
  [job]
  (:steps job))

(defn all-steps
  []
  (mapcat (comp job-steps val) (workflow-jobs)))

(defn run-blocks
  []
  (keep :run (all-steps)))

(defn command-lines
  [run-block]
  (remove #(or (str/blank? %) (str/starts-with? (str/trim %) "#"))
          (str/split-lines run-block)))

(defn calls-gh?
  [run-block]
  (boolean (re-find #"(?<![\w.-])gh\s" run-block)))

(defn needs-strict-shell?
  [run-block]
  (or (str/includes? run-block "|")
      (> (count (command-lines run-block)) 1)))

(defn invoked-subcommands
  []
  (->> (run-blocks)
       (mapcat #(re-seq #"bb --classpath (?:release-tooling/)?\.github -m release (\S+)" %))
       (map second)
       set))

(deftest release-workflow-structure-has-the-spec-graph
  (is (= (set (keys expected-needs)) (set (keys (workflow-jobs)))))
  (doseq [[job-id needs] expected-needs]
    (testing (name job-id)
      (is (= needs (job-needs (get (workflow-jobs) job-id)))))))

(deftest release-workflow-structure-limits-test-tags-to-the-first-four-jobs
  (doseq [[job-id job] (workflow-jobs)]
    (testing (name job-id)
      (if (contains? stable-only-jobs job-id)
        (is (= stable-only-condition (:if job)))
        (is (nil? (:if job)))))))

(deftest release-workflow-structure-triggers-and-concurrency
  (let [triggers (or (get @workflow true) (get @workflow :on))
        concurrency (:concurrency @workflow)]
    (is (= ["v*"] (get-in triggers [:push :tags])))
    (is (= {:required true :type "string"}
           (select-keys (get-in triggers [:workflow_dispatch :inputs :tag]) [:required :type])))
    (is (= "release" (:group concurrency)))
    (is (false? (:cancel-in-progress concurrency)))
    (is (= "max" (str (:queue concurrency))))))

(deftest release-workflow-structure-keeps-workflow-level-env-and-read-only-permissions
  (is (= {:contents "read"} (:permissions @workflow)))
  (is (= {:GH_USERNAME "${{ secrets.GH_USERNAME }}"
          :GH_PACKAGE_TOKEN "${{ secrets.GH_PACKAGE_TOKEN }}"
          :CODESCENE_IDE_DOCS_AND_WEBVIEW_TOKEN "${{ secrets.CODESCENE_IDE_DOCS_AND_WEBVIEW_TOKEN }}"}
         (:env @workflow))))

(deftest release-workflow-structure-resolves-the-tag-in-every-job
  (doseq [[job-id job] (workflow-jobs)]
    (testing (name job-id)
      (is (= (if (= :metadata job-id)
               "${{ inputs.tag || github.ref_name }}"
               "${{ needs.metadata.outputs.tag }}")
             (get-in job [:env :TAG]))))))

(deftest release-workflow-structure-confines-write-permissions
  (doseq [[job-id job] (workflow-jobs)]
    (testing (name job-id)
      (is (= (if (contains? write-jobs job-id) "write" "read")
             (get-in job [:permissions :contents])))
      (is (= (if (= :test-gate job-id) #{:contents :actions} #{:contents})
             (set (keys (:permissions job)))))
      (when (= :test-gate job-id)
        (is (= "read" (get-in job [:permissions :actions])))))))

(deftest release-workflow-structure-exposes-signing-secrets-only-to-publish-steps
  (doseq [[job-id job] (workflow-jobs)
          secret publish-secrets]
    (testing (str (name job-id) " " secret)
      (if (= :publish job-id)
        (do (is (str/includes? (pr-str (job-steps job)) secret))
            (is (not (str/includes? (pr-str (dissoc job :steps)) secret))))
        (is (not (str/includes? (pr-str job) secret))))))
  (doseq [secret publish-secrets]
    (is (not (str/includes? (pr-str (dissoc @workflow :jobs)) secret)))))

(deftest release-workflow-structure-uses-no-personal-access-token
  (let [text (slurp (str (fs/path support/github-dir "workflows" "release.yml")))
        secrets (set (map second (re-seq #"secrets\.(\w+)" text)))]
    (is (= #{"GH_USERNAME" "GH_PACKAGE_TOKEN" "CODESCENE_IDE_DOCS_AND_WEBVIEW_TOKEN" "FEATURE_CWF_DEVMODE"
             "PUBLISH_TOKEN" "CERTIFICATE_CHAIN" "PRIVATE_KEY" "PRIVATE_KEY_PASSWORD"}
           secrets))))

(deftest release-workflow-structure-invokes-only-registered-subcommands
  (is (seq (invoked-subcommands)))
  (is (every? #(contains? sut/commands %) (invoked-subcommands)))
  (is (= #{"metadata" "compose-notes" "missing-assets" "test-gate"
           "marketplace-outcome" "promotion" "record-version"}
         (invoked-subcommands))))

(deftest release-workflow-structure-gives-every-gh-step-a-token
  (let [gh-steps (filter #(some-> (:run %) calls-gh?) (all-steps))]
    (is (= 5 (count gh-steps)))
    (doseq [step gh-steps]
      (testing (:name step)
        (is (= "${{ github.token }}" (get-in step [:env :GH_TOKEN])))))))

(deftest release-workflow-structure-detects-gh-calls-precisely
  (is (calls-gh? "gh release view v1"))
  (is (calls-gh? "x | gh api y"))
  (is (not (calls-gh? "echo high water")))
  (is (not (calls-gh? "bash .github/verify_release_artifact.sh 1"))))

(deftest release-workflow-structure-starts-multi-command-blocks-with-strict-shell
  (is (seq (filter needs-strict-shell? (run-blocks))))
  (doseq [run-block (run-blocks)
          :when (needs-strict-shell? run-block)]
    (testing run-block
      (is (str/starts-with? run-block strict-shell-header)))))

(deftest release-workflow-structure-classifies-publish-outcome-after-the-gradle-run
  (let [publish-step (->> (job-steps (:publish (workflow-jobs)))
                          (filter #(str/includes? (str (:run %)) "publishPlugin"))
                          first)
        lines (command-lines (:run publish-step))
        index-of (fn [text] (first (keep-indexed #(when (str/includes? %2 text) %1) lines)))]
    (is (= "set -euo pipefail" (str/trim (first lines))))
    (is (< (index-of "set +e") (index-of "publishPlugin") (index-of "PIPESTATUS")
           (index-of "marketplace-outcome")))
    (is (= "set -e" (str/trim (nth lines (inc (index-of "PIPESTATUS"))))))))

(deftest release-workflow-structure-checks-out-the-tag-for-builds-and-master-for-recording
  (let [checkout-with (fn [job-id]
                        (->> (job-steps (get (workflow-jobs) job-id))
                             (filter #(str/starts-with? (str (:uses %)) "actions/checkout"))
                             first
                             :with))]
    (is (= "refs/tags/${{ needs.metadata.outputs.tag }}" (:ref (checkout-with :build))))
    (is (= "refs/tags/${{ needs.metadata.outputs.tag }}" (:ref (checkout-with :publish))))
    (is (= {:ref "master" :fetch-depth 0} (checkout-with :record-version)))
    (is (= {:fetch-depth 0} (checkout-with :metadata)))))

(deftest release-workflow-structure-sets-up-babashka-after-checkout-where-tooling-runs
  (doseq [job-id [:metadata :create-release :upload :test-gate :publish :record-version :promote]]
    (testing (name job-id)
      (let [uses (mapv #(str (:uses %)) (job-steps (get (workflow-jobs) job-id)))
            checkout-index (first (keep-indexed #(when (str/starts-with? %2 "actions/checkout") %1) uses))
            setup-index (.indexOf uses (if (= :publish job-id)
                                         "./release-tooling/.github/actions/setup-babashka"
                                         "./.github/actions/setup-babashka"))]
        (is (some? checkout-index))
        (is (< checkout-index setup-index)))))
  (is (not-any? #(str/includes? (str (:uses %)) "setup-babashka")
                (job-steps (:build (workflow-jobs))))))

(deftest release-workflow-structure-runs-publish-tooling-from-the-workflow-ref
  (let [steps (job-steps (:publish (workflow-jobs)))
        checkouts (filterv #(str/starts-with? (str (:uses %)) "actions/checkout") steps)
        tooling (second checkouts)
        runs (str/join "\n" (keep :run steps))]
    (is (= 2 (count checkouts)))
    (is (= "release-tooling" (get-in tooling [:with :path])))
    (is (not (contains? (:with tooling) :ref)))
    (is (str/includes? runs "bb --classpath release-tooling/.github -m release marketplace-outcome"))
    (is (not (re-find #"bb --classpath \.github" runs)))
    (is (not-any? #(= "./.github/actions/setup-babashka" (:uses %)) steps))))

(deftest setup-babashka-action-installs-the-pinned-release
  (let [action (read-yaml "actions" "setup-babashka" "action.yml")
        steps (get-in action [:runs :steps])
        script (str/join "\n" (keep :run steps))]
    (is (= "composite" (get-in action [:runs :using])))
    (is (= "1.12.218" (get-in action [:inputs :version :default])))
    (is (= "bash" (:shell (first steps))))
    (is (str/includes? script "babashka-${BABASHKA_VERSION}-linux-amd64-static.tar.gz"))
    (is (str/includes? script "$GITHUB_PATH"))
    (is (str/includes? script "--version"))
    (is (str/starts-with? script strict-shell-header))))

(deftest build-workflow-has-no-changelog-step
  (let [build-workflow (read-yaml "workflows" "build.yml")
        jobs (:jobs build-workflow)
        steps (mapcat :steps (vals jobs))]
    (is (seq steps))
    (is (not-any? #(str/includes? (str (:run %)) "getChangelog") steps))
    (is (not-any? #(str/includes? (str (:run %)) "changelog") steps))
    (is (not-any? #(contains? (:outputs (val %)) :changelog) jobs))
    (is (every? #(contains? (get-in jobs [:build :outputs]) %) [:version :pluginVerifierHomeDir]))))

(deftest release-metadata-script-is-gone
  (is (not (fs/exists? (fs/path support/github-dir "release_metadata.sh")))))

(def repo-root (fs/parent support/github-dir))

(defn read-doc
  [& path]
  (slurp (str (apply fs/path path))))

(defn mentions-make-release-command?
  [text]
  (boolean (re-find #"make release(\s|$)" text)))

(deftest release-docs-do-not-mention-retired-flow
  (let [releasing (read-doc support/github-dir "RELEASING.md")
        readme (read-doc repo-root "README.md")]
    (testing "no retired make release command"
      (is (not (mentions-make-release-command? releasing)))
      (is (not (mentions-make-release-command? readme))))
    (testing "no changelog file reference"
      (is (not (str/includes? releasing "CHANGELOG.md")))
      (is (not (str/includes? readme "CHANGELOG.md"))))
    (testing "RELEASING.md has the tag-driven sections"
      (doseq [heading ["Stable releases" "What the workflow does" "Release notes" "Recovery"
                       "Abandoning a stable release" "Test releases" "Installing a test release"
                       "Changelog history" "Requirements"]]
        (is (re-find (re-pattern (str "(?m)^#{2,3} " heading "$")) releasing) heading)))
    (testing "README links the release guide"
      (is (str/includes? readme "(.github/RELEASING.md)"))
      (is (str/includes? readme "make test-release [BUMP=patch|minor|major]")))))

(deftest retired-flow-detector-distinguishes-test-release-command
  (is (mentions-make-release-command? "run make release BUMP=patch"))
  (is (mentions-make-release-command? "run make release"))
  (is (mentions-make-release-command? "make release
next"))
  (is (not (mentions-make-release-command? "run make test-release BUMP=minor"))))

