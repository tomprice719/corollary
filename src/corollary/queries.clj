(ns corollary.queries
  (require [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]
           [clojure.set :refer [rename-keys union]]
           [environ.core :refer [env]]
           [clj-http.client :as client]))

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

(defn get-parents [id row-fn]
  (query db ["select parent.title, parent.date, parent.id from posts as parent
              where parent.id = (select child.parent_id from posts as child where child.id = ?)"
             (Integer. id)]
         {:row-fn row-fn}))

(defn get-children [id row-fn]
  (query db ["select title, date, id from posts where parent_id = ? order by date desc"
             (Integer. id)]
         {:row-fn row-fn}))

(defn get-title-map
  ([project-id] (reduce (fn [reduction {:keys [title id]}] (assoc reduction title id))
              {}
              (query db
                     ["select title, id from posts where project_id = ?" project-id])))
  ([project-id post-id] (reduce (fn [reduction {:keys [title id]}] (assoc reduction title id))
                     {}
                     (query db
                            ["select title, id from posts where project_id = ? and id != ?"
                             project-id
                             (Integer. post-id)]))))

(defn get-one-title [post-id]
  (first (query db
                ["select title from posts where id = ?"
                 (Integer. post-id)]
                {:row-fn :title})))

(defn pandoc [input]
  (:body (client/post (env :pandoc-url)
                      {:body input})))

(defn get-projects []
  (query db ["select posts.title, posts.id as root from projects join posts on posts.project_id = projects.id and posts.parent_id IS NULL"]))

(defn get-post [id]                                         ;;Get rid of having to call first
  (query db
         ["select child.title, child.author, child.date, child.raw_content, child.processed_content, child.hover_text, child.parent_id,
           projects.id as project, projects.password as password,
           parent.title as parent_title
           from posts as child join projects on child.project_id = projects.id
           left join posts as parent on parent.id = child.parent_id where child.id = ?"
          (Integer. id)]
         {:row-fn        #(update % :date date-string)
          :result-set-fn first}))

(defn post-href [post-id]
  (str "/selected?selected=" post-id))

(defn link-map [project-id]
  (query db
         ["select id, title, hover_text from posts where hover_text is not null and project_id = ?"
          (Integer. project-id)]
         {:row-fn        #(vector (-> % :id post-href)
                                  {:content (:hover_text %)
                                   :title   (:title %)})
          :result-set-fn #(apply hash-map (flatten %))}))

(defn get-posts [project-id page-num]
  (query db
         ["select title, author, date, id from posts where project_id = ? order by date desc limit ? offset ?"
          (Integer. project-id)
          posts-per-page
          (* page-num posts-per-page)]
         {:row-fn #(update % :date date-string)}))

(defn get-comments [post-id]
  (query db
         ["select author, date, processed_content from comments where post_id = ? order by date asc"
          (Integer. post-id)]
         {:row-fn #(update % :date date-string)}))
