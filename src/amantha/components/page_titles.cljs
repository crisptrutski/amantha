(ns amantha.components.page-titles
  (:require [amantha.utils :refer [titlecase]]
            [re-frame.core :refer [register-handler register-sub subscribe]]
            [amantha.routing :as routing]
            [amantha.components.generic-reagent :as v]))

;; helpers

(defn customer-name [customer]
  (or (get-in customer [:person :full-name])
      (get-in customer [:company :name])))

;; page titles

(defmulti page-title*
  (fn [section]
    section))

(defmethod page-title* :repair-overview
  [_]
  [:h1 "Repair Overview"])

(defmethod page-title* :repair-detail
  [_]
  (let [url-params  (subscribe [:url-params])
        repair-id   (:repair-id @url-params)
        repair      (subscribe [:repair repair-id])
        customer-id (:client-id @repair)
        customer    (subscribe [:customer (keyword customer-id)])
        name        (customer-name @customer)]

    [:div
     [:ol.breadcrumb
      [:li [:a {:href (routing/repairs-incoming-route)} "Repairs"]]
      [:li repair-id]]
     [:div.page-heading

      [:h1
       [:a {:href (amantha.routing/repair-route {:repair-id repair-id})}
        (str "Repair " repair-id)]
       (if name
         [:span
          [:span " for "]
          [:a {:href (amantha.routing/customers-edit-route {:customer-id customer-id})} name]]
         nil)]]]))

(defmethod page-title* :customers-new
  [_]
  [:h1 "New Customer"])

(defmethod page-title* :customers-edit
  [_]
  [:h1 "Edit Customer"])

(defmethod page-title* :devices-new
  [_]
  (let [url-params  (subscribe [:url-params])
        customer-id (:customer-id @url-params)
        customer    (subscribe [:customer customer-id])]
    [:h1
     "New device"
     (if-let [cname (customer-name @customer)]
       [:span
        [:span " for "]
        [:a {:href (amantha.routing/customers-edit-route {:customer-id (name customer-id)})} cname]])]))

(defmethod page-title* :devices-edit
  [_]
  (let [url-params (subscribe [:url-params])
        device-id  (:device-id @url-params)
        device     (subscribe [:device device-id])
        customer   (subscribe [:customer (keyword (:customer-id @device))])
        name       (customer-name @customer)]
    [:h1
     "Edit device"
     (if name
       [:span
        [:span " for "]
        [:a {:href (amantha.routing/customers-edit-route {:customer-id (:customer-id @device)})} name]])]))

(defmethod page-title* :default
  [section]
  [:h1 (if section (titlecase (name section)))])

(defn page-title []
  (let [current-page (subscribe [:current-page])]
    (fn []
      [page-title* @current-page])))
