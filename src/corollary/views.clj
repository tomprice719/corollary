(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.queries :refer :all]
           [cheshire.core :as cheshire]
           [corollary.tree :as tree]
           [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]))

(def posts-per-page 10)

(defn date-string [date]
  (let [time-diff (- (utils/now) date)]
    (str (quot time-diff 1000) " seconds ago")))

(defn get-post [id] ;;Get rid of having to call first
  (first
    (query db
           ["select title, date, processed_content from posts where id = ?"
            (Integer. id)]
           {:row-fn #(update % :date date-string)})))

(defn get-posts [page-num]
  (query db
         ["select title, author, date, id from posts order by date desc limit ? offset ?"
          posts-per-page
          (* page-num posts-per-page)]
         {:row-fn #(update % :date date-string)}))

(defn recent-posts [params]
  (let [page-num (if-let [pn-str (:page-num params)] (Integer. pn-str) 0)]
    {:body
     (render-file
       "templates/recent_posts.html"
       (merge params
              {:page-num page-num
               :posts (get-posts page-num)
               :page "recent"}))}))

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

(defn tree-page [{:keys [selected] :as params}]
  {:body (render-file "templates/tree.html"
                      (merge params
                             { :page "tree"
                               :nodes (tree/draw-data-list selected)}))})
