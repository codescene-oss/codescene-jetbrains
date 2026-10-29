(ns release-test-support
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.string :as str]
            [release :as sut]))

(def github-dir (str (fs/parent *file*)))

(def repositories (atom []))

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
        (str "pluginName = CodeScene\npluginVersion = " version "\n")))

(defn create-repository
  [version]
  (let [repo (fs/create-temp-dir {:prefix "codescene-release-test-"})]
    (swap! repositories conj repo)
    (write-project-files repo version)
    (git repo "init" "-b" "master")
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

(defn cleanup-repositories
  [test-fn]
  (try
    (test-fn)
    (finally
      (doseq [repo @repositories]
        (fs/delete-tree repo {:force true}))
      (reset! repositories []))))

(defn release
  [tag & {:keys [draft prerelease] :or {draft false prerelease false}}]
  {:tag_name tag :draft draft :prerelease prerelease})

(defn exact-message
  [message]
  (re-pattern (str "^" (java.util.regex.Pattern/quote message) "$")))

(defn test-entry
  [tag created-at]
  {:tag_name tag :draft false :prerelease true :created_at created-at})

(defn commit!
  [repo message]
  (git repo "commit" "--allow-empty" "-m" message)
  (git repo "rev-parse" "HEAD"))

(defn annotate!
  [repo tag]
  (git repo "tag" "-a" tag "-m" tag))
