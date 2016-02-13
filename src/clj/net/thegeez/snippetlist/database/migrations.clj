(ns net.thegeez.snippetlist.database.migrations
  (:require [io.pedestal.log :as log]
            [clojure.java.jdbc :as jdbc]
            [net.thegeez.w3a.components.sql-database.migrator :as migrator]))

(defn serial-id [db]
  (if (.contains (:connection-uri db) "derby")
    [:id "INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)"]
    [:id :serial "PRIMARY KEY"]))

(def migrations
  [[1 migrator/version-migration]
   (let [table :users]
     [2 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:username "VARCHAR(256) UNIQUE"]
                    [:password_encrypted "VARCHAR(256)"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         ;; todo also add indexes for lookup by oauth id
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])
   (let [table :snippets]
     [3 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:code "VARCHAR(1024) NOT NULL"]
                    [:quality_good "BOOLEAN DEFAULT FALSE"]
                    [:quality_fast "BOOLEAN DEFAULT FALSE"]
                    [:quality_cheap "BOOLEAN DEFAULT FALSE"]
                    [:owner "BIGINT"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])])
