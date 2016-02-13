(ns net.thegeez.snippetlist.users
  (:require [clojure.java.jdbc :as jdbc]
            [io.pedestal.interceptor :as interceptor]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.link :as link]))

(defn user-resource [context data]
  (let [{:keys [id]} data]
    (-> data
        (dissoc :id)
        (assoc-in [:links :self] (link/link context :users/show :params {:id id}))
        (update-in [:snippets] (fn [snippets]
                                 (map #(link/link context :snippets/show :params {:id (:id %)}) snippets))))))

(defn get-users [context]
  (->> (jdbc/query (:database context) ["SELECT id, username, created_at, updated_at FROM users"])
       (map (fn [user]
              (assoc user :snippets
                     (jdbc/query (:database context) ["SELECT id FROM snippets WHERE owner=?" (:id user)]))))
       (map (partial user-resource context))))

(def index
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:users (get-users context)}}}))}))

(defn get-user [db id]
  (when-let [user (first (jdbc/query db ["SELECT id, username, created_at, updated_at FROM users WHERE id = ?" id]))]
    (assoc user :snippets (jdbc/query db ["SELECT id FROM snippets WHERE owner = ?" (:id user)]))))

(def with-user
  (interceptor/interceptor
   {:enter (fn [context]
             (let [id (get-in context [:request :path-params :id])]
               (if-let [user (get-user (:database context) id)]
                 (assoc context :user (user-resource context user))
                 (context/terminate context 404))))}))

(def show
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:user (get-in context [:user])
                             :links {:home (link/link context :home)}}}}))}))
