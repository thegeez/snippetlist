(ns net.thegeez.snippetlist.core
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [io.pedestal.http :as http]
            [net.thegeez.w3a.components.server :as server]
            [net.thegeez.w3a.components.sql-database :as sql-database]
            [ring.middleware.session.cookie]
            [net.thegeez.snippetlist.service :as service]))

(defn prod-system [config-options]
  (log/info :msg "Hello world, this is the production system!")
    (let [{:keys [db-connect-string port cookie-key]} config-options]
    (component/system-map
     :session-options {:store (ring.middleware.session.cookie/cookie-store {:key cookie-key})}
     :server (component/using
              (server/pedestal-component (assoc service/service
                                                ::http/port port
                                                ::server/component->context {:database [:database :spec]}))
              {:database :db
               :session-options :session-options})
     :jetty (component/using
             (server/jetty-component)
             [:server])
     :db (sql-database/sql-database db-connect-string))))
