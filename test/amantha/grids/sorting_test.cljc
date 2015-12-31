(ns amantha.grids.sorting-test
  (:require [amantha.grids.sorting :as s]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(deftest match-sort-keys-test
  (is (s/match-sort-keys "A" "a" :A :a))
  (is (not (s/match-sort-keys "a" "b"))))

(deftest toggle-keyword-case-test
  (is (= :ABC (s/toggle-keyword-case :abc)))
  (is (= :abc (s/toggle-keyword-case :ABC)))
  ;; Note: here as a warning, we do not depend on this behaviour
  (is (= :abc (s/toggle-keyword-case :aBC)))
  (is (= :abc (s/toggle-keyword-case :Abc))))

(deftest asc?-test
  (is (s/asc? :a))
  (is (not (s/asc? :A))))

(deftest desc?-test
  (is (not (s/desc? :a)))
  (is (s/desc? :A)))

;; real API

(deftest sort-by-keys-test
  (is (= [{:a 1, :b 1, :c 2}
          {:a 1, :b 2, :c 2}
          {:a 1, :b 1, :c 1}
          {:a 1, :b 2, :c 1}
          {:a 2, :b 1, :c 2}
          {:a 2, :b 2, :c 2}
          {:a 2, :b 1, :c 1}
          {:a 2, :b 2, :c 1}
          {:a 3, :b 1, :c 2}
          {:a 3, :b 2, :c 2}
          {:a 3, :b 1, :c 1}
          {:a 3, :b 2, :c 1}]
         (s/sort-by-keys [:a :C :b]
                         (for [a (range 1 4)
                               b (range 1 3)
                               c (range 1 3)]
                           {:a a, :b b, :c c})))))

(deftest update-sort-keys-test
  (testing "evolution"
    (is (= [:a :id]
           (s/update-sort-keys [:id] :a)))
    (is (= [:id :a]
           (s/update-sort-keys [:a :id] :id)))
    (is (= [:A :id]
           (s/update-sort-keys [:a :id] :a)))
    (is (= [:b :A :id]
           (s/update-sort-keys [:A :id] :b))))
  (testing "with pairs"
    (is (= [:a :b :id]
           (s/update-sort-keys [:id] [:a :b])))
    (is (= [:id :b :a]
           (s/update-sort-keys [:a :id] [:id :b])))
    (is (= [:A :ID]
           (s/update-sort-keys [:a :id] [:a :id])))
    (is (= [:id :a]
           (s/update-sort-keys [:A :id] [:id :a])))))
