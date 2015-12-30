(ns amantha.api-routes
  (:require [clojure.string :as str]))

(defn- get-window
  ([key]
    (get-window key nil))
  ([key default]
   (or (and js/window (aget js/window key))
       default)))

(def base
  (get-window "API_BASE_URI" ""))

(defn- safe-name [nameable]
  (if (implements? INamed nameable)
    (name nameable)
    (str nameable)))

(defn- build-url [& segments]
  (str/join "/" (cons base (map safe-name segments))))

;; urls

(defn suggested-repair-stock [repair-id]
  (build-url :repairs repair-id :suggested-stock))
