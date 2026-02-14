#!/usr/bin/env bb
(require '[babashka.process :refer [shell]])

(defn -main []
  (let [p (shell {:continue true} "cs" "delta" "master" "--error-on-warnings")]
    (System/exit (:exit p))))

(-main)
