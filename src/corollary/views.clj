(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [clojure.java.jdbc :refer [query]]
           [environ.core :refer [env]]))

(def db (env :database-url))

(defn post [id]
  (first
    (query db
           [(str "select title, heading from posts where did = '" id "'")])))

(defn posts [] (query db
                      ["select did, title, heading from posts"]))

(def posts-2 [{:title "newer first post title" :heading "hellooooooooooo"}
              {:title "post title 2" :heading "have a nice day today"}
              {:title "third post" :heading "aaaaaaaaaaaaaaaaaaa"}])

(defn recent-posts [name selected-id]
  (do
    (render-file "templates/recent_posts.html" {:posts (posts) :name name :selected-id selected-id :page "recent"})))

(defn selected-post [name post-id]
  (do
    (render-file "templates/selected_post.html" {:name name :post (post post-id) :page "selected"})))

(defn compose-post []
  (render-file "templates/compose_post.html" {})) ;; Is .html extension appropriate for template files? Yes, it is.
