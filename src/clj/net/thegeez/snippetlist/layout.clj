(ns net.thegeez.snippetlist.layout
  (:require [net.cgrand.enlive-html :as enlive]
            [net.thegeez.w3a.breadcrumb :as breadcrumb]
            [net.thegeez.w3a.link :as link]))

(defn login-box-html [context]
  (if-let [auth (get-in context [:auth])]
    (enlive/transform-content
     [:a#name] (enlive/content (:username auth))
     [:a#user] (enlive/set-attr :href (get-in auth [:links :self]))
     [:form#logout] (enlive/do->
                     (enlive/prepend
                      (enlive/html [:input {:type "hidden"
                                            :name "__anti-forgery-token"
                                            :value (get-in context [:request :io.pedestal.http.csrf/anti-forgery-token])}]))
                     (enlive/set-attr :action (get-in auth [:links :logout]))))
    (enlive/transform-content
     [:li]
     (enlive/content (enlive/html [:a {:href (get-in context [:login :links :login])}
                                   "Login"])))))

(def application-template "templates/application.html")

(defmacro application [name args & transformations]
  (assert (= (take 1 args) '[context])
    "layout requires context as first parameter")
  `(enlive/deftemplate ~name ~application-template
     ~args
     [:#login-box]
     (login-box-html (first ~args))
     [:#flash-error] (when-let [error# (or (get-in (first ~args) [:request :flash :error])
                                           (get-in (first ~args) [:response :flash :error]))]
                       (enlive/content error#))
     [:#flash-info] (when-let [info# (or (get-in (first ~args) [:request :flash :info])
                                         (get-in (first ~args) [:response :flash :info]))]
                      (enlive/content info#))
     [:#content] (enlive/before
                  (breadcrumb/html (get-in (first ~args) [:breadcrumbs])))
     ~@transformations))
