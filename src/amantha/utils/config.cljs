(ns amantha.utils.config
  (:require [clojure.string :as str]))

(defn const-str [s]
  (.toUpperCase (str/replace (name s) "-" "_")))

(defn- get-env
  ([key]
   (get-env key nil))
  ([key default]
   (or (and js/window (aget js/window (const-str key)))
       default)))
