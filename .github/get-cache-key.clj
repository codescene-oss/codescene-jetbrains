#!/usr/bin/env bb
(require '[babashka.process :refer [shell]]
          '[babashka.fs :as fs])

(defn sha256-first12 [s]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        bytes (.getBytes (str s) "UTF-8")
        digest (.digest md bytes)
        hex (apply str (map #(format "%02x" %) digest))]
    (subs hex 0 12)))

(defn parse-status-line [line]
  (let [s (clojure.string/trim (subs line 3))]
    (if (re-find #" -> " s)
      (last (clojure.string/split s #" -> "))
      s)))

(defn -main []
  (let [tracked-out (shell {:out :string :err :string :continue true}
                          "git" "ls-files" "-s" "*.kt" "*.kts" "gradle.properties"
                          "gradle/libs.versions.toml" ".codescene/*.json")
        tracked (for [line (clojure.string/split-lines (:out tracked-out))
                     :when (seq line)
                     :let [parts (clojure.string/split line #"\s+" 4)]]
                 {:hash (nth parts 1) :path (nth parts 3)})
        status-out (shell {:out :string :err :string :continue true}
                         "git" "status" "--untracked-files=all" "--short" "--porcelain")
        paths (->> (clojure.string/split-lines (:out status-out))
                  (map parse-status-line)
                  (filter seq))
        matching (filter #(or (re-find #"\.(kt|kts)$" %)
                             (= % "gradle.properties")
                             (= % "gradle/libs.versions.toml")
                             (re-find #"^\.codescene/.*\.json$" %))
                        paths)
        existing (filter fs/exists? matching)
        untracked (for [p existing]
                   {:hash (clojure.string/trim (:out (shell {:out :string} "git" "hash-object" p)))
                    :path p})
        all (sort-by :path (concat tracked untracked))
        concat-hashes (apply str (map :hash all))
        input (if (clojure.string/blank? concat-hashes) "empty" concat-hashes)]
    (println (sha256-first12 input))))

(-main)
