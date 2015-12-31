(ns amantha.utils.config
  (:require
    [clojure.string :as str]))

;; helpers

(defn app-name
  "Guess service name. Must be called from an app namespace."
  []
  (->> (str/split (str *ns*) #"\.")
       (take 2)
       (str/join ".")))

;; layers

(def defaults-
  {:service-name (app-name)})

(defn gen-conf [app-defaults env & [env-whitelist]]
  (merge defaults-
         app-defaults
         (select-keys env env-whitelist)))

(defn override
  "Override map only where vals in overide are non nil"
  [conf overrides]
  (reduce (fn [c [k v]] (if (nil? v) c (assoc c k v))) conf overrides))
