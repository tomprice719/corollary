(ns corollary.views
  (require [selmer.parser :refer [render-file]]
           [corollary.queries :refer :all]
           [cheshire.core :as cheshire]
           [corollary.tree :as tree]
           [corollary.utils :refer [db] :as utils]
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
                       :parents  (not-empty (get-parents selected identity))
                       :children (not-empty (get-children selected identity))
                       :comments (not-empty (get-comments selected))
                       :link-map (cheshire/generate-string (link-map))
                       })))

(defn compose-post [{:keys [selected]}]
  (let [post (get-post selected)]
    (render-file "templates/compose_post.html"
                 {:form-action "/add-post"
                  :parents
                               (cheshire/generate-string [(select-keys post [:title])])
                  :title-map
                               (-> post :project get-title-map cheshire/generate-string)})))

(defn edit-post [{:keys [selected post]}]
  (let [{:keys [title author raw_content hover_text project]} post
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
                  (cheshire/generate-string (get-title-map project selected))})))

(defn navigate-page [{:keys [selected] :as params}]
  (render-file "templates/tree.html"
               (merge params
                      { :page "tree"
                        :nodes (tree/draw-data-list selected)})))

(defn home-page [params]
  (render-file "templates/home.html"
               {:projects (get-projects)}))
