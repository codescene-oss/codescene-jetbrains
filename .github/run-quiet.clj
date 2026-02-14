#!/usr/bin/env bb
(require '[babashka.process :refer [process]]
         '[babashka.fs :as fs])

(when (< (count *command-line-args*) 2)
  (binding [*out* *err*]
    (println "Usage: run-quiet.clj <log-file> <program> [args...]"))
  (System/exit 2))

(let [log-file (first *command-line-args*)
      prog-args (rest *command-line-args*)
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
      (fs/delete-if-exists log-file)
      (System/exit 0))
    (do
      (when (seq combined) (print combined))
      (flush)
      (System/exit exit))))
