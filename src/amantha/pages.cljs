(ns amantha.pages
  (:require [amantha.utils :refer [titlecase]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defmulti page-view (fn [x & _] (:nav x)))

(defmethod page-view :default [app owner]
  (om/component
   (html
    [:div
     [:h2 (titlecase (:nav app))]
     [:br]
     [:h4 "Page not found"]])))
