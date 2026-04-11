#!/usr/bin/env bb
(require '[babashka.process :refer [shell]]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn get-changed-files []
  (let [status-result (shell {:out :string :continue true} "git status --short --porcelain")
        diff-result (shell {:out :string :continue true} "git diff --name-only master...HEAD")
        status-files (when (= 0 (:exit status-result))
                       (->> (str/split-lines (:out status-result))
                            (map #(subs % 3))
                            (map #(if (str/includes? % " -> ")
                                    (second (str/split % #" -> "))
                                    %))))
        diff-files (when (= 0 (:exit diff-result))
                     (str/split-lines (:out diff-result)))
        all-files (distinct (concat status-files diff-files))
        kt-files (filter #(re-matches #".*\.kt$" %) all-files)]
    kt-files))

(defn count-lines [file-path]
  (when (.exists (io/file file-path))
    (count (str/split-lines (slurp file-path)))))

(defn check-file-size [file-path max-lines]
  (when-let [line-count (count-lines file-path)]
    (when (> line-count max-lines)
      (format "%s has %d lines (max %d allowed). Consider splitting the file into smaller classes."
              file-path line-count max-lines))))

(defn -main []
  (let [max-lines 325
        changed-files (get-changed-files)
        violations (keep #(check-file-size % max-lines) changed-files)]
    (if (seq violations)
      (do
        (doseq [v violations]
          (binding [*out* *err*]
            (println v)))
        (System/exit 1))
      (System/exit 0))))

(-main)
