(ns net.thegeez.snippetlist.auth
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.interceptor :as interceptor]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.link :as link]
            [net.thegeez.snippetlist.users :as users]))

(defn get-auth-by-id [db id]
  (first (jdbc/query db ["SELECT id, username, created_at, updated_at FROM users WHERE id = ?" id])))

(defn get-auth-by-credentials [db values]
  (let [{:keys [username password]} values]
    (when-let [user (first (jdbc/query db ["SELECT * FROM users WHERE username = ?" username]))]
      (when (hashers/check password (:password_encrypted user))
        (dissoc user :password_encrypted)))))

(def with-auth
  (interceptor/interceptor
   {:enter (fn [context]
             (let [context (assoc context :login
                                  {:links
                                   {:login (link/link context :auth/login :params {:next (link/self context)})}})]
               (if-let [user (when-let [id (get-in context [:request :session :auth :id])]
                               (-> (get-auth-by-id (:database context) id)
                                   (update-in [:links] merge {:self (link/link context :users/show :params {:id id})
                                                              :logout (link/link context :auth/logout :params {:next (link/self context)})})))]
                 (assoc context :auth user)
                 context)))}))

(def login-form
  [{:id :username
    :label "Username"
    :type :string
    :validator (fn [username]
                 (cond
                  (not (seq username))
                  "Name can't be empty"
                  (try (Long/parseLong username)
                       (catch Exception _ nil))
                  "Name can't be a number"))}
   {:id :password
    :label "Password"
    :type :password
    :validator (fn [password]
                 (when (not (seq password))
                   "Password can't be empty"))}])

(def login
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :session {:auth nil} ;; visit /login is auto logout
                      :data {:links {:login (link/link-with-next context :auth/login-post)
                                     :home (link/link context :home)}}}}))}))

(def login-post
  (interceptor/interceptor
   {:enter (fn [context]
             (let [credentials (get-in context [:request :data :credentials])
                   fail-context (merge context
                                       {:response
                                        {:status 422
                                         :flash {:error "Login failed"}
                                         :data {:credentials credentials
                                                :links {:login (link/link-with-next context :auth/login-post)
                                                        :home (link/link context :home)}}}})]
               (if (:errors credentials)
                 fail-context
                 (if-let [auth (get-auth-by-credentials (:database context) credentials)]
                   (merge context
                          {:response
                           {:status 303
                            :headers {"Location"
                                      (link/next-or-link context :users/show :params {:id (:id auth)})}
                            :session {:auth {:id (:id auth)}}
                            :flash {:info "Login successful"}}})
                   fail-context))))}))

(def logout-post
  (interceptor/interceptor
   {:leave (fn [context]
             (merge context
                    {:response
                     {:status 303
                      :headers {"Location" (link/next-or-link context :home)}
                      :session {:auth nil}
                      :flash {:info "Logout successful"}}}))}))


;; TODO make an (require-auth :except [:snippets/show
;; :snippets/index])

(def require-authentication
  (interceptor/interceptor
   {:enter (fn [context]
             (if-not (:auth context)
               (-> (merge context
                          {:response
                           {:status 401
                            :headers {"Location" (link/link context :auth/login
                                                            :params {:next (link/self context)})}
                            :flash {:info "You need to be logged in for that action."}}})
                   context/terminate)
               context))}))

(def require-authorization
  (interceptor/interceptor
   {:enter (fn [context]
             (if (and (:auth context)
                      (:snippet context)
                      (= (:owner (:snippet context))
                         (:id (:auth context))))
               context
               (-> (merge context
                          {:response
                           {:status 403
                            :headers {"Location" (link/link context :auth/login
                                                            :params {:next (link/self context)})}
                            :flash {:info "You are not authorized to do that"}}})
                   context/terminate)))}))
