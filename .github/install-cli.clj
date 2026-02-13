#!/usr/bin/env bb
(require '[babashka.process :refer [shell]]
          '[babashka.fs :as fs]
          '[clojure.java.io :as io])

(def install-url "https://downloads.codescene.io/enterprise/cli/install-cs-tool.ps1")

(defn cs-available? []
  (= 0 (:exit (shell {:out :string :err :string :continue true} "cs" "--version"))))

(defn -main []
  (if (cs-available?)
    (System/exit 0)
    (let [tmp (fs/create-temp-file {:prefix "install-cs-tool" :suffix ".ps1"})
          _ (io/copy (slurp install-url) (io/file (str tmp)))
          p (shell {:dir (System/getProperty "user.dir")
                    :continue true}
                   "pwsh" "-NoProfile" "-ExecutionPolicy" "Bypass" "-File" (str tmp))]
      (fs/delete-if-exists tmp)
      (System/exit (:exit p)))))

(-main)
