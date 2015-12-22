(ns bss.rampant.utils-test
  (:require [bss.rampant.utils :as u]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(deftest group-by-deep-test
  (testing "construct hierarchies from data easilly"
    (let [base (for [i (range 2), j (range 2)] {:a i, :b j})]
      (is (= {0 [{:a 0, :b 0}, {:a 0, :b 1}], 1 [{:a 1, :b 0}, {:a 1, :b 1}]}
             (u/group-by-deep [:a] base)))
      (is (= {0 {0 [{:a 0, :b 0}], 1 [{:a 0, :b 1}]}, 1 {0 [{:a 1, :b 0}], 1 [{:a 1, :b 1}]}}
             (u/group-by-deep [:a :b] base))))))

(deftest load-fixtures-test
  (comment pending))

(deftest positions-test
  (testing "Returns all indices"
    (is (= [1 3 5 6]
           (u/positions even? [1 2 3 4 5 6 8])))))

(deftest frequencies-by-test
  (testing "Using with records"
    (is (= {12 2, 14 1, 15 1}
           (u/frequencies-by :age [{:age 12} {:age 12} {:age 14} {:age 15}]))))
  (testing "Using with primitives"
    (is (= {1 2, 0 1, -1 1}
           (u/frequencies-by #(compare % 3) [4 3 2 5])))))

(deftest merge-by-test
  (testing "Combines grouping, mapping and reducing"
    (is (= {2014 70, 2015 10}
           (u/merge-by + :year :sales [{:year 2014 :sales 20}
                                     {:year 2014 :sales 50}
                                     {:year 2015 :sales 10}])))))

(deftest titlecase-test
  (testing "covers entire sentence"
    (is (= "The Whole Enchilada"
           (u/titlecase "the whole enchilada"))))
  (testing "preserves case"
    (is (= "NATO"
           (u/titlecase "NATO"))
        (= "PLATO"
           (u/titlecase "pLATO")))))

(deftest data->csv-test
  (testing "Quote and escape only when necessary"
    (is (= "a,b,\"a,b\"\n1,[2 3],3\n\",\",dog,c"
           (u/data->csv [{:a 1, :b [2,3] (keyword "a,b") 3}
                         {:a ",", :b "dog", (keyword "a,b") \c}])))))

(deftest glyhicon-test
  (testing "both varieties"
    (is (= [:span.glyphicon.glyphicon-pancake]
           (u/glyphicon :pancake)))
    (is (= [:span.right.glyphicon.glyphicon-chinchilla]
           (u/right-glyphicon 'chinchilla)))))


;; TODO: test clj-only functions

;; set-url!
;; format-map
;; format-date-generic
;; format-date
;; read-storage
;; write-storage
;; get-local-nav
;; set-local-nav
;; strip-timestamp
