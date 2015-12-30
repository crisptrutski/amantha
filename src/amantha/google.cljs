(ns amantha.google
  (:require #_[ajax.core :refer [GET]]
            #_[gapi.js]))

(declare GET)

(defn signed-in? [result]
  (.. result -status -signed_in))

(defn profile [cb]
  (-> (.load js/gapi.client "plus" "v1")
      (.then #(.get js/gapi.client.plus.people #js {"userId" "me"}))
      ;; TODO: kebab-case the names too
      (.then #(js->clj % :keywordize-keys true))
      (.then cb)))

(defn get-token []
  (when-let [token (.getToken js/gapi.auth)]
    (.-access_token token)))

(defn build-identity [result]
  {:name  (get-in result [:result :displayName])
   :email (->> (get-in result [:result :emails])
               (filter (comp #{"account"} :type))
               first
               :value)
   :source result})

(defn handle-sign-in [state auth-result]
  (if (signed-in? auth-result)
    (profile (comp #(swap! state :session %) build-identity))
    (prn "Sign in state: " (keyword (.-error auth-result)))))

(defn sign-in [state]
  (.signIn js/gapi.auth #js {"callback" (partial handle-sign-in state)}))

(defn disconnect [& [cb]]
  ;; FIXME: this is hitting a CORS error, should be sent as JSONP instead
  (GET (str "https://accounts.google.com/o/oauth2/revoke?token=" (get-token))
      {:error-handler cb
       :handler       cb}))

(defn sign-in-view [state]
  [:button#g-signin {:on-click (partial sign-in state)}
   [:span.icon]
   [:span.buttonText "Sign in"]])

(defn sign-out [state]
  (disconnect (fn [] (swap! state dissoc :session))))
