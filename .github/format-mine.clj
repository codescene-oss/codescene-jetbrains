#!/usr/bin/env bb
(require '[babashka.process :refer [shell]]
         '[babashka.fs :as fs]
         '[clojure.string :as str])

(def gradlew (if (fs/windows?) ".\\gradlew.bat" "./gradlew"))

(defn mine-files []
  (let [result (shell {:out :string :err :string :continue true}
                      "bb" "-f" ".github/mine.clj")]
    (when (= 0 (:exit result))
      (->> (str/split-lines (:out result))
           (map str/trim)
           (filter seq)))))

(defn -main []
  (if-let [files (seq (mine-files))]
    (let [results (doall (for [f files]
                           (:exit (shell {:continue true}
                                         gradlew "ktlintFormat" (str "-PinternalKtlintGitFilter=" f) "--rerun-tasks"))))
          failed (first (filter #(not= 0 %) results))]
      (when failed
        (System/exit failed)))
    (println "No Kotlin files to format")))

(-main)
