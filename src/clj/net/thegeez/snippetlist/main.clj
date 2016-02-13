(ns net.thegeez.snippetlist.main
  (:require [io.pedestal.log :as log]
            [com.stuartsierra.component :as component]
            [clojure.string :as string]
            [net.thegeez.w3a.components.sql-database :as sql-database]
            [net.thegeez.w3a.components.sql-database.migrator :as migrator]
            [net.thegeez.snippetlist.core :as core]
            [net.thegeez.snippetlist.database.fixtures :as fixtures]
            [net.thegeez.snippetlist.database.migrations :as migrations])
  (:gen-class))

(defn -main [& args]
  (log/info :main "Running main" :args args)
  (let [port (try (Long/parseLong (first args))
                  (catch Exception _ -1))
        _ (assert (pos? port) (str "Something is wrong with the port argument: " (first args)))
        database-url (let [db-url (second args)]
                       (assert (.startsWith db-url "postgres:")
                               (str "Something is wrong with the database argument: " (second args)))
                       (sql-database/db-url-for-heroku db-url))
        cookie-key (nth args 2)
        _ (assert (and (string? cookie-key)
                       (= 16 (count cookie-key)))
                  "cookie-key needs to be 16 characters")
        system (core/prod-system {:db-connect-string database-url
                                  :port port
                                  :cookie-key cookie-key})]
    (component/start system)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (log/info :main "Shutting down main")
                                 (component/stop system))))))

(defn database-migrate []
  (let [db-url (System/getenv "DATABASE_URL")
        _ (assert (.startsWith db-url "postgres:")
                  (str "Something is wrong with the database argument: " db-url))
        db-connect-string (sql-database/db-url-for-heroku db-url)
        db-spec {:connection-uri db-connect-string}]
    (log/info :msg "Running the database migrator")
    (migrator/migrate! db-spec migrations/migrations)
    (log/info :msg "Database migrator done")))

(defn database-fixtures []
  (let [db-url (System/getenv "DATABASE_URL")
        _ (assert (.startsWith db-url "postgres:")
                  (str "Something is wrong with the database argument: " db-url))
        db-connect-string (sql-database/db-url-for-heroku db-url)
        db-spec {:connection-uri db-connect-string}]
    (log/info :msg "Running the database fixtures inserter")
    (fixtures/insert-fixtures! db-spec)
    (log/info :msg "Database fixtures done")))
