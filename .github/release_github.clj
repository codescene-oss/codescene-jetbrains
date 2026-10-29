#!/usr/bin/env bb
(ns release-github
  (:require [clojure.string :as str]))

(defn fail [message]
  (throw (ex-info message {})))

(def compose-notes-usage
  "Usage: bb --classpath .github -m release compose-notes <is-test> <stable-tag> <tag> <server-url> <repository> < generated-notes.md")

(def missing-assets-usage
  "Usage: bb --classpath .github -m release missing-assets <existing-names-file> <path>...")

(defn compare-link [stable-tag tag server-url repository]
  (str "[Full changes since " stable-tag "](" server-url "/" repository
       "/compare/" stable-tag "..." tag ")"))

(defn compose-notes-text [notes test? stable-tag tag server-url repository]
  (let [body (str/trimr notes)]
    (if (and test? (not (str/blank? stable-tag)))
      (str (compare-link stable-tag tag server-url repository)
           (when (seq body) (str "\n\n" body))
           "\n")
      (str body "\n"))))

(defn parse-flag [value]
  (case value
    "true" true
    "false" false
    (fail compose-notes-usage)))

(defn compose-notes [& [is-test stable-tag tag server-url repository]]
  (when (some nil? [is-test stable-tag tag server-url repository])
    (fail compose-notes-usage))
  (let [test? (parse-flag is-test)]
    (when (and test? (not (str/blank? stable-tag)) (some str/blank? [tag server-url repository]))
      (fail compose-notes-usage))
    (print (compose-notes-text (slurp *in*) test? stable-tag tag server-url repository))))

(defn asset-name [path]
  (last (str/split path #"[\\/]")))

(defn existing-asset-names [names-text]
  (set (remove str/blank? (map str/trim (str/split-lines names-text)))))

(defn unattached-paths [existing paths]
  (let [{missing false attached true} (group-by #(contains? existing (asset-name %)) paths)]
    {:missing (vec missing)
     :attached (vec attached)}))

(defn missing-assets [& [names-file & paths]]
  (when (or (str/blank? names-file) (empty? paths))
    (fail missing-assets-usage))
  (let [{:keys [missing attached]} (unattached-paths (existing-asset-names (slurp names-file)) paths)]
    (binding [*out* *err*]
      (doseq [path attached]
        (print (str "Asset " (asset-name path) " already attached, skipping\n")))
      (flush))
    (doseq [path missing]
      (print (str path "\n")))))
