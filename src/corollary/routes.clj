(ns corollary.routes
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [corollary.views :as views]
            [corollary.updates :as updates]
            [clojure.core :refer [rand-int]]
            [ring.util.response :refer [redirect]]))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello from Heroku"})

;;Is it really necessary to "mirror" the session data? probably not.
;;ACTUALLY I think you do. Theory: whenever you set a key in the session, you need to provide correct values for ALL other keys.
;;"the rest of the route is encased in an implicit do block, just like normal functions"
(defroutes app
  (GET "/" {{name :name selected-id :selected-id} :session}
       (views/recent-posts name selected-id))
  (GET "/recent" {{name :name selected-id :selected-id} :session}
       (views/recent-posts name selected-id))
  (GET "/selected/:postid" {{name :name} :session
                            {postid :postid} :params}
       {:session {:selected-id postid :name name}
        :body (views/selected-post name postid)})
  (GET "/user/:name" [name]
       {:session {:name name}
        :body (views/selected-post name 0)})
  (GET "/compose" []
       (views/compose-post))
  (POST "/addpost" [title heading]
        (let [postid (rand-int 100000)]
          (updates/create-post postid title heading)
          (redirect (str "/selected/" postid) :see-other)))
  (route/resources "/")
  (route/not-found "Page not found"))
