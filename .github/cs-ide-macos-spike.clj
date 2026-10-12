#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]]
         '[cheshire.core :as json]
         '[clojure.string :as str])
(import '[java.io BufferedReader InputStream OutputStream]
        '[java.util.concurrent LinkedBlockingQueue TimeUnit])

(def start-timeout-ms 60000)
(def inventory-timeout-ms 30000)
(def review-timeout-ms 60000)
(def poll-ms 25)

(def simple-kotlin
  "class Calculator {\n    fun add(a: Int, b: Int): Int = a + b\n}\n")

(def complex-kotlin
  "class ComplexProcessor {\n    fun process(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int): Int {\n        if (a > 0) {\n            if (b > 0) {\n                if (c > 0) {\n                    if (d > 0) {\n                        return a + b + c + d + e + f + g + h\n                    }\n                }\n            }\n        }\n        return 0\n    }\n}\n")

(def ^:private eof ::eof)

(defn usage! []
  (binding [*out* *err*]
    (println "Usage: cs-ide-macos-spike.clj <java> <extension-api-jar>"))
  (System/exit 2))

(defn default-threads []
  (max 1 (quot (.availableProcessors (Runtime/getRuntime)) 2)))

(defn git! [repo & args]
  (let [result (apply shell {:dir (str repo) :out :string :err :string :continue true}
                      "git" args)]
    (when (pos? (:exit result))
      (throw (ex-info (str "git failed: " (:err result)) result)))
    (str/trim (or (:out result) ""))))

(defn create-repo! []
  (let [root (fs/create-temp-dir {:prefix "cs-ide-macos-spike-"})
        rel-path "Sample.kt"]
    (spit (fs/file root rel-path) simple-kotlin)
    (git! root "init")
    (git! root "config" "user.email" "spike@codescene.local")
    (git! root "config" "user.name" "cs-ide spike")
    (git! root "add" ".")
    (git! root "commit" "-m" "initial")
    {:root (str (fs/canonicalize root))
     :rel-path rel-path}))

(defn- read-header-line [^InputStream in]
  (let [sb (StringBuilder.)]
    (loop []
      (let [b (.read in)]
        (cond
          (neg? b) (if (zero? (.length sb)) eof (.toString sb))
          (= b 10) (.toString sb)
          (= b 13) (recur)
          :else (do (.append sb (char b)) (recur)))))))

(defn- read-frame [^InputStream in]
  (loop [length nil]
    (let [line (read-header-line in)]
      (cond
        (= eof line) nil
        (str/blank? line)
        (when length
          (let [body (.readNBytes in length)]
            (when (= length (alength body))
              (json/parse-string (String. body "UTF-8")))))
        :else
        (recur (or length
                   (some-> (re-matches #"Content-Length:\s*(\d+)" line)
                           second
                           Integer/parseInt)))))))

(defn- write-frame! [^OutputStream out lock message]
  (let [body (json/generate-string message)
        body-bytes (.getBytes body "UTF-8")
        header (str "Content-Length: " (alength body-bytes) "\r\n\r\n")]
    (locking lock
      (doto out
        (.write (.getBytes header "US-ASCII"))
        (.write body-bytes)
        (.flush)))))

(defn start-rpc-client [^InputStream in ^OutputStream out]
  (let [notifications (LinkedBlockingQueue.)
        stream-error (atom nil)
        reader (Thread.
                (fn []
                  (try
                    (loop []
                      (when-let [message (read-frame in)]
                        (.put notifications {:message message})
                        (recur)))
                    (catch Exception e
                      (reset! stream-error e))))
                "cs-ide-macos-spike-reader")]
    (.setDaemon reader true)
    (.start reader)
    {:in in
     :out out
     :write-lock (Object.)
     :notifications notifications
     :stream-error stream-error
     :reader reader}))

(defn notify! [{:keys [out write-lock] :as _client} method params]
  (write-frame! out write-lock {"jsonrpc" "2.0" "method" method "params" params}))

(defn take-notification! [client timeout-ms]
  (.poll ^LinkedBlockingQueue (:notifications client) timeout-ms TimeUnit/MILLISECONDS))

(defn await-notification [client method deadline-ms]
  (loop []
    (let [remaining (- deadline-ms (System/currentTimeMillis))
          frame (take-notification! client (max 1 (min poll-ms remaining)))]
      (cond
        (and frame (= method (get-in frame [:message "method"]))) (:message frame)
        (< remaining 0) nil
        frame (recur)
        :else (recur)))))

(defn await-file-review [client rel-path deadline-ms]
  (loop []
    (let [remaining (- deadline-ms (System/currentTimeMillis))
          frame (take-notification! client (max 1 (min poll-ms remaining)))]
      (cond
        (< remaining 0) nil
        (and frame (= "cs-ide/fileReview" (get-in frame [:message "method"]))
             (= rel-path (get-in frame [:message "params" "path"]))) (:message frame)
        frame (recur)
        :else (recur)))))

(defn drain-stderr! [^Process process lines]
  (doto (Thread.
         (fn []
           (try
             (with-open [^BufferedReader reader (java.io.BufferedReader. (.getErrorStream process))]
               (loop []
                 (when-let [line (.readLine reader)]
                   (swap! lines conj line)
                   (recur))))
             (catch Exception _ nil)))
         "cs-ide-macos-spike-stderr")
    (.setDaemon true)
    (.start)))

(defn linkage-lines [stderr-lines]
  (vec (filter #(or (str/includes? % "LinkageError")
                    (str/includes? % "UnsatisfiedLinkError"))
               @stderr-lines)))

(defn server-command [java jar threads]
  [java
   "--enable-native-access=ALL-UNNAMED"
   "-Xmx2g"
   "-cp"
   jar
   "clojure.main"
   "-m"
   "codescene.cli.ide"
   "server"
   "--threads"
   (str threads)])

(defn destroy-process! [^Process process]
  (when (.isAlive process)
    (try
      (let [children (.toList (.descendants process))]
        (doseq [^java.lang.ProcessHandle child children]
          (try (.destroy child) (catch Exception _))))
      (catch Exception _))
    (try (.destroy process) (catch Exception _))
    (try (.waitFor process 3 TimeUnit/SECONDS) (catch Exception _))
    (when (.isAlive process)
      (try
        (let [children (.toList (.descendants process))]
          (doseq [^java.lang.ProcessHandle child children]
            (try (.destroyForcibly child) (catch Exception _))))
        (catch Exception _))
      (try (.destroyForcibly process) (catch Exception _))))
  (try (.waitFor process 10 TimeUnit/SECONDS) (catch Exception _)))

(defn delete-repo! [root]
  (try
    (fs/delete-tree root {:fail-if-not-exists false})
    (catch Exception _
      (Thread/sleep 500)
      (try
        (fs/delete-tree root {:fail-if-not-exists false})
        (catch Exception _ nil)))))

(defn run-spike! [java jar]
  (let [repo (create-repo!)
        stderr (atom [])
        threads (default-threads)
        ^Process process (.start (ProcessBuilder. ^java.util.List (vec (server-command java jar threads))))
        _ (drain-stderr! process stderr)
        client (start-rpc-client (.getInputStream process) (.getOutputStream process))
        start-deadline (+ (System/currentTimeMillis) start-timeout-ms)
        start (await-notification client "cs-ide/start" start-deadline)
        result (atom nil)]
    (try
      (if-not start
        (reset! result {:pass false :reason "cs-ide/start not received"})
        (do
          (notify! client "cs-ide/watchFiles"
                   {"repo-root" (:root repo)
                    "relative-paths" ["."]})
          (let [inventory-deadline (+ (System/currentTimeMillis) inventory-timeout-ms)
                inventory (await-notification client "cs-ide/watchInventoryChanged" inventory-deadline)]
            (if-not inventory
              (reset! result {:pass false
                              :reason "cs-ide/watchInventoryChanged not received within 30s"})
              (do
                (spit (fs/file (:root repo) (:rel-path repo)) complex-kotlin)
                (let [review-deadline (+ (System/currentTimeMillis) review-timeout-ms)
                      review (await-file-review client (:rel-path repo) review-deadline)]
                  (if review
                    (reset! result {:pass true})
                    (reset! result {:pass false
                                    :reason (str "cs-ide/fileReview for "
                                                 (:rel-path repo)
                                                 " not received within 60s")}))))))))
      (catch Throwable t
        (reset! result {:pass false :reason (str "spike error: " (.getMessage t))}))
      (finally
        (try (.close ^OutputStream (:out client)) (catch Exception _))
        (destroy-process! process)
        (delete-repo! (:root repo))))
    (assoc @result :stderr (linkage-lines stderr))))

(defn -main [& args]
  (let [[java jar] args]
    (when (or (str/blank? java) (str/blank? jar))
      (usage!))
    (when-not (fs/regular-file? java)
      (binding [*out* *err*]
        (println "Java executable not found:" java))
      (System/exit 2))
    (when-not (fs/regular-file? jar)
      (binding [*out* *err*]
        (println "Extension API jar not found:" jar))
      (System/exit 2))
    (let [{:keys [pass reason stderr]} (run-spike! (str (fs/canonicalize java)) (str (fs/canonicalize jar)))]
      (when (seq stderr)
        (println "stderr (linkage):")
        (run! println stderr))
      (if pass
        (do (println "PASS") (System/exit 0))
        (do (println "FAIL" (or reason "")) (System/exit 1))))))

(apply -main *command-line-args*)
