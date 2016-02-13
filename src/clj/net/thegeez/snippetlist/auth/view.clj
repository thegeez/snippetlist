(ns net.thegeez.snippetlist.auth.view
  (:require [net.cgrand.enlive-html :as enlive]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.snippetlist.layout :as layout]
            [net.thegeez.snippetlist.auth :as auth]))

(layout/application html-render-login
                    [context]
                    [:.navbar] nil
                    [:ul.breadcrumb] nil
                    [:#content] (enlive/before
                                 (enlive/html [:h1 "Snippetlist login"]
                                              [:div "Login with amy/amy or bob/bob"]
                                              (form/html-form context {:action (get-in context [:response :data :links :login])
                                                                       :fields (form/form-fields :credentials
                                                                                                 auth/login-form
                                                                                                 (get-in context [:response :data :credentials]))
                                                                       :submit "Login"}))))
