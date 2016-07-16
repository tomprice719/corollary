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

(defn get-params [request]
  (merge (:params request)
         (:session request)
         (:query-params request)
         (select-keys request [:uri])))

(defn expand-route [method [pathstring & funcs]]
  (let [request (gensym)
        params (gensym)]
    `(~method ~pathstring ~request
              (let [~params (get-params ~request)]
                (merge-with merge
                            (select-keys ~request [:session])
                            ~@(map #(list % params)
                                   funcs))))))

(defn expand-method-block [[method & routes]]
  (map #(expand-route method %) routes))

(defmacro easy-routes [name & method-blocks]
  `(defroutes ~name
     ~@(mapcat expand-method-block method-blocks)
     (route/resources "/")
     (route/not-found "Page not found")))

(defn set-name [params]
  {:session (select-keys params [:name])})

(easy-routes app
             (GET
               ("/" views/recent-posts)
               ("/recent" views/recent-posts)
               ("/selected" views/selected-post)
               ("/user/:name" set-name views/recent-posts)
               ("/compose" views/compose-post)
               ("/edit" views/edit-post)
               ("/tree" views/tree-page))
             (POST
               ("/add-post" updates/create-post)
               ("/update-post" updates/update-post)
               ("/delete-post" updates/delete-post)
               ("/preview-html" ajax/get-preview-html)))

(def mysite
  (site #'app
        {:session {:store (cookie-store {:key (.getBytes (env :cookie-store-key))})}}))
