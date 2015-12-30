(ns amantha.state
  (:require [amantha.grids.repairs :as headers]
            [re-frame.core :refer [register-handler dispatch-sync]]))

(def base-data
  {:nav     :repair-overview
   :grids   headers/grids
   :temp    {:histogram {}, :grid {}}
   :filters (headers/filters-for nil)})

(register-handler
  :init-app-db
  (constantly base-data))

(dispatch-sync [:init-app-db])
