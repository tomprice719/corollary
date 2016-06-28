(ns corollary.routes
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [compojure.handler :refer [site]] ;; DEPRECATED
            [corollary.views :as views]
            [corollary.updates :as updates]
            [clojure.core :refer [rand-int]]
            [ring.util.response :refer [redirect]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [environ.core :refer [env]]
            [clojure.pprint :refer [pprint]]))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello from Heroku"})

;;Is it really necessary to "mirror" the session data? probably not.
;;ACTUALLY I think you do. Theory: whenever you set a key in the session, you need to provide correct values for ALL other keys.
;;"the rest of the route is encased in an implicit do block, just like normal functions"
(defroutes app
  (GET "/" {{name :name} :session
                  {selected "selected"} :query-params}
       (views/recent-posts name selected))
  (GET "/recent" {{name :name} :session
                  {selected "selected"} :query-params}
       (views/recent-posts name selected))
  (GET "/selected" {{name :name} :session
                            {selected "selected"} :query-params}
       (views/selected-post name selected))
  (GET "/user/:name" [name]
       {:session {:name name}
        :body (views/selected-post name 0)})
  (GET "/compose" []
       (views/compose-post))
  (GET "/request" request (str request))
  (POST "/addpost" {session :session
                    params :params}
        (let [postid (views/next-post-id)]
          (updates/create-post postid (merge session params))
          (redirect (str "/selected?selected=" postid) :see-other)))
  (route/resources "/")
  (route/not-found "Page not found"))

(def mysite
  (site #'app
        {:session {:store (cookie-store {:key (.getBytes (env :cookie-store-key))})}}))
