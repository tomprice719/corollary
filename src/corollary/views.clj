(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.queries :refer :all]
           [cheshire.core :as cheshire]
           [corollary.tree :as tree]
           [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]))

(def posts-per-page 10)
(def edge-colour "#000000")

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
           ["select title, author, date, raw_content, processed_content, hover_text from posts where id = ?"
            (Integer. id)]
           {:row-fn #(update % :date date-string)})))

(defn post-href [post-id]
  (str "/selected?selected=" post-id))

(defn link-map []
  (query db
         ["select id, hover_text from posts where hover_text is not null"]
         {:row-fn        #(vector (-> % :id post-href) (:hover_text %))
          :result-set-fn #(apply hash-map (flatten %))}))

(defn get-posts [page-num]
  (query db
         ["select title, author, date, id from posts order by date desc limit ? offset ?"
          posts-per-page
          (* page-num posts-per-page)]
         {:row-fn #(update % :date date-string)}))

(defn get-top-level [page-num]
  (query db
         ["select title, author, date, id from posts where id not in (select child_id from edges group by child_id) order by date desc limit ? offset ?"
          posts-per-page
          (* page-num posts-per-page)]
         {:row-fn #(update % :date date-string)}))

(defn get-comments [post-id]
  (query db
         ["select author, date, processed_content from comments where post_id = ? order by date asc"
          (Integer. post-id)]
         {:row-fn #(update % :date date-string)}))

(defn recent-posts [params]
  (let [page-num (if-let [pn-str (:page-num params)] (Integer. pn-str) 0)]
    (render-file
      "templates/feed.html"
      (merge params
             {:page-num page-num
              :posts (get-posts page-num)
              :page "recent"
              :path "/recent"}))))

(defn top-level-posts [params]
  (let [page-num (if-let [pn-str (:page-num params)] (Integer. pn-str) 0)]
    (render-file
      "templates/feed.html"
      (merge params
             {:page-num page-num
              :posts (get-top-level page-num)
              :page "top-level"
              :path "/top-level"}))))

(defn selected-post [{:keys [selected subscribed] :as params}]
  (render-file "templates/selected_post.html"
               (merge params
                      {:post     (get-post selected)
                       :page     "selected"
                       :parents  (not-empty (get-parents selected identity))
                       :children (not-empty (get-children selected identity))
                       :comments (not-empty (get-comments selected))
                       :link-map (cheshire/generate-string (link-map))
                       :subscribed subscribed})))

(defn compose-post [{:keys [parent-title]}]
  (render-file "templates/compose_post.html"
               {:form-action "/add-post"
                :parents (if parent-title
                           (cheshire/generate-string [{:title parent-title}]))
                :title-map
                (cheshire/generate-string (get-title-map))}))

(defn edit-post [{:keys [selected]}]
  (let [{:keys [title author raw_content hover_text]} (get-post selected)
        parents (get-parents selected identity)
        children (get-children selected identity)]
    (render-file "templates/compose_post.html"
                 {:id selected
                  :title title
                  :author author
                  :content raw_content
                  :hover_text hover_text
                  :edit true
                  :form-action "/update-post"
                  :parents (cheshire/generate-string parents)
                  :children (cheshire/generate-string children)
                  :title-map
                  (cheshire/generate-string (get-title-map selected))})))

(defn navigate-page [{:keys [selected] :as params}]
  (render-file "templates/tree.html"
               (merge params
                      { :page "tree"
                        :nodes (tree/draw-data-list selected)
                        :edge-colour edge-colour})))

(defn request-password [wrong-password]
  (render-file "templates/password.html" {:wrong-password wrong-password}))
