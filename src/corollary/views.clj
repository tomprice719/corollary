(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.queries :refer :all]
           [cheshire.core :as cheshire]
           [corollary.tree :as tree]
           [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]))

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

(defn get-projects []
  (query db ["select name, posts.id as root from projects join posts on posts.project_id = projects.id and posts.root=true"]))

(defn get-post [id] ;;Get rid of having to call first
  (first
    (query db
           ["select title, author, posts.date, raw_content, processed_content, hover_text, projects.id as project, projects.password as password
            from posts join projects on posts.project_id = projects.id where posts.id = ?"
            (Integer. id)]
           {:row-fn #(update % :date date-string)})))

(defn post-href [post-id]
  (str "/selected?selected=" post-id))

(defn link-map []
  (query db
         ["select id, title, hover_text from posts where hover_text is not null"]
         {:row-fn        #(vector (-> % :id post-href)
                                  {:content (:hover_text %)
                                   :title   (:title %)})
          :result-set-fn #(apply hash-map (flatten %))}))

(defn get-post-project [post-id]
  (query db
         ["select project_id from posts where id = ?"
          (Integer. post-id)]
         {:row-fn        :project_id
          :result-set-fn first}))

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

(defn recent-posts [{:keys [page-num selected] :as params}]
  (let [page-num-int (if page-num (Integer. page-num) 0)
        project-id (get-post-project selected)]
    (render-file
      "templates/feed.html"
      (merge params
             {:page-num page-num
              :posts (get-posts project-id page-num-int)
              :page "recent"
              :path "/recent"}))))

(defn project-password-key [project]
  (assert (some? project))
  (str project "-password"))

(defn selected-post-after-password [params post selected]
  (render-file "templates/selected_post.html"
               (merge params
                      {:post       post
                       :page       "selected"
                       :parents    (not-empty (get-parents selected identity))
                       :children   (not-empty (get-children selected identity))
                       :comments   (not-empty (get-comments selected))
                       :link-map   (cheshire/generate-string (link-map))
                       })))

(defn selected-post-request-password [project retry]
  (render-file "templates/request_password.html"
               {:password-key (project-password-key project)
                :retry        retry}))

(defn selected-post [{:keys [selected] :as params}]
  (let [post (get-post selected)
        project (:project post)
        submitted-password (get-in params [(project-password-key project) :value])
        true-password (:password post)]
    (if (= submitted-password true-password)
      (selected-post-after-password params post selected)
      (selected-post-request-password project (some? submitted-password)))))
(defn compose-post [{:keys [selected]}]
  (let [post (get-post selected)]
    (render-file "templates/compose_post.html"
                 {:form-action "/add-post"
                  :parents
                               (cheshire/generate-string [(select-keys post [:title])])
                  :title-map
                               (-> post :project get-title-map cheshire/generate-string)})))

(defn edit-post [{:keys [selected]}]
  (let [{:keys [title author raw_content hover_text]} (get-post selected)
        parents (get-parents selected identity)
        children (get-children selected identity)
        project (get-post-project selected)]
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
                  (cheshire/generate-string (get-title-map project selected))})))

(defn navigate-page [{:keys [selected] :as params}]
  (render-file "templates/tree.html"
               (merge params
                      { :page "tree"
                        :nodes (tree/draw-data-list selected)})))

(defn home-page [params]
  (render-file "templates/home.html"
               {:projects (get-projects)}))
