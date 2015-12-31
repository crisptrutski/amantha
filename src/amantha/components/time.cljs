(ns amantha.components.time
  (:require [reagent.core :as reagent]))

(defonce
  ^{:doc "Reactive atom holding current time. Accurate to 1s"}
  current-time
  (reagent/atom (js/Date.)))

(defn- update-current-time []
  (reset! current-time (js/Date.)))


