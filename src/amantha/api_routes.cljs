(ns amantha.api-routes
  (:require [clojure.string :as str]
            [amantha.utils :as u]
            [amantha.utils.config :as config]))

(def base
  (config/get-env :api-base-url ""))

(defn- build-url [& segments]
  (str/join "/" (cons base (map u/safe-name segments))))

;; urls

(defn suggested-repair-stock [repair-id]
  (build-url :repairs repair-id :suggested-stock))
