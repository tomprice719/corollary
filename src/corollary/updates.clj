(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [environ.core :refer [env]]
           [clojure.java.shell :refer [sh]]))

(def db (env :database-url))

(defn pandoc [input]
  (:out (sh "pandoc" "-f" "markdown-raw_html" :in input))) ;; you MUST escape raw HTML

(defn create-post [postid title heading]
  (jdbc/insert! db :posts
                {:did postid
                 :title title
                 :heading (pandoc heading)}))

