(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [environ.core :refer [env]]
           [clj-http.client :as client]))

(def db (env :database-url))

;(defn local-pandoc [input]
;  (:out (clojure.java.shell/sh "pandoc" "-f" "markdown-raw_html" "--mathjax" :in input))) ;; you MUST escape raw HTML

(defn pandoc [input]
  (:body (client/post (env :pandoc-url)
                      {:body input})))

(defn create-post [postid title heading]
  (jdbc/insert! db :posts
                {:did postid
                 :title title
                 :heading (pandoc heading)}))

