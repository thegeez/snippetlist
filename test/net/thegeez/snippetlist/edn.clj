(ns net.thegeez.snippetlist.edn
  (:require [clojure.test :refer [deftest is]]
            [io.pedestal.log :as log]
            [net.thegeez.w3a.test :as w3a-test]
            [peridot.core :as p]
            [net.thegeez.snippetlist.test-core :as test-core]))

(deftest new-snippet
  (-> (p/session (test-core/ring-handler))
      (p/header "Accept" "application/edn")
      (p/request "http://testhost:-1/snippets")
      ((fn [res]
         (let [csrf-token (get-in res [:response :headers "X-TEST-HELPER-CSRF"])]
           (-> res
               (p/header "X-CSRF-TOKEN" csrf-token)
               ((fn [res]
                  (is (= 101 (get-in res [:response :edn :data :snippets :count])))
                  (is (= 10 (count (get-in res [:response :edn :data :snippets :results]))))
                  (let [new (get-in res [:response :edn :data :links :new])
                        create (get-in res [:response :edn :data :links :create])]
                    (is (= "http://testhost:-1/snippets/new" new))
                    (is (= "http://testhost:-1/snippets" create))
                    (-> res
                          (p/request new)
                          ((fn [res]
                             (is (= (-> res :response :status) 401))
                             (-> res
                                 (p/content-type "application/edn")
                                 (p/request (get-in res [:response :headers "Location"])
                                            :request-method :post
                                            :params ^:edn {:credentials
                                                           {:username "amy"
                                                            :password "amy"}})
                                 ((fn [res]
                                    (is (= (-> res :response :status) 303))
                                    (let [loc (get-in res [:response :headers "Location"])]
                                      (is (= loc "http://testhost:-1/snippets/new"))
                                      (-> res
                                          (p/request loc)
                                          ((fn [res]
                                             (is (= new (-> res :response :edn :breadcrumbs last second)))
                                             (is (= create (get-in res [:response :edn :data :links :create])))
                                             (-> res
                                                 (p/request create
                                                            :request-method :post
                                                            :params ^:edn
                                                            {:snippet
                                                             {:code nil
                                                               :quality
                                                               {:fast true
                                                                :cheap true
                                                                :good true}}})
                                                 ((fn [res]
                                                    (is (= (-> res :response :status) 422))
                                                    (is (= (get-in res [:response :edn :data :snippet :errors])
                                                           {:code ["Code can't be empty"]
                                                            :quality ["Sorry, you can only select up to 2 qualities"]}))
                                                    res))
                                                 (p/request create
                                                            :request-method :post
                                                            :params ^:edn
                                                            {:snippet
                                                             {:code "New test snippet"
                                                              :quality {:fast true :cheap true}}})
                                                 ((fn [res]
                                                    (is (= (-> res :response :status) 201))
                                                    (let [loc (get-in res [:response :headers "Location"])]
                                                      (is (= loc "http://testhost:-1/snippets/102"))
                                                      (-> res
                                                          (p/request loc)
                                                          ((fn [res]
                                                             (is (= (get-in res [:response :edn :data :snippet :code]) "New test snippet"))
                                                             (is (= (get-in res [:response :edn :data :snippet :quality]) #{:fast :cheap}))
                                                             res))))))
                                                 ))))))))))))))))))))

(deftest edit-snippet
  (-> (p/session (test-core/ring-handler))
      (p/header "Accept" "application/edn")
      (p/request "/snippets")
      ((fn [res]
         (let [csrf-token (get-in res [:response :headers "X-TEST-HELPER-CSRF"])]
           (-> res
               (p/header "X-CSRF-TOKEN" csrf-token)
               ((fn [res]
                  (let [snippets (get-in res [:response :edn :data :links :self])]
                    (is (= "http://testhost:-1/snippets" snippets))
                    (is (= 101 (get-in res [:response :edn :data :snippets :count])))
                    (let [self (-> res :response :edn :data :snippets :results first :links :self)
                          ;; edit link is not included when not authenticated
                          edit (str self "/edit")]
                      (is (= "http://testhost:-1/snippets/1/edit" edit))
                      (-> res
                          (p/request edit)
                          ((fn [res]
                             (is (= (-> res :response :status) 401))
                             (-> res
                                 (p/content-type "application/edn")
                                 (p/request (get-in res [:response :headers "Location"])
                                            :request-method :post
                                            :params ^:edn
                                            {:credentials
                                             {:username "amy"
                                              :password "amy"}})
                                 ((fn [res]
                                    (is (= (-> res :response :status) 303))
                                    (-> res
                                        (p/request (get-in res [:response :headers "Location"]))
                                        ((fn [res]

                                           (is (= "http://testhost:-1/snippets/1/edit" (-> res :response :edn :breadcrumbs last second)))
                                           res))
                                        (p/content-type "application/edn")
                                        (p/request self
                                                   :request-method :post
                                                   :params ^:edn
                                                   {:snippet
                                                    {:code nil}})
                                        ((fn [res]
                                           (is (= (-> res :response :status) 422))
                                           (is (= (get-in res [:response :edn :data :snippet :errors])
                                                  {:code ["Code can't be empty"]}))
                                           res))
                                        (p/request self
                                                   :request-method :post
                                                   :params ^:edn
                                                   {:snippet
                                                    {:code "Edited snippet code"}})
                                        ((fn [res]
                                           (is (= (-> res :response :status) 204))
                                           (let [loc (get-in res [:response :headers "Location"])]
                                             (is (= loc "http://testhost:-1/snippets/1"))
                                             (-> res
                                                 (p/request loc)
                                                 ((fn [res]
                                                    (is (= (get-in res [:response :edn :data :snippet :code]) "Edited snippet code"))
                                                    res))))))
                                        )))))))))))))))))

