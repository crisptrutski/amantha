(ns amantha.components.generic
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [amantha.data.poll :as poll]
            [bss.rampant.utils :as utils :refer [glyphicon]]
            [cljs.core.async :as async]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn dirty?
  "Test whether potentially new changes from server have not been rendered."
  [owner]
  (om/get-state owner :pending-global-sha-render?))

(defn dirty!
  "Track that component has definitely rerendered since last change was tracked."
  [owner]
  (om/set-state! owner :pending-global-sha-render? true))

(defn clean!
  "Track that component has definitely rerendered since last change was tracked."
  [owner]
  (om/set-state! owner :pending-global-sha-render? nil))

(defn start-tracking-server!
  "Trigger rerender of component when `global-sha` changes on server.
   Renders are triggered by updating component local state.
   Additional state is used to track state for unsubscribing and avoiding multiple
   concurrent subscriptions."
  [owner]
  (when (not (om/get-state owner :global-sha-listener))
    (let [[id ch] (poll/subscribe!)]
      (go-loop []
               (om/set-state! owner :global-sha (<! ch))
               (dirty! owner)
               (recur))
      (om/set-state! owner :global-sha-listener id))))

(defn stop-tracking-server!
  "See `start-tracking-server!`. This removes the subscription."
  [owner]
  (when-let [id (om/get-state owner :global-sha-listener)]
    (poll/unsubscribe! id)
    (om/set-state! owner :global-sha-listener nil)))

(defn tracked-component-shell [owner render-fn & opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (start-tracking-server! owner))

    om/IWillUnmount
    (will-unmount [_]
      (stop-tracking-server! owner))

    om/IRender
    (render [_]
      (apply render-fn opts))))

;;

(defn toggle-panel [app title hide-key content]
  (let [show (not (get-in app [:preferences hide-key]))]
    [:div.panel.panel-default
     [:div.panel-heading.clickable
      {:on-click #(om/transact! app [:preferences hide-key] not)}
      [:div.panel-title
       (if show (glyphicon "minus") (glyphicon "plus"))
       " " title " "]]
     (if show content)]))

