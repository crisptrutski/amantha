(ns amantha.api-routes
  (:require [clojure.string :as str]))

(def base
  (or (and js/window (aget js/window "API_BASE_URI"))
      ""))

(defn- safe-name [nameable]
  (if (implements? INamed nameable)
    (name nameable)
    (str nameable)))

(defn- build-url [& segments]
  (str/join "/" (cons base (map safe-name segments))))

;; urls

(defn global-sha []
  (build-url :global-sha))

(defn repairs []
  (build-url :repairs))

(defn read-repair [id]
  (build-url :repairs id))

(defn add-diagnosis-to-repair [& {:keys [id]}]
  (build-url :repairs id :add-diagnosis))

(defn remove-diagnosis-from-repair [& {:keys [id]}]
  (build-url :repairs id :remove-diagnosis))

(defn set-diagnosis-description [& {:keys [id]}]
  (build-url :repairs id :set-extra-diagnosis))

(defn update-status [id status]
  (build-url :repairs id :status status))

(defn assign-repair [id assign-to]
  (build-url :repairs id :assign-to assign-to))

(defn technician-repairs [technician-email-address]
  (build-url :users technician-email-address :repairs))

(defn repair-diagnoses [device-model]
  (build-url :diagnosis device-model :list))

(defn histogram [data-key hist-key]
  (build-url :histogram data-key :by hist-key))

(defn aggregate [data-key attr-key group-key]
  (build-url :aggregate data-key :on attr-key :by group-key))

(defn grid [data-key]
  (build-url :grid data-key))

(defn repair-quotes [repair-id]
  (build-url :repairs repair-id :quotes))

(defn create-quote []
  (build-url :quotes))

(defn quote [id]
  (build-url :quotes id))

(defn set-quote-status [quote-id status]
  (build-url :quotes quote-id :status status))

(defn stock-search []
  (build-url :stock-items :search))

(defn suggested-repair-stock [repair-id]
  (build-url :repairs repair-id :suggested-stock))
