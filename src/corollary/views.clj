(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.utils :as utils]
           [clojure.java.jdbc :refer [query]]
           [environ.core :refer [env]]))

(def db (env :database-url))

(defn date-string [date]
  (let [time-diff (- (utils/now) date)]
    (str (quot time-diff 1000) " seconds ago")))

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

(defn recent-posts [{:keys [name selected]}]
  {:body
   (render-file
     "templates/recent_posts.html"
     {:posts (posts)
      :name name
      :selected selected
      :page "recent"})})

(defn selected-post [{:keys [name selected]}]
  {:body
   (render-file "templates/selected_post.html"
                {:name name :post (post selected) :selected selected :page "selected"})})

(defn compose-post [params]
  {:body (render-file "templates/compose_post.html" {})})

