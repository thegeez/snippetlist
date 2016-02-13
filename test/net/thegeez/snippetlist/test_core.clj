(ns net.thegeez.snippetlist.test-core
  (:require [io.pedestal.log :as log]
            [net.thegeez.w3a.test :as w3a-test]
            [user :as user]))

(defn ring-handler []
  (w3a-test/system->ring-handler (user/dev-system user/dev-config)))


