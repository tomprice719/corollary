(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.queries :refer :all]
           [cheshire.core :as cheshire]
           [corollary.tree :as tree]
           [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]
           [corollary.edges :refer [get-edge-colour]]))

(def posts-per-page 10)

(defn date-string [date]
  (let [seconds-diff (quot (- (utils/now) date) 1000)
        minute 60
        two-minutes (* minute 2)
        hour (* minute 60)
        two-hours (* hour 2)
        day (* hour 24)
        two-days (* day 2)
        week (* day 7)
        two-weeks (* week 2)]
    (condp > seconds-diff
      minute (str seconds-diff " seconds ago")
      two-minutes "1 minute ago"
      hour (str (quot seconds-diff 60) " minutes ago")
      two-hours "1 hour ago"
      day (str (quot seconds-diff 3600) " hours ago")
      two-days "1 day ago"
      week (str (quot seconds-diff 86400) " days ago")
      two-weeks "1 week ago"
      (str (quot seconds-diff 604800) " weeks ago"))))

(defn get-post [id] ;;Get rid of having to call first
  (first
    (query db
           ["select title, date, raw_content, processed_content from posts where id = ?"
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

(defn add-edge-colour [{:keys [edge_type] :as post}]
  (assoc post :edge-colour (get-edge-colour edge_type)))

(defn selected-post [{:keys [selected] :as params}]
  {:body
   (render-file "templates/selected_post.html"
                (merge params
                       {:post (get-post selected)
                        :page "selected"
                        :parents (get-parents selected add-edge-colour)
                        :children (get-children selected add-edge-colour)}))})

(defn compose-post []
  {:body (render-file "templates/compose_post.html"
                      {:form-action "/add-post"
                       :title-map
                       (cheshire/generate-string (get-title-map))
                       :link-types
                       (cheshire/generate-string (get-edge-types))})})

(defn edit-post [{:keys [selected]}]
  (let [{:keys [title raw_content]} (get-post selected)
        parents (get-parents selected identity)
        children (get-children selected identity)]
    {:body (render-file "templates/compose_post.html"
                        {:id selected
                         :title title
                         :content raw_content
                         :edit true
                         :form-action "/update-post"
                         :parents (cheshire/generate-string parents)
                         :children (cheshire/generate-string children)
                         :title-map
                         (cheshire/generate-string (get-title-map))
                         :link-types
                         (cheshire/generate-string (get-edge-types))})}))

(defn tree-page [{:keys [selected] :as params}]
  {:body (render-file "templates/tree.html"
                      (merge params
                             { :page "tree"
                               :nodes (tree/draw-data-list selected)}))})
