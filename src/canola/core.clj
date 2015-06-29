(ns canola.core
  "Canola's Main"
  (:gen-class)
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.java.io :as io]
    [clojure.string :refer [join split]]
    [clojure.tools.cli :refer [parse-opts]]
    [canola.compile]
    [canola.config :refer [config]]
    [clj-time.format]
    [me.raynes.fs :as fs]
    [selmer.parser :refer [set-resource-path!]])
  (:import
    [java.io File]
    [java.nio.file Paths]))

(def cwd (System/getProperty "user.dir"))

(defn spit-path [path content]
  (spit (.toString path) content))

(defn path-join [& args]
  ;(Paths/get "" (into-array String args)))
  (->> (into-array String args)
       (Paths/get "")))

(defn organize-yearmonth
  [content]
  (let [pubdate (content :pubdate)
        formater (clj-time.format/formatter "yyyyMM")]
    (clj-time.format/unparse formater pubdate)))

(defn get-content-name
  "Get Content's Name without extension"
  [content-file]
  (fs/name content-file))

(defn latest-contents
  [grouped-contents amount]
  (->> (vals grouped-contents)
       (flatten)
       (sort-by :pubdate)
       (reverse)
       (take amount)))

(defn get-contents
  "List of Contents"
  []
  (let [content-files (->> (path-join cwd "contents/")
                           (.toString)
                           (io/file)
                           (file-seq)
                           (filter #(.endsWith (.getName %) ".md")))
        parse (fn [path]
                (with-open [rdr (io/reader path)]
                  (let [content-name (get-content-name path)]
                    (canola.compile/parse-content content-name (line-seq rdr)))))
        parsed-contents (map parse content-files)]
    (->> parsed-contents
         (sort-by :pubdate)
         (reverse)
         (group-by organize-yearmonth))))

(defn copy-dir
  [from-dir to-dir]
  (when (fs/exists? to-dir)
    (if (fs/directory? to-dir)
      (fs/delete-dir to-dir)
      (throw (IllegalArgumentException. (str to-dir " isn't Directory (maybe file)")))))
  (fs/copy-dir from-dir to-dir))

(defn new-post [content-name]
  (let [pubdate (canola.compile/current-isodatetime)
        content-path (path-join cwd (str "contents/" content-name ".md"))]
    (spit (.toString content-path)
          (str "Title: " content-name "\n"
               "Pubdate: " pubdate "\n"
               "Tags: []\n"
               "---\n\n"
               "Content Here."))))

(defn build-list [yearmonth contents-chunk]
  (let [generated-list (canola.compile/render-contentlist yearmonth contents-chunk)
        archive-path (path-join cwd "dist/archives" yearmonth)]
    (.mkdirs (.toFile archive-path))
    (spit-path (path-join (.toString archive-path) "/index.html") generated-list)))

(defn build-post [content]
  (let [content-name (content :name)
        content-path (path-join cwd "contents/" (str content-name ".md"))
        post-path (path-join cwd "dist/posts" content-name)
        rendered (canola.compile/render-content content)
        metadata (content :metadata)]
    (.mkdirs (.toFile post-path))
    (spit-path (path-join (.toString post-path) "/index.html")
               (canola.compile/render-content content))
    (let [media-path (path-join cwd "contents/" (str content-name ".media"))
          media-dist-path (path-join (.toString post-path) "media/")]
      (copy-dir media-path media-dist-path))))

(defn build-archives [contents]
  (println "Rendering Archive page......")
  (let [yearmonths (keys contents)
        archive-path (path-join cwd "dist/archives/index.html")
        generated-archive (canola.compile/render-archive yearmonths)]
    (spit-path archive-path generated-archive)))

(defn build-index [contents]
  (println "Rendering Index page......")
  (let [top10 (latest-contents contents 10)
        index-path (path-join cwd "dist/index.html")
        generated-index (canola.compile/render-index top10)]
    (spit-path index-path generated-index)))

(defn build []
  (.mkdirs (.toFile (path-join cwd "dist/")))
  (let [theme-name (config :theme)
        theme-path (path-join cwd "themes/" theme-name)
        all-contents (get-contents)]
    (set-resource-path! (.toString (path-join (.toString theme-path) "templates/")))
    (doseq [yearmonth (keys all-contents)]
      (let [contents-chunk (all-contents yearmonth)]
        ; Render List
        (println (format "Rendering List %s" yearmonth))
        (build-list yearmonth contents-chunk)
        (doseq [content contents-chunk]
          ; Render Post
          (println (format "Rendering Post '%s'" (content :name)))
          (build-post content))))
    (doto all-contents
      ; Render Archive-list
      (build-archives)
      ; Render Index
      (build-index))
    ; Copy Site-wide Media
    (let [media-path (path-join cwd "media/")
          media-dist-path (path-join cwd "dist/media/")]
      (copy-dir media-path media-dist-path))
    ; Copy Theme's Assets
    (let [assets-path (path-join (.toString theme-path) "assets/")
          assets-dist-path (path-join cwd "dist/assets/")]
      (copy-dir assets-path assets-dist-path))))

(def cli-options
  [["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (when (options :help)
      (println summary)
      (System/exit 0))
    (case (first arguments)
      "new" (new-post (second arguments))
      "build" (build)
      (do ; default
        (println summary)
        (System/exit 1)))))
