(ns canola.config
  "Canola's config"
  (:require
    [clj-yaml.core :as yaml]
    [clojure.java.io :refer [reader]])
  (:import
    [java.nio.file Paths]))

(def cwd (System/getProperty "user.dir"))

(def config
  (let [config-yml (->> [(str "" cwd) "config.yml"]
                        (into-array String)
                        (Paths/get "")
                        (.toString)
                        (slurp))]
    (yaml/parse-string config-yml)))
