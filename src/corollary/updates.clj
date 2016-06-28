(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [corollary.utils :as utils]
           [environ.core :refer [env]]
           [clj-http.client :as client]))

(def db (env :database-url))

;(defn local-pandoc [input]
;  (:out (clojure.java.shell/sh "pandoc" "-f" "markdown-raw_html" "--mathjax" :in input))) ;; you MUST escape raw HTML

;date, author, raw content, processed content, id

(defn pandoc [input]
  (:body (client/post (env :pandoc-url)
                      {:body input})))

(defn create-post [id {:keys [name title content]}]
  (jdbc/insert! db :posts
                {:id id
                 :author name
                 :title title
                 :date (utils/now)
                 :raw_content content
                 :processed_content (pandoc content)}))
