(ns corollary.sandbox
  (require [corollary.utils :refer [db]]
           [clojure.java.jdbc :as jdbc]
           [sqlingvo.db :as db]
           [sqlingvo.core :as sql]))

(def pg (db/postgresql))

(defn add-children-sql [parent_id child_titles]
  (sql/sql (sql/insert pg :edges [:parent_id :child_id]
                       (sql/select pg [parent_id :id]
                                   (sql/from :posts)
                                   (sql/where `(in title ~(seq child_titles)))))))

(defn add-parents-sql [child_id parent_titles]
  (sql/sql (sql/insert pg :edges [:child_id :parent_id]
                       (sql/select pg [child_id :id]
                                   (sql/from :posts)
                                   (sql/where `(in title ~(seq parent_titles)))))))

(defn testquery2 [] (jdbc/query db ["select title from posts where id = ?" 7]))

;;(add-children-sql 7 ["test2" "test3" "test4"])
;;(add-parents-sql 5 ["parent1" "parent2" "another parent"])

;;(jdbc/execute! db (add-children-sql 7 ["test2" "test3" "test4"]))

