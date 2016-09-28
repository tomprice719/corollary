(ns corollary.routes
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [compojure.handler :refer [site]] ;; DEPRECATED
            [corollary.views :as views]
            [corollary.updates :as updates]
            [clojure.core :refer [rand-int]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [environ.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [corollary.ajax :as ajax]))

;;Is compojure even doing anything for you?

;;TODO: you want to get an alert when you enter the wrong password

(defn get-params [request]
  (merge (:params request)
         (:query-params request)
         (:cookies request)))

(defn expand-route [method [pathstring handler]]
  (let [request (gensym)
        params (gensym)]
    `(~method ~pathstring ~request
       (let [~params (get-params ~request)]
         (~handler ~params)))))

(defn expand-method-block [[method & routes]]
  (map #(expand-route method %) routes))

(defmacro easy-routes [name & method-blocks]
  `(defroutes ~name
     ~@(mapcat expand-method-block method-blocks)
     (route/resources "/")
     (route/not-found "Page not found")))

(easy-routes app
             (GET
               ("/" views/recent-posts)
               ("/recent" views/recent-posts)
               ("/top-level" views/top-level-posts)
               ("/selected" views/selected-post)
               ("/compose" views/compose-post)
               ("/edit" views/edit-post)
               ("/navigate" views/navigate-page))
             (POST
               ("/add-post" updates/create-post)
               ("/add-comment" updates/create-comment)
               ("/update-post" updates/update-post)
               ("/delete-post" updates/delete-post)
               ("/subscribe" updates/create-subscription)
               ("/preview-html" ajax/get-preview-html)))

;;TODO: get rid of this
(def mysite
  (site #'app))
