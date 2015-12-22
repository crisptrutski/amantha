(ns amantha.state
  (:require [amantha.utils :as utils]
            [amantha.config.repairs :as headers]
            [amantha.data.permissions :as perms]
            [om.core :as om]
            [re-frame.core :refer [register-handler dispatch-sync]]))

(def firebase-uri "https://braaisoft-test-crm.firebaseio.com")

(def base-data {:nav     :repair-overview
                :grids   headers/grids
                :temp    {:histogram {}, :grid {}}
                :filters (headers/filters-for nil)})

(register-handler
  :init-app-db
  (constantly base-data))

(dispatch-sync [:init-app-db])

(defonce app-state (atom base-data))

(add-watch app-state :current-user-changed (fn [key ref old-state new-state]
                                             (when (not= (:current-user new-state) (:current-user old-state))
                                               (swap! app-state assoc :filters (headers/filters-for (:current-user new-state))))))

(defn set-user! [user]
  (swap! app-state assoc :current-user user))

(comment
  (reset! app-state base-data))
