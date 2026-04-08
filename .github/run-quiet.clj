#!/usr/bin/env bb
(require '[babashka.process :refer [process]]
         '[babashka.fs :as fs])

(when (< (count *command-line-args*) 3)
  (binding [*out* *err*]
    (println "Usage: run-quiet.clj <log-file> <description> <program> [args...]"))
  (System/exit 2))

(let [log-file (first *command-line-args*)
      description (second *command-line-args*)
      prog-args (drop 2 *command-line-args*)
      p @(apply process {:out :string :err :string :continue true} prog-args)
      exit (:exit p)
      out (or (:out p) "")
      err (or (:err p) "")
      combined (str out
                    (when (and (seq out) (seq err)) "\n")
                    err)]
  (spit log-file combined)
  (if (= 0 exit)
    (do
      (when (seq out)
        (let [lines (clojure.string/split-lines out)]
          (when (seq lines)
            (run! println (take-last 2 lines)))))
      (println (str "command `" description "` succeeded"))
      (fs/delete-if-exists log-file)
      (System/exit 0))
    (do
      (when (seq combined) (print combined))
      (println (str "command `" description "` failed. Logs available at " log-file))
      (flush)
      (System/exit exit))))
