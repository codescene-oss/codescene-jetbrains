#!/usr/bin/env bb
(ns migrate-changelog
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [release :as release]))

(def default-repo "codescene-oss/codescene-jetbrains")
(def changelog-path "CHANGELOG.md")
(def start-marker "<!-- changelog-migration:start -->")
(def end-marker "<!-- changelog-migration:end -->")
(def version-heading-pattern #"^#{2,3} \[(\d+\.\d+\.\d+)\] - .+$")
(def any-heading-pattern #"^#{1,3}(\s.*)?$")
(def block-pattern
  (re-pattern (str "(?s)" (java.util.regex.Pattern/quote start-marker)
                   ".*?" (java.util.regex.Pattern/quote end-marker))))
(def existing-block-pattern
  (re-pattern (str "(?s)\\s*" (java.util.regex.Pattern/quote start-marker)
                   ".*?" (java.util.regex.Pattern/quote end-marker))))

(def usage
  "Usage: bb --classpath .github -m migrate-changelog [--file <path>] [--repo <owner/name>] [--apply]")

(defn fail [message]
  (release/fail message))

(defn parse-args [args]
  (loop [[arg & more :as remaining] args
         options {:repo default-repo :apply false}]
    (cond
      (empty? remaining) options
      (= "--apply" arg) (recur more (assoc options :apply true))
      (#{"--file" "--repo"} arg)
      (let [[value & rest-args] more]
        (when (or (str/blank? value) (str/starts-with? value "--"))
          (fail (str "Missing value for " arg ".\n" usage)))
        (recur rest-args (assoc options (keyword (subs arg 2)) value)))
      :else (fail (str "Unknown option: " arg ".\n" usage)))))

(defn read-changelog-from-history []
  (let [deletion (release/sh "git" "log" "-1" "--diff-filter=D" "--format=%H" "--" changelog-path)]
    (when (str/blank? deletion)
      (fail (str "No deletion of " changelog-path " found in git history. "
                 "Pass --file <path> to read the changelog from a file.")))
    (release/sh "git" "show" (str deletion "^:" changelog-path))))

(defn read-changelog [file]
  (cond
    (nil? file) (read-changelog-from-history)
    (fs/exists? file) (slurp file)
    :else (fail (str "Changelog file not found: " file "."))))

(defn collect-sections [lines]
  (let [{:keys [sections current]}
        (reduce
         (fn [{:keys [sections current] :as state} line]
           (if-let [[_ version] (re-matches version-heading-pattern line)]
             {:sections (cond-> sections current (conj current))
              :current {:version version :lines []}}
             (if (re-matches any-heading-pattern line)
               {:sections (cond-> sections current (conj current))
                :current nil}
               (cond-> state current (update-in [:current :lines] conj line)))))
         {:sections [] :current nil}
         lines)]
    (cond-> sections current (conj current))))

(defn assert-unique-versions [sections]
  (doseq [[version occurrences] (group-by :version sections)]
    (when (> (count occurrences) 1)
      (fail (str "Version " version " appears more than once in the changelog.")))))

(defn parse-changelog [text]
  (let [sections (collect-sections (str/split-lines text))]
    (assert-unique-versions sections)
    (mapv (fn [{:keys [version lines]}]
            {:version version :body (str/trim (str/join "\n" lines))})
          sections)))

(defn changelog-block [section]
  (str start-marker "\n## Changelog\n\n" section "\n" end-marker))

(defn apply-changelog-block [body section]
  (let [remaining (str/trimr (str/replace (or body "") existing-block-pattern ""))
        block (changelog-block section)]
    (if (str/blank? remaining)
      block
      (str remaining "\n\n" block))))

(defn plan-section [releases {:keys [version body]}]
  (let [tag (str "v" version)
        release (first (filter #(and (release/stable-release? %) (= tag (:tag_name %))) releases))]
    (cond
      (str/blank? body) [:empty tag]
      (nil? release) [:unmatched tag]
      :else
      (let [current (or (:body release) "")
            new-body (apply-changelog-block current body)]
        (if (= current new-body)
          [:up-to-date tag]
          [:updates {:id (:id release) :tag tag :body current :new-body new-body}])))))

(defn plan-migration [sections releases]
  (reduce (fn [plan [bucket entry]] (update plan bucket conj entry))
          {:updates [] :up-to-date [] :unmatched [] :empty []}
          (map #(plan-section releases %) sections)))

(defn list-releases [repo]
  (release/parse-releases
   (release/sh "gh" "api" "--paginate" "--slurp" (str "repos/" repo "/releases?per_page=100"))))

(defn update-release-body [repo id body]
  (let [file (fs/create-temp-file {:prefix "release-body-" :suffix ".md"})]
    (try
      (spit (str file) body)
      (release/sh "gh" "api" "-X" "PATCH" (str "repos/" repo "/releases/" id)
                  "-F" (str "body=@" file))
      (finally
        (fs/delete-if-exists file)))))

(defn print-tags [label tags]
  (println (str label ": " (count tags)))
  (doseq [tag tags]
    (println (str "  " tag))))

(defn print-update [apply? {:keys [tag new-body]}]
  (println (str (if apply? "Updating " "Would update ") tag ":"))
  (println (re-find block-pattern new-body))
  (println))

(defn print-summary [{:keys [updates up-to-date unmatched empty]}]
  (print-tags "Releases to update" (map :tag updates))
  (print-tags "Already up to date" up-to-date)
  (print-tags "No matching stable release" unmatched)
  (print-tags "Empty changelog section" empty))

(defn run-migration [{:keys [repo] apply? :apply} changelog-text]
  (let [plan (plan-migration (parse-changelog changelog-text) (list-releases repo))]
    (doseq [{:keys [id new-body] :as planned} (:updates plan)]
      (print-update apply? planned)
      (when apply?
        (update-release-body repo id new-body)))
    (print-summary plan)
    (when-not apply?
      (println "Dry run: no releases were changed. Pass --apply to update them."))
    plan))

(defn migrate [args]
  (let [options (parse-args args)]
    (run-migration options (read-changelog (:file options)))))

(defn -main [& args]
  (try
    (migrate args)
    (catch clojure.lang.ExceptionInfo error
      (binding [*out* *err*]
        (println (ex-message error)))
      (System/exit 1))))
