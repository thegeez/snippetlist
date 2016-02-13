(ns net.thegeez.snippetlist.snippets.view
  (:require [net.cgrand.enlive-html :as enlive]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.w3a.pagination :as pagination]
            [net.thegeez.snippetlist.layout :as layout]
            [net.thegeez.snippetlist.snippets :as snippets]))

(def snippet-table-template (enlive/html-resource "templates/snippets_table.html"))

(layout/application html-render-index
                    [context]
                    [:#content] (enlive/before
                                 (pagination/html (get-in context [:response :data :snippets])))
                    [:#content] (let [new (get-in context [:response :data :links :new])]
                                  (enlive/before
                                   (enlive/html
                                    [:br]
                                    [:a.btn.btn-default {:href new}
                                     "New snippet"])))
                    [:#content] (enlive/before
                                 (enlive/at snippet-table-template
                                            [:tbody :tr] (enlive/clone-for
                                                          [{:keys [id code owner_username links] :as snippet}
                                                           (get-in context [:response :data :snippets :results])]
                                                          [:td.id] (enlive/content (str id))
                                                          [:td.code] (enlive/content code)
                                                          [:td.owner :a] (enlive/do->
                                                                          (enlive/content owner_username)
                                                                          (enlive/set-attr :href (:owner links)))
                                                          [:td.links :a.show-link] (enlive/set-attr :href (:self links))
                                                          [:td.links :a.edit-link] (if-let [edit-link (:edit links)]
                                                                                     (enlive/set-attr :href edit-link)
                                                                                     (enlive/add-class "disabled"))
                                                          [:td.links :form.link] (if-let [delete-link (:delete links)]
                                                                                   (enlive/set-attr :action delete-link)
                                                                                   identity)
                                                          [:td.links :#__anti-forgery-token] (enlive/set-attr :value (get-in context [:request :io.pedestal.http.csrf/anti-forgery-token]))
                                                          [:td.links :form :button] (if (:delete links)
                                                                                      identity
                                                                                      (enlive/add-class "disabled")))))
                    [:#content] (enlive/before
                                 (pagination/html (get-in context [:response :data :snippets])))
                    [:#content] (enlive/html-content
                                 (html/edn->html (get-in context [:response :data]))))

(layout/application html-render-show
  [context]
  [:#content] (enlive/before
               (enlive/html
                [:div
                 [:h3 "Show snippet"]
                 [:form.form-horizontal
                  (for [[k v] (-> (get-in context [:response :data :snippet])
                                  (dissoc :id :owner))]
                    [:div.form-group
                     [:label.control-label.col-sm-2
                      (get
                       {:code "Code"
                        :owner_username "Owner username"
                        :links "Links"
                        :quality "Quality"}
                       k k)]
                     [:div.col-sm-10
                      [:p.form-control-static
                       (cond
                        (= k :links)
                        (interpose ", "
                                   (for [[title href] v]
                                     [:span title " "[:a {:href href} href]]))
                        (contains? #{:created_at :updated_at} k)
                        (str v " / " (java.util.Date. v))
                        :else
                        v) ]]])]]))
  [:#content] (enlive/html-content
               (html/edn->html (get-in context [:response :data]))))

(defmethod form/render-form-field :snippet/quality-field
  [{:keys [id label name :render/options]} value errors]
  [:div
     {:class (str "form-group"
                  (when errors
                    " has-error"))}
     [:label.control-label
      {:for name} label]
     [:div
      (for [[val s] options]
        (let [id (str name "[" (clojure.core/name val) "]")]
          [:div.checkbox
           [:label {:for id}
            [:input
             (cond->
              {:type "checkbox"
               :name id
               :id id}
              (boolean (get value val))
              (assoc :checked "checked") )]
            s]]))]
   (when errors
     [:div.errors
      (for [error errors]
        [:span.help-block error])])])

(layout/application html-render-new
  [context]
  [:#content] (enlive/before
               (enlive/html [:h1 "New snippet"]
                            (form/html-form context {:action (get-in context [:response :data :links :create])
                                                     :fields (form/form-fields :snippet
                                                                               snippets/snippet-form
                                                                               (get-in context [:response :data :snippet]))
                                                     :submit "Create new snippet"})))
  [:#content] (enlive/html-content
               (html/edn->html (get-in context [:response :data]))))

(layout/application html-render-edit
  [context]
  [:#content] (enlive/before
               (enlive/html [:h1 "Edit snippet"]
                            (form/html-form context {:action (get-in context [:response :data :snippet :links :self])
                                                     :fields (form/form-fields :snippet
                                                                               snippets/snippet-form
                                                                               (get-in context [:response :data :snippet]))
                                                     :submit "Edit"})))
  [:#content] (enlive/html-content
               (html/edn->html (get-in context [:response :data]))))
