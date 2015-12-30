(ns amantha.config
  (:require
    [clojure.string :as str]
    [environ.core :refer [env]])
  (:import
    [java.net ServerSocket InetAddress]))

;; helpers

(defn get-free-port!
  "Warning: opens and closes socket to determine this, so not called automatically"
  []
  (let [socket (ServerSocket. 0)
        port   (.getLocalPort socket)]
    (.close socket)
    port))

(defn- ensure-port [conf]
  (if (:port conf) conf (assoc conf :port (get-free-port!))))

(defn app-name
  "Guess service name. Must be called from an app namespace."
  []
  (->> (str/split (str *ns*) #"\.")
       (take 2)
       (str/join ".")))

(defn guess-host-name []
  (.getCanonicalHostName (InetAddress/getLocalHost)))

;; layers

(def defaults-
  {:host         (guess-host-name)
   :service-name (app-name)})

(defn gen-conf [app-defaults & [env-whitelist is-dev?]]
  (ensure-port
    (merge defaults-
           app-defaults
           (select-keys env env-whitelist))))

(defn override
  "Override map only where vals in overide are non nil"
  [conf overrides]
  (reduce (fn [c [k v]] (if (nil? v) c (assoc c k v))) conf overrides))
