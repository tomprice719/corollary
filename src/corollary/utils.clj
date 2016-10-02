(ns corollary.utils
  (require [environ.core :refer [env]]))

(def db (env :database-url))

(defn now []
  (.getTime (new java.util.Date)))

(defn thrush [x] ;;currently unused
  #(% x))

(defn project-password-key [project]
  (assert (some? project))
  (str project "-password"))

(defn password-status [post params]
  (let [project (:project post)
        submitted-password (get-in params [(project-password-key project) :value])
        true-password (:password post)]
    (cond
      (= submitted-password true-password) :correct
      (some? submitted-password) :incorrect
      :else :empty)))
