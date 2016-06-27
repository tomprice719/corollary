(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [environ.core :refer [env]]
           [clojure.java.shell :refer [sh]]
           [clj-http.client :as client]))

(def db (env :database-url))

(defn pandoc [input]
  (:out (sh "pandoc" "-f" "markdown-raw_html" "--mathjax" :in input))) ;; you MUST escape raw HTML

(defn remote-pandoc [input]
  (:body (client/post (env :pandoc-url)
                      {:body input})))

(defn create-post [postid title heading]
  (jdbc/insert! db :posts
                {:did postid
                 :title title
                 :heading (pandoc heading)}))

