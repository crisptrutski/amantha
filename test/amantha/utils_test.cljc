(ns amantha.utils-test
  (:require
    [amantha.utils :as u]
    [amantha.components.basic :as b]
    #?(:clj [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is testing]])))

;(defn group-by-deep [ks data]
;  (if-not (seq ks)
;    data
;    (let [[k & ks'] ks]
;      (->> (group-by k data)
;           (map (fn [[idx data']] [idx (group-by-deep ks' data')]))
;           (into {})))))
;
;(deftest group-by-deep-test
;  (testing "construct hierarchies from data easilly"
;    (let [base (for [i (range 2), j (range 2)] {:a i, :b j})]
;      (is (= {0 [{:a 0, :b 0}, {:a 0, :b 1}], 1 [{:a 1, :b 0}, {:a 1, :b 1}]}
;             (u/group-by-deep [:a] base)))
;      (is (= {0 {0 [{:a 0, :b 0}], 1 [{:a 0, :b 1}]}, 1 {0 [{:a 1, :b 0}], 1 [{:a 1, :b 1}]}}
;             (u/group-by-deep [:a :b] base))))))

;(defn merge-by
;  "Similar to merge-with, reduces elements over a collection grouped by
;  key-fn and merges the value of val-fn as applied to each element with f.
;
;      (merge-by + :year :sales [{:year 2014 :sales 20}
;                                {:year 2014 :sales 50}
;                                {:year 2015 :sales 10}])
;      => {2014 70, 2015 10}"
;  [f key-fn val-fn coll]
;  (persistent!
;    (reduce (fn [acc x]
;              (let [v (key-fn x)]
;                (assoc! acc v (f (get acc v 0) (val-fn x)))))
;            (transient {}) coll)))
;
;(deftest merge-by-test
;  (testing "Combines grouping, mapping and reducing"
;    (is (= {2014 70, 2015 10}
;           (u/merge-by + :year :sales [{:year 2014 :sales 20}
;                                     {:year 2014 :sales 50}
;                                     {:year 2015 :sales 10}])))))
;
;(defn frequencies-by
;  "Returns a map from distinct items in coll to the number of times they appear."
;  [f coll]
;  (persistent!
;    (reduce (fn [counts x]
;              (let [v (f x)]
;                (assoc! counts v (inc (get counts v 0)))))
;            (transient {}) coll)))
;
;(deftest frequencies-by-test
;  (testing "Using with records"
;    (is (= {12 2, 14 1, 15 1}
;           (u/frequencies-by :age [{:age 12} {:age 12} {:age 14} {:age 15}]))))
;  (testing "Using with primitives"
;    (is (= {1 2, 0 1, -1 1}
;           (u/frequencies-by #(compare % 3) [4 3 2 5])))))

(deftest positions-test
  (testing "Returns all indices"
    (is (= [1 3 5 6]
           (u/positions even? [1 2 3 4 5 6 8])))))

(deftest safe-name-test
  (testing "usual suspects"
    (is (= "abc" (u/safe-name "abc")))
    (is (= "abc" (u/safe-name 'abc)))
    (is (= "abc" (u/safe-name :abc))))
  (testing "less usual"
    (is (= "Sir Thingy" (u/safe-name (reify Object (toString [_] "Sir Thingy")))))))

(deftest titlecase-test
  (testing "covers entire sentence"
    (is (= "The Whole Enchilada"
           (u/titlecase "the whole enchilada"))))
  (testing "respects whitespace"
    (is (= "AT  Bat\t  \n Cat"
           (u/titlecase "aT  bat\t  \n cat"))))
  (testing "preserves case"
    (is (= "NATO"
           (u/titlecase "NATO"))
        (= "PLATO"
           (u/titlecase "pLATO")))))

(deftest format-currency-test
  #?(:cljs
     (is (= "R 1,234,567.89" (u/format-currency :amount 1234567.8910)))))

(deftest glyphicon-test
  (testing "both varieties"
    (is (= [:span.glyphicon.glyphicon-pancake]
           (b/glyphicon :pancake)))
    (is (= [:span.right.glyphicon.glyphicon-chinchilla]
           (b/right-glyphicon 'chinchilla)))))
