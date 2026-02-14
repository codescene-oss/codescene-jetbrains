#!/usr/bin/env bb
(require '[babashka.process :refer [shell]])

(defn- parse-status-line [line]
  (if (< (count line) 4)
    nil
    (let [prefix (subs line 0 2)]
      (if (re-find #"D" prefix)
        nil
        (let [s (subs line 3)
              path (clojure.string/trim s)]
          (if (re-find #" -> " path)
            (last (clojure.string/split path #" -> "))
            path))))))

(defn- changed-files []
  (let [result (shell {:out :string :err :string :continue true}
                      "git" "status" "--short" "--porcelain")
        paths (->> (clojure.string/split-lines (:out result))
                   (map parse-status-line)
                   (filter identity)
                   (filter seq))]
    (filter #(re-find #"\.(kt|kts)$" %) paths)))

(defn -main []
  (let [files (changed-files)]
    (if (seq files)
      (do (run! println files) (System/exit 0))
      (System/exit 1))))

(-main)
