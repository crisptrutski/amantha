(ns amantha.api-routes
  (:require [clojure.string :as str]
            [amantha.utils :as u]))

(defn- get-window
  ([key]
    (get-window key nil))
  ([key default]
   (or (and js/window (aget js/window key))
       default)))

(def base
  (get-window "API_BASE_URI" ""))

(defn- build-url [& segments]
  (str/join "/" (cons base (map u/safe-name segments))))

;; urls

(defn suggested-repair-stock [repair-id]
  (build-url :repairs repair-id :suggested-stock))
