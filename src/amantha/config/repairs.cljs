(ns amantha.config.repairs
  (:require [amantha.utils :refer [format-currency]]
            [amantha.utils :as utils :refer [format-date]]
            [clojure.string :as str]
            [goog.string]
            [goog.string.format]
            [cljs.core.async :refer [chan]]))

(def id identity)

(defn label [x] [:span.label.label-default x])

(defn labels
  ([options colors x]
   (let [idx (mod (first (utils/positions #{x} options)) 7)]
     [:span.label.label-defaul {:style {:background-color (nth colors idx)}} x]))
  ([options x]
   (let [idx (inc (mod (first (utils/positions #{x} options)) 7))]
     [:span.label.label-defaul {:class (str "bg-col" idx)} x])))

(defn link-to [url-fn x]
  [:a {:href (url-fn x)} x])

(def full-stars (take 5 (repeat (utils/glyphicon "star"))))

(def empty-stars (take 5 (repeat (utils/glyphicon "star-empty"))))

(defn points-render [num]
  (let [num   (if (number? num) num (js/parseInt num))
        full  (take num full-stars)
        empty (take (- 3 num) empty-stars)
        stars (concat full empty)]
    (into [:span] (interpose " " stars))))

(defn right-align [content]
  [:div {:style {:width "100%" :text-align "right" :padding-right 10}}
   content])

(defn full-name [e]
  (str (:first-name e) " " (:last-name e)))

(defn tick-view [tick?]
  (utils/glyphicon (if tick? :ok :remove)))

(defn strip-branch
  "For branch-prefixed strings, strip the branch"
  [value]
  (let [chunks (str/split value #"-")]
    (if (> (count chunks) 1)
      (str/join "-" (rest chunks))
      (first chunks))))

(def grids
  {:payments {:data-key  [:payments]
              :chart-key :custom-payments-chart
              :sort-keys [:id :created-at]
              :headers   [["Date" :created-at format-date]
                          ["Branch" :branch id]
                          ["Payment Number" :id strip-branch]
                          ["Method" :method id]
                          ["Amount" :amount (comp right-align #(format-currency :amount %))]]}

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
                          ["Rating" :rating label]
                          ["Job" :job-id (partial link-to #(str "#/job/" %))]
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
                          ["Active?" :is-active (comp tick-view boolean)]]}})


(defn filters-for [user]
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
