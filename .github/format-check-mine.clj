#!/usr/bin/env bb
(require '[babashka.process :refer [shell]]
         '[babashka.fs :as fs]
         '[clojure.string :as str])

(def gradlew (if (fs/windows?) ".\\gradlew.bat" "./gradlew"))
(def reports-dir "build/reports/ktlint")

(def summary-start "Summary error count (descending) by rule:")

(defn strip-summary [s]
  (if-let [idx (str/index-of s summary-start)]
    (str/trim (subs s 0 idx))
    s))

(defn read-violation-reports []
  (when (fs/exists? reports-dir)
    (->> (fs/glob reports-dir "**/*.txt")
         (filter #(pos? (fs/size %)))
         (map #(strip-summary (str/trim (slurp (str %)))))
         (filter seq)
         (str/join "\n\n"))))

(defn mine-files []
  (let [result (shell {:out :string :err :string :continue true}
                      "bb" "-f" ".github/mine.clj")]
    (when (= 0 (:exit result))
      (->> (str/split-lines (:out result))
           (map str/trim)
           (filter seq)))))

(defn check-file [f accum]
  (let [p (shell {:out :string :err :string :continue true}
                 gradlew "ktlintCheck" (str "-PinternalKtlintGitFilter=" f)
                 "-PktlintFailOnError=true"
                 "--rerun-tasks" "--quiet")
        exit (:exit p)
        content (when (not= 0 exit) (read-violation-reports))]
    (when (and content (seq content))
      (swap! accum conj content))
    exit))

(defn -main []
  (if-let [files (seq (mine-files))]
    (let [accum (atom [])
          results (doall (map #(check-file % accum) files))
          failed (first (filter #(not= 0 %) results))]
      (when failed
        (run! println @accum)
        (System/exit failed)))
    (println "No Kotlin files to check")))

(-main)
