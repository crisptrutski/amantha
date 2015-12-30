(ns amantha.utils.http
  (:require [amantha.utils :as u]
            [clojure.string :as str]
            #?(:cljs [cljs.reader :as reader])))

;; TODO: provide http lib
(declare GET)

(defn wrap-edn-callback [callback]
  (if-not callback
    u/no-op
    (fn [reply]
      #?(:cljs
         (-> (.-target reply)
             .getResponseText
             reader/read-string
             callback)))))

(defn get-data
  ([url params handler]
   (get-data url params handler println))
  ([url params handler error-handler]
    ;; TODO: abstract this away from given ajax lib
   (GET url
        {:handler         handler
         :params          params
         :error-handler   error-handler
         :response-format :edn})))

(defn build-http-vars [payload]
  (str/join "&" (map (fn [[k v]] (str (name k) "=" (pr-str v))) payload)))

