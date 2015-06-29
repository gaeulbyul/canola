(defproject canola "0.0.1-DEVELOPING"
  :description "Canola - Static Site Generator"
  :url "https://github.com/zn/canola"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [circleci/clj-yaml "0.5.3"]
                 [clj-time "0.9.0"]
                 [markdown-clj "0.9.67"]
                 [me.raynes/fs "1.4.6"]
                 [selmer "0.8.2"]]
  :main ^:skip-aot canola.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
