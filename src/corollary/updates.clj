(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [corollary.utils :as utils :refer [db]]
           [ring.util.response :refer [redirect]]
           [cheshire.core :as cheshire]
           [clojure.pprint :refer [pprint]]
           [sqlingvo.db]
           [sqlingvo.core :as sql]
           [corollary.queries :refer [pandoc]]))

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

(defn create-post [{:keys [name title content parents children]}]
  (let [id (next-post-id)]
    (jdbc/insert! db :posts
                  {:id id
                   :author name
                   :title title
                   :date (utils/now)
                   :raw_content content
                   :processed_content (pandoc content)})
    (->> parents cheshire/parse-string
         (map (fn [{edge-type "linkType" parent-id "id"}]
                {:type edge-type
                 :parent_id parent-id
                 :child_id id}))
         (add-edges))
    (->> children cheshire/parse-string
         (map (fn [{edge-type "linkType" child-id "id"}]
                {:type edge-type
                 :parent_id id
                 :child_id child-id}))
         (add-edges))
    (redirect (str "/selected?selected=" id) :see-other)))

