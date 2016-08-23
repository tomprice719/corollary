(ns corollary.queries
  (require [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]
           [clojure.set :refer [rename-keys union]]
           [environ.core :refer [env]]
           [clj-http.client :as client]))

(defn get-parents [id row-fn]
  (query db ["select posts.title, posts.date, posts.id, edges.type as edge_type from posts join edges on posts.id = edges.parent_id where edges.child_id = ? order by posts.date desc"
             (Integer. id)]
         {:row-fn row-fn}))

(defn get-children [id row-fn]
  (query db ["select posts.title, posts.date, posts.id, edges.type as edge_type from posts join edges on posts.id = edges.child_id where edges.parent_id = ? order by posts.date desc"
             (Integer. id)]
         {:row-fn row-fn}))

(defn get-title-map
  ([] (reduce (fn [reduction {:keys [title id]}] (assoc reduction title id))
              {}
              (query db
                     ["select title, id from posts"])))
  ([post-id] (reduce (fn [reduction {:keys [title id]}] (assoc reduction title id))
                     {}
                     (query db
                            ["select title, id from posts where id != ?" (Integer. post-id)]))))

(defn get-one-title [post-id]
  (first (query db
                ["select title from posts where id = ?"
                 (Integer. post-id)]
                {:row-fn :title})))

(defn pandoc [input]
  (:body (client/post (env :pandoc-url)
                      {:body input})))
