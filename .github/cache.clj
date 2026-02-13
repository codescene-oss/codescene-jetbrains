#!/usr/bin/env bb
(require '[babashka.process :refer [process]]
         '[babashka.fs :as fs])

(defn sha256 [s]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        bytes (.getBytes (str s) "UTF-8")
        digest (.digest md bytes)]
    (apply str (map #(format "%02x" %) digest))))

(defn cache-files [cache-key command cache-dir]
  (let [command-hash (sha256 command)
        cache-path (fs/path cache-dir cache-key command-hash)]
    {:cache-path cache-path
     :stdout-file (fs/path cache-path "stdout")
     :stderr-file (fs/path cache-path "stderr")
     :exitcode-file (fs/path cache-path "exitcode")}))

(defn print-output [stdout stderr]
  (when (seq stdout) (print stdout))
  (when (seq stderr) (print stderr))
  (flush))

(defn read-cached-result [{:keys [stdout-file stderr-file exitcode-file]}]
  (let [stdout (slurp (str stdout-file))
        stderr (if (fs/exists? (str stderr-file))
                 (slurp (str stderr-file))
                 "")
        exit (Integer/parseInt (slurp (str exitcode-file)))]
    (print-output stdout stderr)
    exit))

(defn execute-command [command]
  (let [normalized-command (if (and (fs/windows?)
                                    (>= (count command) 2)
                                    (= \' (first command))
                                    (= \' (last command)))
                             (subs command 1 (dec (count command)))
                             command)
        shell (if (fs/windows?) "cmd.exe" "/bin/sh")
        shell-arg (if (fs/windows?) "/c" "-c")
        p @(process {:out :string :err :string
                     :continue true
                     :dir (System/getProperty "user.dir")}
                    shell shell-arg normalized-command)]
    {:exit (:exit p)
     :stdout (or (:out p) "")
     :stderr (or (:err p) "")}))

(defn write-cached-result [{:keys [cache-path stdout-file stderr-file exitcode-file]}
                           {:keys [stdout stderr exit]}]
  (fs/create-dirs cache-path)
  (spit (str stdout-file) stdout)
  (spit (str stderr-file) stderr)
  (spit (str exitcode-file) (str exit)))

(defn run-cached [cache-key command cache-dir]
  (let [{:keys [stdout-file] :as files} (cache-files cache-key command cache-dir)]
    (if (fs/exists? (str stdout-file))
      (read-cached-result files)
      (let [result (execute-command command)]
        (write-cached-result files result)
        (print-output (:stdout result) (:stderr result))
        (:exit result)))))

(when (= (count *command-line-args*) 3)
  (let [[cache-key command cache-dir] *command-line-args*
        exit (run-cached cache-key command cache-dir)]
    (System/exit exit)))
