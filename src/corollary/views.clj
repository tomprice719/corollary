(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.utils :as utils]
           [clojure.java.jdbc :refer [query]]
           [environ.core :refer [env]]))

(def db (env :database-url))

(defn date-string [date]
  (let [time-diff (- (utils/now) date)]
    (str (quot time-diff 1000) " seconds ago")))

(defn get-post [id] ;;Get rid of having to call first
  (first
    (query db
           [(str "select title, date, processed_content from posts where id = '" id "'")]
           {:row-fn #(update % :date date-string)})))

(defn get-posts [] (query db
                      ["select title, author, date, id from posts"]
                      {:row-fn #(update % :date date-string)}))

(defn recent-posts [params]
  {:body
   (render-file
     "templates/recent_posts.html"
     (merge params
            {:posts (get-posts)
             :page "recent"}))})

(defn selected-post [{:keys [selected] :as params}]
  {:body
   (render-file "templates/selected_post.html"
                (merge params
                       {:post (get-post selected)
                        :page "selected"}))})

(defn compose-post [params]
  {:body (render-file "templates/compose_post.html" {})})

