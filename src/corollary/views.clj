(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [clojure.java.jdbc :refer [query]]
           [environ.core :refer [env]]))

(def db (env :database-url))

(def posts (query db
                  ["select title, heading from posts"]))

(def posts-2 [{:title "newer first post title" :heading "hellooooooooooo"}
              {:title "post title 2" :heading "have a nice day today"}
              {:title "third post" :heading "aaaaaaaaaaaaaaaaaaa"}])

(defn recent-posts-page [name]
  (do
    (println "Name: " name)
    (render-file "templates/recent_posts.html" {:posts posts :name name})))

(defn selected-post-page [name]
  (do
    (println "Name: " name)
    (render-file "templates/selected_post.html" {:name name})))
