(ns canola.compile
  "Compile Source File"
  (:require
    [clojure.java.io :refer [reader]]
    [clojure.string :as cs]
    [canola.config]
    [clj-yaml.core :as yaml]
    [clj-time.core]
    [clj-time.format :as ctformat]
    [clj-time.coerce :refer [from-date]]
    [selmer.parser :refer [render-file]]
    [markdown.core :refer [md-to-html-string]]))

; (selmer.parser/set-resource-path! (.toString (canola.path/get-path "templates/")))

(defn- render-template [filename parameter]
  (render-file filename (merge {:config canola.config/config} parameter)))

(defn- make-formatter [fs]
  (let [seoul (clj-time.core/time-zone-for-id "Asia/Seoul")]
    (ctformat/with-zone (ctformat/formatter fs seoul) seoul)))

(defn current-isodatetime []
  (let [formater (make-formatter "yyyy-MM-dd'T'HH:mm:ssZZ")]
    (ctformat/unparse formater (clj-time.core/now))))

(defn format-datetime
  [datetime]
  (let [formater (make-formatter "yyyy-MM-dd (EEEE) a hh:mm")]
    (ctformat/unparse formater datetime)))

(defn format-date
  [datetime]
  (let [formater (make-formatter "yyyy-MM-dd")]
    (ctformat/unparse formater datetime)))

(defn process-metadata
  [metadata-r]
  (merge metadata-r {:Pubdate (->> (metadata-r :Pubdate)
                                   (from-date)
                                   (format-datetime))
                     :Pubdate-notime (->> metadata-r :Pubdate
                                          (from-date)
                                          (format-date))
                     :Summary (if (contains? metadata-r :Summary)
                                (md-to-html-string (metadata-r :Summary))
                                nil)}))

(defn parse-content
  "Get MetaData from Content File"
  [content-name fileseq]
  (let [s-content (partition-by #(= % "---") fileseq)
        head (cs/replace (cs/join "\n" (first s-content)) "#" "＃")
        body-lines (drop-while #(= (cs/trim %) "") (flatten (drop 2 s-content)))
        body (cs/join "\n" body-lines)
        metadata (yaml/parse-string head)
        metadata2 (process-metadata metadata)]
    {:name content-name
     :metadata metadata
     :metadata2 metadata2
     :content (md-to-html-string body)
     :pubdate (from-date (metadata :Pubdate))}))

(defn render-content
  "Render Content to HTML file"
  [parsed-content]
  (render-template "article.html" parsed-content))

(defn render-contentlist
  "Render List of Contents"
  [yearmonth contents-list]
  (render-template "list.html" {:contents contents-list
                                :yearmonth yearmonth}))

(defn render-archive
  "Render Archive of Posts"
  [yearmonths]
  (render-template "archive.html" {:yearmonths yearmonths}))

(defn render-index
  "Render Index of Site"
  [contents-list]
  (render-template "index.html" {:contents contents-list}))

