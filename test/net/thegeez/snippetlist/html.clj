(ns net.thegeez.snippetlist.html
  (:require [clojure.test :refer [deftest is]]
            [io.pedestal.log :as log]
            [net.cgrand.enlive-html :as html]
            [net.thegeez.w3a.test :as w3a-test :refer [follow at?]]
            [kerodon.core :as k]
            [kerodon.test :as t]
            [peridot.core :as p]
            [net.thegeez.snippetlist.test-core :as test-core]))

(deftest snippet
  (-> (k/session (test-core/ring-handler))
      (p/header "Accept" "text/html")
      (k/visit "/")
      (follow "/snippets")
      (k/within [:#content]
                (t/has (t/link? "http://testhost:-1/snippets/new"))
                (t/has (t/link? "http://testhost:-1/snippets/1"))
                (t/has (t/link? "http://testhost:-1/snippets/2")))
      (follow "http://testhost:-1/snippets/new")
      (k/follow-redirect)
      (at? "/login")
      (k/within [:div#flash-info]
                (t/has (t/text? "You need to be logged in for that action.")))
      (k/within [[:form (html/attr= :action "http://testhost:-1/login?next=http%3A%2F%2Ftesthost%3A-1%2Fsnippets%2Fnew")]]
                (k/fill-in "Username" "amy")
                (k/fill-in "Password" "amy")
                (k/press "Login"))
      (k/follow-redirect)
      (at? "/snippets/new")
      (k/within [:ul.breadcrumb]
                (k/within [[:li html/first-of-type]]
                          (t/has (t/link? "Home" "http://testhost:-1/")))
                (k/within [[:li (html/nth-of-type 2)]]
                          (t/has (t/link? "Snippets" "http://testhost:-1/snippets"))))
      (k/within [(html/left [:label (html/attr= :for "snippet[owner_username]")])]
                (t/has (t/text? "amy")))
      (k/press "Create new snippet")
      (at? "/snippets")
      (k/within [:#flash-error]
                (t/has (t/text? "Creating snippet failed")))
      (k/within [[:form (html/attr= :action "http://testhost:-1/snippets")]]
                (k/within [:span.help-block]
                          (t/has (t/text? "Code can't be empty"))))
      (k/fill-in "Code" "Amy's new snippet")
      (k/check "Fast")
      (k/check "Good")
      (k/check "Cheap")
      (k/press "Create new snippet")
      (at? "/snippets")
      (k/within [(html/lefts [:label (html/attr= :for "snippet[quality]")])
                 :span.help-block]
                (t/has (t/text? "Sorry, you can only select up to 2 qualities")))
      (k/uncheck "Good")
      (k/press "Create new snippet")
      (k/follow-redirect)
      (k/within [:#flash-info]
                (t/has (t/text? "Snippet inserted")))
      (k/within [(html/left [:label (html/has [(html/text-pred #{"Code"})])]) :p]
                (t/has (t/text? "Amy's new snippet")))
      (k/within [(html/left [:label (html/has [(html/text-pred #{"Quality"})])]) :p]
                (t/has (t/text? "#{:fast :cheap}")))
      (follow "http://testhost:-1/snippets/102/edit")
      (k/fill-in "Code" "Amy's edited snippet")
      (k/press "Edit")
      (k/follow-redirect)
      (at? "/snippets/102")
      (k/within [:#flash-info]
                (t/has (t/text? "Snippet updated")))
      (k/within [(html/left [:label (html/has [(html/text-pred #{"Code"})])]) :p]
                (t/has (t/text? "Amy's edited snippet")))
      (follow "Snippets")
      (at? "/snippets")
      (follow "11")
      (at? "/snippets?page=11")
      (k/within [(html/lefts [:td.id (html/has [(html/text-pred #{"102"})])])
                 :form]
                (k/press "Delete"))
      (k/follow-redirect)
      (at? "/snippets")
      (k/within [:#flash-info]
                (t/has (t/text? "Snippet deleted")))
      (k/visit "/snippets/102")
      (t/has (t/text? "Not found"))
      ))
