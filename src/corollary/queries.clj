(ns corollary.queries
  (require [corollary.utils :refer [db] :as utils]
           [clojure.java.jdbc :refer [query]]))

(defn get-parents [id]
  (query db ["select title, id from posts join edges on posts.id = edges.parent_id where edges.child_id = ?"
             (Integer. id)]))

(defn get-children [id]
  (query db ["select title, id from posts join edges on posts.id = edges.child_id where edges.parent_id = ?"
             (Integer. id)]))

(defn get-post-titles []
  (query db
         ["select title from posts"]
         {:row-fn :title}))

(defn get-one-title [post-id]
  (first (query db
                ["select title from posts where id = ?"
                 (Integer. post-id)]
                {:row-fn :title})))
