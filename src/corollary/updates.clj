(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [environ.core :refer [env]]))

(def db (env :database-url))

(defn create-post [postid title heading]
  (println postid title heading)
  (jdbc/insert! db :posts
                {:did postid
                 :title title
                 :heading heading}))

