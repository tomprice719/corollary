(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.utils :as utils]
           [clojure.java.jdbc :refer [query]]
           [environ.core :refer [env]]))

(def db (env :database-url))

(defn date-string [date]
  (let [time-diff (- (utils/now) date)]
    (str (quot time-diff 1000) " seconds ago")))

(defn next-post-id [] ;; Move this into queries module
  (-> (query db ["SELECT nextval('posts_id_seq')"]) first :nextval))

(defn post [id] ;;Get rid of having to call first
  (first
    (query db
           [(str "select title, date, processed_content from posts where id = '" id "'")]
           {:row-fn #(update % :date date-string)})))

(defn posts [] (query db
                      ["select title, author, date, id from posts"]
                      {:row-fn #(update % :date date-string)}))

(def posts-2 [{:title "newer first post title" :heading "hellooooooooooo"}
              {:title "post title 2" :heading "have a nice day today"}
              {:title "third post" :heading "aaaaaaaaaaaaaaaaaaa"}])

(defn recent-posts [name selected]
  (do
    (render-file "templates/recent_posts.html" {:posts (posts) :name name :selected selected :page "recent"})))

(defn selected-post [name selected]
  (do
    (render-file "templates/selected_post.html" {:name name :post (post selected) :selected selected :page "selected"})))

(defn compose-post []
  (render-file "templates/compose_post.html" {})) ;; Is .html extension appropriate for template files? Yes, it is.
