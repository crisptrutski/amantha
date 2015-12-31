(ns amantha.grids.repairs
  (:require [amantha.utils :refer [format-currency format-date]]
            [amantha.components.basic :as b]
            [goog.string]
            [goog.string.format]
            [cljs.core.async :refer [chan]]))

(def id identity)

(defn full-name [e]
  (str (:first-name e) " " (:last-name e)))

(def grids
  {:payments {:data-key  [:payments]
              :chart-key :custom-payments-chart
              :sort-keys [:id :created-at]
              :headers   [["Date" :created-at format-date]
                          ["Branch" :branch id]
                          ["Payment Number" :id]
                          ["Method" :method id]
                          ["Amount" :amount (comp b/right-align #(format-currency :amount %))]]}

   :clients  {:data-key  [:clients]
              :hist-key  nil
              :sort-keys [:id]
              :headers   [["Date" :created-at format-date]
                          ["Branch" :branch id] ["Number" :id id]
                          ["Name" full-name id [:first-name :last-name]]
                          ["Company" :company id]
                          ["Email" :email id]
                          ["Phone" :phone id]]}

   :ratings  {:data-key  [:ratings]
              :hist-key  :created-at
              :sort-keys [:CREATED-AT :id]
              :headers   [["Date" :created-at format-date]
                          ["Branch" :branch id]
                          ["Name" full-name id [:first-name :last-name]]
                          ["Rating" :rating b/label]
                          ["Job" :job-id (partial b/link-to #(str "#/job/" %))]
                          ["Comments" :comments id]]}

   :users    {:data-key  [:users]
              :hist-key  :created-at
              :sort-keys [:id]
              :headers   [["Date" :created-at format-date]
                          ["Branch" :branch id]
                          ["Name" full-name id [:first-name :last-name]]
                          ["Display Name" :display-name id]
                          ["Email" :email (comp #(.toLowerCase %) str)]
                          ["Phone" :phone id]
                          ["Active?" :is-active (comp b/tick-view boolean)]]}})

(defn filters-for [_]
  {:payments [{:key :created-at, :label "Date", :type :date-range}
              {:key     :method, :label "Method", :type :multi-select,
               :options ["Credit Card" "Cash" "Bank Deposit" "iSureFix" "SnapScan" "Santam"]}]
   :clients  [{:key :created-at, :label "Date", :type :date-range}
              {:key :id, :label "ID", :type :full-text-search}
              {:key :first-name, :label "First Name", :type :full-text-search}
              {:key :last-name, :label "Last Name", :type :full-text-search}
              {:key :company, :label "Company", :type :full-text-search}
              {:key :email, :label "Email", :type :full-text-search}
              {:key :phone, :label "Phone", :type :full-text-search}]
   :ratings  [{:key :created-at, :label "Date", :type :date-range}
              {:key :first-name, :label "First Name", :type :full-text-search}
              {:key :last-name, :label "Last Name", :type :full-text-search}
              {:key     :rating, :label "Rating", :type :multi-select
               :options ["Awesome" "Average" "Bad"]}]
   :users    [{:key :created-at, :label "Date", :type :date-range}
              {:key :display-name, :label "Display Name", :type :full-text-search}
              {:key :first-name, :label "First Name", :type :full-text-search}
              {:key :last-name, :label "Last Name", :type :full-text-search}
              {:key :email, :label "Email", :type :full-text-search}
              {:key :phone, :label "Phone", :type :full-text-search}]})
