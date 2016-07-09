(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]
           [cheshire.core :as cheshire]
           [corollary.tree :as tree]))

(defn date-string [date]
  (let [time-diff (- (utils/now) date)]
    (str (quot time-diff 1000) " seconds ago")))

(defn get-parents [id]
  (query db ["select title, id from posts join edges on posts.id = edges.parent_id where edges.child_id = ?"
             (Integer. id)]))

(defn get-children [id]
  (query db ["select title, id from posts join edges on posts.id = edges.child_id where edges.parent_id = ?"
             (Integer. id)]))

(defn get-post [id] ;;Get rid of having to call first
  (first
    (query db
           ["select title, date, processed_content from posts where id = ?"
            (Integer. id)]
           {:row-fn #(update % :date date-string)})))

(defn get-posts []
  (query db
         ["select title, author, date, id from posts"]
         {:row-fn #(update % :date date-string)}))

(defn get-post-titles []
  (query db
         ["select title from posts"]
         {:row-fn :title}))

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
                        :page "selected"
                        :parents (get-parents selected)
                        :children (get-children selected)}))})

(defn compose-post [params]
  {:body (render-file "templates/compose_post.html"
                      {:post-titles
                       (cheshire/generate-string (get-post-titles))})})

(defn tree-page [{:keys [selected]}]
  {:body (render-file "templates/tree.html"
                      {:nodes (tree/draw-data-list selected)})})
