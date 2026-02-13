#!/usr/bin/env bb
(require '[babashka.process :refer [shell]]
         '[babashka.fs :as fs])

(def gradlew (if (fs/windows?) ".\\gradlew.bat" "./gradlew"))

(let [args (concat [gradlew "--rerun-tasks" "--warn"] *command-line-args*)
      p (apply shell (into [{:continue true}] args))]
  (System/exit (:exit p)))