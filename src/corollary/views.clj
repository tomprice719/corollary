(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.queries :refer :all]
           [cheshire.core :as cheshire]
           [corollary.tree :as tree]
           [corollary.utils :refer [db project-password-key] :as utils]
           [clojure.java.jdbc :refer [query]]))

(defn recent-posts [{:keys [page-num post] :as params}]
  (let [page-num-int (if page-num (Integer. page-num) 0)
        project-id (:project post)]
    (render-file
      "templates/feed.html"
      (merge params
             {:page-num page-num
              :posts (get-posts project-id page-num-int)
              :page "recent"
              :path "/recent"}))))

(defn selected-post [{:keys [post selected] :as params}]
  (render-file "templates/selected_post.html"
               (merge params
                      {:post     post
                       :page     "selected"
                       :children (not-empty (get-children selected identity))
                       :comments (not-empty (get-comments selected))
                       :link-map (cheshire/generate-string (link-map (:project post)))
                       })))

(defn compose-post [{:keys [selected]}]
  (let [parent (get-post selected)]
    (render-file "templates/compose_post.html"
                 {:parent-title (:title parent)
                  :parent-id    selected
                  :title-map
                                (-> parent :project get-title-map cheshire/generate-string)})))

(defn edit-post [{:keys [selected post]}]
  (let [{:keys [title author raw_content hover_text project]} post
        post (get-post selected)]
    (render-file "templates/compose_post.html"
                 {:id           selected
                  :title        title
                  :author       author
                  :content      raw_content
                  :hover_text   hover_text
                  :edit         true
                  :parent-title (:parent_title post)
                  :parent-id    (:parent_id post)
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

(defn request-password [project retry]
  (render-file "templates/request_password.html"
               {:password-key (project-password-key project)
                :retry        retry}))
