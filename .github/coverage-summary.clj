#!/usr/bin/env bb
(require '[clojure.java.io :as io])

(def default-report "build/reports/jacoco/jacocoMergedReport/jacocoMergedReport.xml")

(def report-file
  (let [f (or (first *command-line-args*) default-report)]
    (when-not (.exists (io/file f))
      (binding [*out* *err*]
        (println (str "Report not found: " f))
        (println "Run: ./gradlew fetchCwf jacocoMergedReport"))
      (System/exit 1))
    f))

(def xml-content (slurp report-file))

(def counter-re #"<counter type=\"([^\"]+)\" missed=\"(\d+)\" covered=\"(\d+)\"/>")

(defn parse-counters [s]
  (->> (re-seq counter-re s)
       (map (fn [[_ type missed covered]]
              [type {:missed (parse-long missed) :covered (parse-long covered)}]))
       (into {})))

(defn pct [{:keys [missed covered]}]
  (let [total (+ missed covered)]
    (if (zero? total) 0.0 (* 100.0 (/ covered total)))))

(defn fmt-pct [counters type]
  (if-let [c (get counters type)]
    (format "%5.1f%%" (pct c))
    "    -"))

(defn fmt-ratio [counters type]
  (if-let [{:keys [missed covered]} (get counters type)]
    (format "%d/%d" covered (+ missed covered))
    "-"))

(defn pad-right [s n]
  (format (str "%-" n "s") s))

(defn print-separator [col1-width]
  (println (str (apply str (repeat col1-width \-)) " | ---------- | ------ | ------")))

(defn print-header [label col1-width]
  (println (str (pad-right label col1-width) " | Lines      | Line%  | Branch%")))

(defn extract-packages []
  (->> (re-seq #"<package name=\"([^\"]+)\">(.*?)</package>" xml-content)
       (map (fn [[block name content]]
              {:name (.replace name "/" ".")
               :counters (parse-counters block)
               :classes (->> (re-seq #"<class name=\"([^\"]+)\"[^>]*>(.*?)</class>" content)
                             (map (fn [[block full-name class-content]]
                                    {:name (last (.split full-name "/"))
                                     :full-name (.replace full-name "/" ".")
                                     :counters (parse-counters block)})))}))))

(defn report-counters []
  (let [tail (subs xml-content (max 0 (- (count xml-content) 500)))]
    (parse-counters (subs tail (.lastIndexOf tail "</package>")))))

;; Overall
(let [counters (report-counters)]
  (println "=== Code Coverage Summary ===")
  (println)
  (println (format "Overall: %s line | %s branch"
                   (fmt-pct counters "LINE")
                   (fmt-pct counters "BRANCH")))
  (println (format "         %s lines | %s branches"
                   (fmt-ratio counters "LINE")
                   (fmt-ratio counters "BRANCH")))
  (println))

;; Per package
(let [col1 60
      pkg-data (->> (extract-packages)
                    (sort-by #(pct (get (:counters %) "LINE"))))]
  (println "--- Packages (sorted by line coverage) ---")
  (println)
  (print-header "Package" col1)
  (print-separator col1)
  (doseq [{:keys [name counters]} pkg-data]
    (println (str (pad-right name col1) " | "
                  (pad-right (fmt-ratio counters "LINE") 10) " | "
                  (fmt-pct counters "LINE") " | "
                  (fmt-pct counters "BRANCH"))))
  (println)

  ;; Lowest coverage classes
  (let [col1 70
        class-data (->> pkg-data
                        (mapcat :classes)
                        (filter #(let [c (get (:counters %) "LINE")]
                                   (and c (pos? (+ (:missed c) (:covered c))))))
                        (sort-by #(pct (get (:counters %) "LINE")))
                        (take 30))]
    (println "--- Lowest Coverage Classes (bottom 30) ---")
    (println)
    (print-header "Class" col1)
    (print-separator col1)
    (doseq [{:keys [full-name counters]} class-data]
      (println (str (pad-right full-name col1) " | "
                    (pad-right (fmt-ratio counters "LINE") 10) " | "
                    (fmt-pct counters "LINE") " | "
                    (fmt-pct counters "BRANCH"))))))
