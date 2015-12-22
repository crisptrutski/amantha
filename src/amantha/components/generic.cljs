(ns amantha.components.generic
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [amantha.utils :refer [glyphicon]]
            [om.core :as om :include-macros true]))

(defn toggle-panel [app title hide-key content]
  (let [show (not (get-in app [:preferences hide-key]))]
    [:div.panel.panel-default
     [:div.panel-heading.clickable
      {:on-click #(om/transact! app [:preferences hide-key] not)}
      [:div.panel-title
       (if show (glyphicon "minus") (glyphicon "plus"))
       " " title " "]]
     (if show content)]))

