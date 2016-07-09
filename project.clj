(defproject clojure-getting-started "1.0.0-SNAPSHOT"
  :description "Demo Clojure web app"
  :url "http://clojure-getting-started.herokuapp.com"
  :main ^:skip-aot corollary.core
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [environ "1.0.0"]
                 [selmer "1.0.4"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [postgresql "9.3-1102.jdbc41"]
                 [migratus "0.8.25"]
                 [clj-http "2.2.0"]
                 [cheshire "5.6.3"]
                 [sqlingvo "0.8.14"]
                 [org.clojure/data.priority-map "0.0.7"]]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "corollary-standalone.jar")
