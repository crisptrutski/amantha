(ns amantha.pages.customers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [amantha.components.forms :as forms]
    [amantha.pages :refer [page-view]]
    [amantha.customers.data :refer [customer-name]]
    [amantha.utils :refer [handler-fn]]
    [bss.rampant.utils :refer [set-url! e->value]]
    [om.core :as om]
    [om.dom :as dom]
    [reagent.core :as reagent]
    [reagent.ratom :as ratom]
    [re-frame.core :refer [dispatch subscribe]]))

;; helpers

(defn customer-email [{:keys [contact person company]}]
  (or (:email contact)
      (:email company)))

(defn customer-company [{:keys [company]}]
  (or (:name company)
      "(empty)"))

(defn validate [data]
  (if-not (some #(get-in data %) [[:contact :email]
                                  [:person :full-name]
                                  [:company :name]
                                  [:company :email]])
    ["Must provide one of full name, email or company name"]))

;; UI

(defn customer-row [{:keys [id] :as customer}]
  [:tr {:on-click #(set-url! (routing/customers-edit-route {:customer-id (name id)}))}
   [:td (customer-name customer)]
   [:td (customer-email customer)]
   [:td (customer-company customer)]
   [:td
    [:a {:on-click (handler-fn #(dispatch [:delete-customer customer]))} "Delete"]]])

(defn customer-table [customers]
  [:div
   [:h3 (count customers) " Customers"]
   [:table.table.table-hover
    [:thead [:th "Name"] [:th "Email"] [:th "Company"] [:th "Actions"]]
    [:tbody
     (for [customer customers]
       ^{:key (:id customer)}
       [customer-row customer])]]])

(defn loading-view [inner-view data]
  (if (nil? @data)
    [:h3 "Loading..."]
    (inner-view @data)))

(defn customer-list-view []
  [loading-view customer-table (subscribe [:customers])])

(defn customer-list-actions []
  [:div
   [:a.btn.btn-primary {:href (routing/customers-new-route)} "New Customer"]])

(defn contact-fields [state]
  (let [notes? (ratom/atom false)]
    (fn []
      [:div
       [:h4 "Contact"]
       [:div.form-group
        [:label "Email"]
        [:input (forms/form-input state :contact/email {:type "email"})]]
       [:div.form-group
        [:label "Phone Number"]
        [:input (forms/form-input state :contact/phone)]]
       [:div.form-group
        [:a {:on-click #(swap! notes? not)}
         [:label "Notes"]]
        [:textarea (forms/form-input state :contact/notes {:style {:display (if (or @notes?
                                                                                    (not (empty? (:contact/notes @state))))
                                                                              "block"
                                                                              "none")}
                                                           :on-change #(do (reset! notes? true) nil)})]]])))

(defn person-fields [state]
  [:div
   [:h4 "Person"]
   [:div.form-group.row
    [:div.col-xs-10
     [:label "Name"]
     [:input (forms/form-input state :person/full-name)]]
    [:div.col-xs-2
     [:label "Title"]
     [:select (forms/form-input state :person/title)
      [:option ""]
      [:option "Mr"]
      [:option "Mrs"]
      [:option "Ms"]
      [:option "Miss"]
      [:option "Madam"]
      [:option "Prof"]
      [:option "Dr"]]]]
   [:div.form-group
    [:label "Phone Number"]
    [:input (forms/form-input state :person/phone)]]
   [:div.form-group
    [:label "Address"]
    [:input (forms/form-input state :person/street {:placeholder "Number and Street"})]]
   [:div.row
    [:div.col-xs-6
     [:input (forms/form-input state :person/city {:placeholder "City"})]]
    [:div.col-xs-4
     [:input (forms/form-input state :person/province {:placeholder "Province"})]]
    [:div.col-xs-2
     [:input (forms/form-input state :person/postal-code {:placeholder "Postal Code"})]]]])

(defn company-fields [state]
  [:div
   [:h4 "Company"]
   [:div.form-group
    [:label "Name"]
    [:input (forms/form-input state :company/name)]]
   [:div.form-group
    [:label "VAT Number"]
    [:input (forms/form-input state :company/vat-number)]]
   [:div.form-group
    [:label "Email"]
    [:input (forms/form-input state :company/email {:type "email"})]]])

(defn form-errors [errors]
  [:p.bg-danger
   (if @errors
     (for [error @errors]
       [:div.error error]))])

(defn customer-form [{:keys [data label on-submit]}]
  (let [errors (ratom/atom {})
        state (forms/state data)]
    (fn []
      [:form {:on-submit (forms/handle-submit state #(let [error-data (validate %)]
                                                       (if error-data
                                                         (reset! errors error-data)
                                                         (on-submit %))))}
       [contact-fields state]
       [:hr]
       [person-fields state]
       [:hr]
       [company-fields state]
       [:hr]
       [form-errors errors]
       [:input.btn.btn-primary {:type "submit" :value label}]])))

(defn customers-edit-form [{:keys [id] :as customer}]
  [:div
   [customer-form {:data customer
                   :label "Update Customer"
                   :on-submit #(do
                                 (dispatch [:update-customer %])
                                 (set-url! (routing/customers-route)))}]
   [:hr]
   [devices/device-table id]
   [:a.btn.btn-primary {:href (routing/devices-new-route {:customer-id (name id)})} "New Device"]])

(defn customers-new-form []
  [customer-form {:init-state {}
                  :label "Create Customer"
                  :on-submit #(do
                                (dispatch [:create-customer %])
                                (set-url! (routing/customers-route)))}])

(defn customers-edit-page []
  [:div
   [:p [:a {:href (routing/customers-route)} "List Customers"]]
   [loading-view customers-edit-form (subscribe [:current-customer])]])

(defn customers-new-page []
  [:div
   [:p [:a {:href (routing/customers-route)} "List Customers"]]
   [customers-new-form]])

(defn customers-list-page []
  [:div
   [customer-list-view]
   [customer-list-actions]])

(defn customers-page [section]
  (case section
    :customers      [customers-list-page]
    :customers-edit [customers-edit-page]
    :customers-new  [customers-new-page]

    [:div "Page not found"]))


;; Om inter-op

;; TODO: avoid remounting jitter, have navigation set in re-frame db

(defn customers-base [app owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [node        (om/get-node owner "mount")
            section     (:nav app)]
        (reagent/render [customers-page section] node)))

    om/IRender
    (render [_]
      (dom/div (dom/div #js {:ref "mount"})))))

(defmethod page-view :customers
  [app owner]
  (customers-base app owner))

(defmethod page-view :customers-edit
  [app owner]
  (customers-base app owner))

(defmethod page-view :customers-new
  [app owner]
  (customers-base app owner))
