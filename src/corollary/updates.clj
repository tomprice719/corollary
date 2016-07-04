(ns corollary.updates
  (require [clojure.java.jdbc :as jdbc]
           [corollary.utils :as utils :refer [db]]
           [clj-http.client :as client]
           [ring.util.response :refer [redirect]]
           [cheshire.core :as cheshire]
           [clojure.pprint :refer [pprint]]
           [sqlingvo.db]
           [sqlingvo.core :as sql]
           [environ.core :refer [env]]))

;(defn local-pandoc [input]
;  (:out (clojure.java.shell/sh "pandoc" "-f" "markdown-raw_html" "--mathjax" :in input))) ;; you MUST escape raw HTML

(def pg (sqlingvo.db/postgresql))

(defn pandoc [input]
  (:body (client/post (env :pandoc-url)
                      {:body input})))

(defn next-post-id []
  (-> (jdbc/query db ["SELECT nextval('posts_id_seq')"]) first :nextval))

(defn add-parents-sql [child_id parent_titles]
  (sql/sql (sql/insert pg :edges [:child_id :parent_id]
                       (sql/select pg [child_id :id]
                                   (sql/from :posts)
                                   (sql/where `(in title ~(seq parent_titles)))))))

(defn add-parents [id parents]
  (let [sql-string (add-parents-sql id parents)]
    (if (not-empty parents)
      (jdbc/execute! db sql-string))))

(defn add-children-sql [parent_id child_titles]
  (sql/sql (sql/insert pg :edges [:parent_id :child_id]
                       (sql/select pg [parent_id :id]
                                   (sql/from :posts)
                                   (sql/where `(in title ~(seq child_titles)))))))

(defn add-children [id children]
  (if (not-empty children)
    (let [sql-string (add-children-sql id children)]
      (jdbc/execute! db sql-string))))

(defn create-post [{:keys [name title content parents children]}]
  (let [id (next-post-id)]
    (jdbc/insert! db :posts
                  {:id id
                   :author name
                   :title title
                   :date (utils/now)
                   :raw_content content
                   :processed_content (pandoc content)})
    (->> parents cheshire/parse-string (map #(get % "title")) (add-parents id))
    (->> children cheshire/parse-string (map #(get % "title")) (add-children id))
    (redirect (str "/selected?selected=" id) :see-other)))

