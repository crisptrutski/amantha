(ns amantha.routing
  (:require
    [goog.events :as ge]
    [goog.history.EventType :as EventType]
    [re-frame.core :refer [dispatch-sync]]
    [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

(def app-state (atom nil))

(defn- navigate [section params]
  (swap! app-state #(-> % (assoc :nav section) (assoc :url-params params)))
  (dispatch-sync [:navigate section params]))

;; define routes

(defroute explore-route "/explore/:section" [section]
  (navigate (keyword section) {}))

;; bootstrap

(secretary/set-config! :prefix "#")

(defn start! []
  (let [h (History.)]
    (ge/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))))
