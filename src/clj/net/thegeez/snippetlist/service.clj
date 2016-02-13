(ns net.thegeez.snippetlist.service
  (:require [io.pedestal.log :as log]
            [io.pedestal.http :as http]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [net.thegeez.w3a.breadcrumb :as breadcrumb]
            [net.thegeez.w3a.edn :as edn]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.w3a.link :as link]
            [net.thegeez.snippetlist.auth :as auth]
            [net.thegeez.snippetlist.auth.view :as auth.view]
            [net.thegeez.snippetlist.users :as users]
            [net.thegeez.snippetlist.users.view :as users.view]
            [net.thegeez.snippetlist.snippets :as snippets]
            [net.thegeez.snippetlist.snippets.view :as snippets.view]))

(def home
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
               {:response
                {:status 200
                 :headers {"Content-Type" "text/html"}
                 :body (str "Hello world, snippetlist<br/>"
                            "<a href=\"" (link/link context :users/index) "\">/users</a><br/>"
                            "<a href=\"" (link/link context :snippets/index) "\">/snippets</a><br/>"
                            "<a href=\"" (link/link context :auth/login) "\">/login</a><br/>"
                            "<a href=\"" (link/link context :home) "\">/</a>")}}))}))

(defroutes
  routes
  [[["/"
     ^:interceptors [auth/with-auth
                     (breadcrumb/add-breadcrumb "Home" :home)
                     (html/for-html 404 (constantly "Not found"))

                     (edn/for-edn (fn [context]
                                    {:data (get-in context [:response :data])
                                     :breadcrumbs (:breadcrumbs context)}))]

     {:get [:home home]}
     ["/login"
      {:get
       [:auth/login
        ^:interceptors [(html/for-html 200 auth.view/html-render-login)]
        auth/login]
       :post [:auth/login-post
              ^:interceptors [(html/for-html 422 auth.view/html-render-login)
                              (form/parse-form :credentials auth/login-form)]
              auth/login-post]}]
     ["/logout" {:post [:auth/logout auth/logout-post]}]

     ["/users"
      ^:interceptors [(breadcrumb/add-breadcrumb "Users" :users/index)]
      {:get
       [:users/index
        ^:interceptors [(html/for-html 200 users.view/html-render-index)]
        users/index]}
      ["/:id"
       {:get
        [:users/show
         ^:interceptors [(breadcrumb/add-breadcrumb "User" :users/show {:id [:request :path-params :id]})
                         (html/for-html 200 users.view/html-render-show)
                         (link/coerce-path-params {:id :long})
                         users/with-user]
         users/show]}]]
     ["/snippets"
      ^:interceptors [(breadcrumb/add-breadcrumb "Snippets" :snippets/index)]
      {:get
       [:snippets/index
        ^:interceptors [(html/for-html 200 snippets.view/html-render-index)]
        snippets/index]
       :post
       [:snippets/create
        ^:interceptors [auth/require-authentication
                        (html/for-html 422 snippets.view/html-render-new)
                        (form/parse-form :snippet snippets/snippet-form)
                        snippets/with-empty-snippet]
        snippets/create]}
      ["/new"
       {:get
        [:snippets/new
         ^:interceptors [auth/require-authentication
                         (breadcrumb/add-breadcrumb "New Snippet" :snippets/new)
                         (html/for-html 200 snippets.view/html-render-new)
                         snippets/with-empty-snippet]
         snippets/new]}]
      ["/:id"
       ^:interceptors [(breadcrumb/add-breadcrumb "Snippet" :snippets/show {:id [:request :path-params :id]})
                       (link/coerce-path-params {:id :long})
                       snippets/with-snippet]
       {:get
        [:snippets/show
         ^:interceptors [(html/for-html 200 snippets.view/html-render-show)]
         snippets/show]
        :post
        [:snippets/edit-post
         ^:interceptors [auth/require-authentication
                         auth/require-authorization
                         (html/for-html 422 snippets.view/html-render-edit)
                         (form/parse-form :snippet snippets/snippet-form)]
         snippets/edit-post]
        :delete
        [:snippets/delete
         ^:interceptor [auth/require-authentication
                        auth/require-authorization]
         snippets/delete]}
       ["/edit"
        {:get
         [:snippets/edit
          ^:interceptors [auth/require-authentication
                          auth/require-authorization
                          (breadcrumb/add-breadcrumb "Edit" :snippets/edit {:id [:request :path-params :id]})
                          (html/for-html 200 snippets.view/html-render-edit)]
          snippets/edit]}]
       ]]]]])

(def bootstrap-webjars-resource-path "META-INF/resources/webjars/bootstrap/3.3.4")
(def jquery-webjars-resource-path "META-INF/resources/webjars/jquery/1.11.1")

(def service
  {:env :prod
   ::http/router :linear-search

   ::http/routes routes

   ::http/resource-path "/public"

   ::http/default-interceptors [(middlewares/resource bootstrap-webjars-resource-path)
                                (middlewares/resource jquery-webjars-resource-path)]

   ::http/type :jetty
   })
