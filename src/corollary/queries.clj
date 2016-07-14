(ns corollary.queries
  (require [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]
           [clojure.set :refer [rename-keys]]))

(defn get-parents [id row-fn]
  (query db ["select posts.title, posts.date, posts.id, edges.type as edge_type from posts join edges on posts.id = edges.parent_id where edges.child_id = ?"
             (Integer. id)]
         {:row-fn (comp row-fn #(rename-keys % {:edge_type :edge-type}))}))

(defn get-children [id row-fn]
  (query db ["select posts.title, posts.date, posts.id, edges.type as edge_type from posts join edges on posts.id = edges.child_id where edges.parent_id = ?"
             (Integer. id)]
         {:row-fn (comp row-fn #(rename-keys % {:edge_type :edge-type}))}))

(defn get-post-titles []
  (query db
         ["select title from posts"]
         {:row-fn :title}))

(defn get-one-title [post-id]
  (first (query db
                ["select title from posts where id = ?"
                 (Integer. post-id)]
                {:row-fn :title})))
