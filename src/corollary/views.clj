(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [clojure.java.jdbc :refer [query]]
           [environ.core :refer [env]]))

(def db (env :database-url))

(defn post [id]
  (first
    (query db
          [(str "select title, heading from posts where did = '" id "'")])))

(def posts (query db
                  ["select did, title, heading from posts"]))

(def posts-2 [{:title "newer first post title" :heading "hellooooooooooo"}
              {:title "post title 2" :heading "have a nice day today"}
              {:title "third post" :heading "aaaaaaaaaaaaaaaaaaa"}])

(defn recent-posts-page [name selected-id]
  (do
    (println "Name: " name)
    (render-file "templates/recent_posts.html" {:posts posts :name name :selected-id selected-id})))

(defn selected-post-page [name post-id]
  (do
    (println "Name: " name)
    (render-file "templates/selected_post.html" {:name name :post (post post-id)})))
