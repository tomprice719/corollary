(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [corollary.utils :as utils :refer [db]]
           [ring.util.response :refer [redirect]]
           [cheshire.core :as cheshire]
           [clojure.pprint :refer [pprint]]
           [sqlingvo.db]
           [sqlingvo.core :as sql]
           [corollary.queries :refer [pandoc get-parents]]
           [clojure.pprint :refer [pprint]]
           [clojure.string :refer [trim blank?]]))

;(defn local-pandoc [input]
;  (:out (clojure.java.shell/sh "pandoc" "-f" "markdown-raw_html" "--mathjax" :in input))) ;; you MUST escape raw HTML

(def pg (sqlingvo.db/postgresql))

(defn next-post-id []
  (-> (jdbc/query db ["SELECT nextval('posts_id_seq')"]) first :nextval))

(defn add-edges-sql [edges]
  (sql/sql (sql/insert pg :edges [] (sql/values edges))))

(defn add-edges [edges]
  (if (not-empty edges)
    (jdbc/execute! db (add-edges-sql edges))))

(defn add-parents [parents id]
  (->> parents cheshire/parse-string
       (map (fn [{parent-id "id"}]
              {:parent_id parent-id
               :child_id (Integer. id)}))
       (add-edges)))

(defn add-children [children id]
  (->> children cheshire/parse-string
       (map (fn [{child-id "id"}]
              {:parent_id (Integer. id)
               :child_id child-id}))
       (add-edges)))

(defn delete-parents [child-id]
  (jdbc/delete! db :edges ["child_id = ?" (Integer. child-id)]))

(defn delete-children [parent-id]
  (jdbc/delete! db :edges ["parent_id = ?" (Integer. parent-id)]))

(defn create-post [{:keys [name title content hover-text parents children]}]
  (let [id (next-post-id)]
    (jdbc/insert! db :posts
                  {:id                id
                   :author            name
                   :title             (trim title)
                   :date              (utils/now)
                   :raw_content       content
                   :processed_content (pandoc content)
                   :hover_text        (if (blank? hover-text) nil hover-text)})
    (add-parents parents id)
    (add-children children id)
    (println "NEW POST " id)
    (redirect (str "/selected?selected=" id) :see-other)))

(defn update-post [{:keys [content hover-text parents children id title name]}]
  (jdbc/update! db :posts
                {:author            name
                 :title             title
                 :raw_content       content
                 :processed_content (pandoc content)
                 :hover_text        (if (blank? hover-text) nil hover-text)}
                ["id = ?" (Integer. id)])
  (delete-parents id)
  (delete-children id)
  (add-parents parents id)
  (add-children children id)
  (redirect (str "/selected?selected=" id) :see-other))

(defn delete-post [{:keys [id]}]
  (let [parents (get-parents id identity)]
    (delete-parents id)
    (delete-children id)
    (jdbc/delete! db :posts ["id = ?" (Integer. id)])
    (if (empty? parents)
      (redirect "/recent":see-other)
      (redirect (str "/selected?selected=" (:id (first parents))) :see-other))))

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

