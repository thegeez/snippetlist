(ns net.thegeez.snippetlist.snippets
  (:require [io.pedestal.interceptor :as interceptor]
            [clojure.java.jdbc :as jdbc]
            [net.thegeez.w3a.context :as context]
            [net.thegeez.w3a.html :as html]
            [net.thegeez.w3a.link :as link]
            [net.thegeez.w3a.pagination :as pagination]))

(defn snippet-resource [context snippet]
  (let [{:keys [id owner]} snippet]
    (-> snippet
        (assoc :quality
          (set (for [[k v] {:quality_cheap :cheap
                            :quality_fast :fast
                            :quality_good :good}
                     :when (get snippet k)]
                 v)))
        (dissoc :quality_cheap :quality_fast :quality_good)
        (assoc-in [:links :self] (link/link context :snippets/show :params {:id id}))
        (assoc-in [:links :owner]
                  (link/link context :users/show :params {:id owner}))
        (cond->
         (= owner (get-in context [:auth :id]))
         (-> (assoc-in [:links :edit] (link/link context :snippets/edit :params {:id id}))
             (assoc-in [:links :delete] (link/link context :snippets/delete :params {:id id})))
         ))))

(defn get-snippets [context]
  (let [{:keys [page limit] :as params} (pagination/pagination-params context)
        offset (* (dec page) limit)
        db (:database context)
        results (->> (jdbc/query db ["SELECT s.id, s.code, s.owner, u.username as owner_username, s.created_at, s.updated_at FROM snippets s JOIN users u ON u.id = s.owner"])
                     (drop offset)
                     (take limit)
                     (map (partial snippet-resource context)))
        count (:count (first (jdbc/query db ["SELECT count(*) as count FROM snippets s"])))]
    (merge {:results results
            :count count}
           (pagination/links context :snippets/index params count))))

(def index
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                      {:response
                       {:status 200
                        :data {:snippets (get-snippets context)
                               :links {:new (link/link context :snippets/new)
                                       :create (link/self context)
                                       :self (link/self context)}}}}))}))

(defn get-snippet [db id]
  (first (jdbc/query db ["SELECT s.id, s.code, s.owner, u.username as owner_username, s.quality_fast, s.quality_good, s.quality_cheap, s.created_at, s.updated_at FROM snippets s JOIN users u ON u.id = s.owner WHERE s.id=?" id])))

(def with-snippet
  (interceptor/interceptor
   {:enter (fn [context]
             (let [id (get-in context [:request :path-params :id])]
               (if-let [snippet (get-snippet (:database context) id)]
                 (assoc context :snippet (snippet-resource context snippet))
                 (context/terminate context 404))))}))

(def with-empty-snippet
  (interceptor/interceptor
   {:enter (fn [context]
             (assoc context :snippet {:owner_username (get-in context [:auth :username])}))}))

(def show
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:snippet (get-in context [:snippet])}}}))}))

(def new
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:snippet {:owner_username (get-in context [:auth :username])}
                             :links {:create (link/link context :snippets/index)
                                     :self (link/self context)}}}}))}))

(def snippet-form
  [{:id :owner_username
    :label "Owner"
    :type :static}
   {:id :code
    :label "Code"
    :type :string
    :validator (fn [code]
                 (when (not (seq code))
                   "Code can't be empty"))}
   {:id :quality
    :label "Quality"
    :type :checkbox
    :render :snippet/quality-field
    :render/options {:fast "Fast"
                     :good "Good"
                     :cheap "Cheap"}
    :coerce (fn [value]
              (set (keys value)))
    :validator (fn [quality]
                 (when (= (count quality) 3)
                   "Sorry, you can only select up to 2 qualities"))}])

(def edit
  (interceptor/interceptor
   {:enter (fn [context]
             (merge context
                    {:response
                     {:status 200
                      :data {:snippet (get-in context [:snippet])}}}))}))

(defn normalize-snippet-values [values]
  (-> values
      (merge
       (into {} (for [[k v] {:fast :quality_fast
                             :good :quality_good
                             :cheap :quality_cheap}]
                  [v (boolean (get-in values [:quality k]))])))
      (dissoc :quality)))

(defn fail-context [context error-msg]
  (merge context
         {:response
          {:status 422
           :flash {:error error-msg}
           :data {:snippet (merge (get-in context [:snippet])
                                  (get-in context [:request :data :snippet]))
                  :links {:self (link/link context :snippets/index)
                          :create (link/self context)}}}}))

(defn insert-snippet [database values]
  (try
    (jdbc/insert! database
                  :snippets
                  (merge values
                         {:created_at (.getTime (java.util.Date.))
                          :updated_at (.getTime (java.util.Date.))}))
    (catch Exception _
      {:error "Could not insert snippet"})))

(def create
  (interceptor/interceptor
   {:enter (fn [context]
             (if (get-in context [:request :data :snippet :errors])
               (fail-context context "Creating snippet failed")
               (let [values (-> (get-in context [:request :data :snippet])
                                (assoc :owner (get-in context [:auth :id]))
                                normalize-snippet-values)
                     res (insert-snippet (:database context) values)]
                 (if-let [error (:error res)]
                   (fail-context context error)
                   (let [new-id (long (val (ffirst res)))
                         location (link/link context :snippets/show :params {:id new-id})]
                     (merge context
                            {:response
                             {:status 201
                              :headers {"Location" location}
                              :flash {:info "Snippet inserted"}}}))))))}))

(defn update-snippet [database id values]
  (try
    (jdbc/update! database
                  :snippets
                  (merge values
                         {:updated_at (.getTime (java.util.Date.))})
                  ["id = ?" id])
    nil
    (catch Exception _
      {:error "Could not save snippet"})))

(def edit-post
  (interceptor/interceptor
   {:enter (fn [context]
             (if (get-in context [:request :data :snippet :errors])
               (fail-context context "Update failed")
               (let [id (get-in context [:snippet :id])
                     values (-> (get-in context [:request :data :snippet])
                                normalize-snippet-values)
                     res (update-snippet (:database context) id values)]
                 (if-let [error (:error res)]
                   (fail-context context error)
                   (merge context
                          {:response
                           {:status 204
                            :headers {"Location" (get-in context [:snippet :links :self])}
                            :flash {:info "Snippet updated"}}})))))}))

(defn delete-snippet [database id]
  (try
    (jdbc/delete! database
                  :snippets
                  ["id = ?" id])
    nil
    (catch Exception _
      {:error "Could not delete snippet"})))

(def delete
  (interceptor/interceptor
   {:enter (fn [context]
             (let [id (get-in context [:snippet :id])
                   res (delete-snippet (:database context) id)]
               (if-let [error (:error res)]
                 (merge context
                        {:response
                         {:status 303
                          :headers {"Location"
                                    (link/link context :snippets/index)}
                          :flash {:error error}}})
                 (merge context
                        {:response
                         {:status 204
                          :headers {"Location"
                                    (link/link context :snippets/index)}
                          :flash {:info "Snippet deleted"}}}))))}))
