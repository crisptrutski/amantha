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

(defn start! []
  (let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))))
