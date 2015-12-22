(ns amantha.routing
  (:require [amantha.state :refer [app-state]]
            [goog.events]
            [goog.history.EventType :as EventType]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

(secretary/set-config! :prefix "#")

(defn- navigate [section params]
  (swap! app-state #(-> %
                        (assoc :nav section)
                        (assoc :url-params params)))
  (dispatch-sync [:navigate section params]))

(defroute explore-route "/explore/:section" [section]
  (navigate (keyword section) {}))

(defroute repairs-incoming-route "/repairs/incoming" []
  (navigate :repair-overview {}))

(defroute repair-route "/repairs/:repair-id" [repair-id]
  (navigate :repair-detail {:repair-id repair-id}))

(defroute customers-route "/customers" []
  (navigate :customers {}))

(defroute customers-new-route "/customers/new" []
  (navigate :customers-new {}))

(defroute customers-edit-route "/customers/:customer-id" [customer-id]
  (navigate :customers-edit {:customer-id (keyword customer-id)}))

(defroute devices-new-route "/customers/:customer-id/devices/new" [customer-id]
  (navigate :devices-new {:customer-id (keyword customer-id)}))

(defroute devices-edit-route "/devices/:device-id" [device-id]
  (navigate :devices-edit {:device-id (keyword device-id)}))

(defroute quote-route "/quotes/:access-token" [access-token]
  (navigate :quote-acceptance {:access-token access-token}))

(defn start! []
  (let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))))
