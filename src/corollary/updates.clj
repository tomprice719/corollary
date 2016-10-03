(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [corollary.utils :as utils :refer [db password-status]]
           [ring.util.response :refer [redirect]]
           [cheshire.core :as cheshire]
           [clojure.pprint :refer [pprint]]
           [sqlingvo.db]
           [sqlingvo.core :as sql]
           [corollary.queries :refer :all]
           [clojure.pprint :refer [pprint]]
           [clojure.string :refer [trim blank?]]))

;(defn local-pandoc [input]
;  (:out (clojure.java.shell/sh "pandoc" "-f" "markdown-raw_html" "--mathjax" :in input))) ;; you MUST escape raw HTML

(defn next-post-id []
  (-> (jdbc/query db ["SELECT nextval('posts_id_seq')"]) first :nextval))

(defn create-post [{:keys [name title content hover-text parent-id] :as params}]
  (let [id (next-post-id)
        parent (get-post parent-id)]
    (assert (= (password-status parent params) :correct))
    (jdbc/insert! db :posts
                  {:id                id
                   :author            name
                   :title             (trim title)
                   :date              (utils/now)
                   :raw_content       content
                   :processed_content (pandoc content)
                   :hover_text        (if (blank? hover-text) nil hover-text)
                   :parent_id         (Integer. parent-id)
                   :project_id        (:project parent)})
    (println "NEW POST " id)
    (redirect (str "/selected?selected=" id) :see-other)))

(defn update-post [{:keys [content hover-text id title name parent-id] :as params}]
  (let [post (get-post id)]
    (assert (= (password-status post params) :correct))
    (if parent-id
      (assert (= (:project post) (:project (get-post parent-id))))))
  (jdbc/update! db :posts
                {:author            name
                 :title             title
                 :raw_content       content
                 :processed_content (pandoc content)
                 :hover_text        (if (blank? hover-text) nil hover-text)
                 :parent_id         (if parent-id (Integer. parent-id))
                 }
                ["id = ?" (Integer. id)])
  (redirect (str "/selected?selected=" id) :see-other))

(defn delete-post [{:keys [id]}]
  (jdbc/delete! db :posts ["id = ?" (Integer. id)])
  (redirect (str "/selected?selected=" (:id (first parents))) :see-other))

(defn create-comment [{:keys [name content post-id]}]
  (jdbc/insert! db :comments
                {:author name
                 :date (utils/now)
                 :raw_content content
                 :processed_content (pandoc content)
                 :post_id (Integer. post-id)})
  (println "NEW COMMENT " post-id)
  (redirect (str "/selected?selected=" post-id "#bottom-comment") :see-other))

(defn create-subscription [{:keys [id email descs]}]
  (jdbc/insert! db :subscriptions
                {:post_id     (Integer. id)
                 :email       email
                 :descendants (some? descs)
                 :date        (utils/now)})
  (redirect (str "/selected?subscribed=true&selected=" id) :see-other))

