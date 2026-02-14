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

(defn path-to-class [path]
  (-> path
      (str/replace #"^src[\\/]test[\\/]kotlin[\\/]" "")
      (str/replace #"[\\/]" ".")
      (str/replace #"\.(kt|kts)$" "")))

(defn test-classes-for-files [files]
  (->> files
       (filter #(re-find #"Test\.(kt|kts)$" %))
       (map path-to-class)
       (filter seq)))

(defn print-test-failure! [result]
  (when (:out result) (print (:out result)))
  (when (:err result) (binding [*err* *out*] (print (:err result))))
  (flush)
  (System/exit (:exit result)))

(defn run-tests! [test-classes]
  (let [args (mapcat #(vector "--tests" %) test-classes)
        result (apply shell (concat [{:continue true :out :string :err :string}]
                                    [gradlew "test"] args))]
    (if (= 0 (:exit result))
      (let [lines (some-> (:out result) str/split-lines)
            last-10 (when lines (take-last 10 lines))]
        (run! println last-10))
      (print-test-failure! result))))

(defn -main []
  (some-> (mine-files)
          seq
          test-classes-for-files
          seq
          run-tests!))

(-main)
