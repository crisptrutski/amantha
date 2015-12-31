(ns amantha.grids.filters-test
  (:require [amantha.grids.filters :as f]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is]])))

(deftest combine-filters-test
  (is (= [[:a 1]
          [:a 2]
          [:a 3]
          [:a 4]
          [:b 5]]
         (f/combine-filters [{:key :a, :filters [1 2 3]}
                             {:key :a, :filters [4]}
                             {:key :b, :filters [5]}]))))
