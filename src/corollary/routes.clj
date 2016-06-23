(ns corollary.routes
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [corollary.views :as views]))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello from Heroku"})

(defroutes app
  (GET "/" {{name :name} :session}
       {:session {:name name}
        :body (views/recent-posts-page name)})
  (GET "/recent" {{name :name} :session}
       {:session {:name name}
        :body (views/recent-posts-page name)})
  (GET "/selected" {{name :name} :session}
       {:session {:name name}
        :body (views/selected-post-page name)})
  (GET "/user/:name" [name]
       {:session {:name name}
        :body (views/selected-post-page name)})
  (route/resources "/")
  (route/not-found "Page not found"))
