(ns bss.rampant.pagination-test
  (:require [bss.rampant.pagination :as page]
            [bss.rampant.sorting :refer [sort-by-keys]]
            #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(def ^:private test-data
  [{:id 5 :name "Chris" :age 29 :timestamp #inst "2014-01-01Z"}
   {:id 3 :name "Petrus" :age 27 :timestamp #inst "2014-01-08Z"}
   {:id 1 :name "John" :age 27 :timestamp #inst "2014-01-07Z"}
   {:id 2 :name "Jake" :age 7 :timestamp #inst "2014-01-03Z"}
   {:id 25 :name "Hake" :age 14 :timestamp #inst "2014-01-03Z"}
   {:id 17 :name "Mary" :age 14 :timestamp #inst "2014-01-04Z"}
   {:id 29 :name "Alice" :age 6 :timestamp #inst "2014-01-01Z"}
   {:id 64 :name "Bob" :age 40 :timestamp #inst "2014-01-05Z"}
   {:id 12 :name "Grace" :age 35 :timestamp #inst "2014-01-01Z"}
   {:id 9 :name "Sxymum" :age 19 :timestamp #inst "2014-01-05Z"}
   {:id 31 :name "Cooldad" :age 31 :timestamp #inst "2014-01-10Z"}])

(def ^:private updated-test-data
  (-> test-data
      (conj {:id 101 :name "Aaron"})
      (conj {:id 666 :name "Frank"})
      (conj {:id 457 :name "Billy Bob"})))

(defn- hashify [key xs]
  (map #(hash-map key %) xs))

;;

(deftest parse-test
  (testing "first"
    (is (instance? bss.rampant.pagination.FirstPage (page/parse [:open])))
    (is (instance? bss.rampant.pagination.FirstPage (page/parse [:first]))))

  (testing "last"
    (is (instance? bss.rampant.pagination.LastPage (page/parse [:last]))))

  (testing "before"
    (let [before [:before [32] false]
          parsed (page/parse before)]
      (is (instance? bss.rampant.pagination.PageBefore parsed))
      (is (= [32] (:value parsed)))
      (is (= false (:inclusive parsed)))))

  (testing "after"
    (let [after  [:after ["a" 3] true]
          parsed (page/parse after)]
      (is (instance? bss.rampant.pagination.PageAfter parsed))
      (is (= ["a" 3] (:value parsed)))
      (is (= true (:inclusive parsed)))))

  (testing "range"
    (let [range  [:range ["a"] ["g"]]
          parsed (page/parse range)]
      (is (instance? bss.rampant.pagination.PageRange parsed))
      (is (= ["a"] (:from parsed)))
      (is (= ["g"] (:to parsed))))))

(deftest serialize-test
  (let [cases [#_[:open]
               [:first]
               [:last]
               [:before [32] false]
               [:after ["a" 3] true]
               [:range ["a"] ["g"]]]]
    (doseq [case cases]
      (is (= case (page/serialize (page/parse case)))))))

(def next-map
  {[:before [32] false]   [:after [32] true]
   [:before ["a" 3] true] [:after ["a" 3] false]})

(def next* (comp page/serialize page/next page/parse))
(def prev* (comp page/serialize page/prev page/parse))

(deftest next-test
  (is (= [:after ["c"] false] (next* [:range ["a"] ["c"]])))
  (doseq [[in out] next-map] (is (= out (next* in)))))

(deftest prev-test
  (is (= [:before ["a"] false] (prev* [:range ["a"] ["c"]])))
  (doseq [[in out] next-map] (is (= in (prev* out)))))

(deftest next-prev-count-test
  (let [data (hashify :z (vec (range 1 11)))]
    (is (= 3 (page/next-count (page/parse [:range [3] [7]]) data [:z])))
    (is (= 2 (page/prev-count (page/parse [:range [3] [7]]) data [:z])))))

(deftest refine-test

  (testing "can refine empty results"
    (is (= (page/parse [:first])
           (page/refine (page/parse [:range [1] [3]]) [] [:a] 3))))

  (is (= (page/parse [:range [0] [2]])
         (page/refine (page/parse [:open]) (hashify :a (range 10)) [:a] 3)))

  (is (= [:range ["John" 1] ["Petrus" 3]]
         (page/serialize
          (page/refine (page/next (page/parse [:before ["Jake" 2] true]))
                       test-data
                       [:name :id]
                       3))))

  (is (= [:range ["Hake" 25] ["John" 1]]
         (page/serialize
          (page/refine (page/prev
                        (page/parse [:after ["Mary" 17] true]))
                       test-data [:name :id]
                       3))))

  ;; Skip to last page
  (is (= [:range ["Mary" 17] ["Sxymum" 9]]
         (page/serialize
          (page/refine (page/parse [:last])
                       test-data
                       [:name :id]
                       3))))

  ;; What happens when data changes and navigate forwards?
  (is (= [:range ["Cooldad" 31] ["Grace" 12]]
         (page/serialize
          (page/refine
           (page/next
            (page/parse [:range["Alice" 29] ["Chris" 5]]))
           updated-test-data [:name :id]
           3))))

  ;; What happens when data changes and navigate backwads?
  (is (= [:range ["Aaron" 101] ["Alice" 29]]
         (page/serialize
          (page/refine
           (page/prev
            (page/parse [:range ["Billy Bob" 457] ["Chris" 5]]))
           updated-test-data
           [:name :id]
           3))))

  ;; What happens when data changes and page refreshes?
  (is (= [:range ["Alice" 29] ["Bob" 64]]
         (page/serialize
          (page/refine
           (page/parse [:range ["Alice" 29] ["Chris" 5]])
           updated-test-data [:name :id] 3)))))

(deftest next-prev-refine-test
  (testing "Integration between all the navigation pieces"
    (is (= [:range [4] [9]]
           (-> (page/parse [:open])
               ;; unrestricted can only refine, and takes until limit from start
               (page/refine (hashify :a [1 2 3 4]) [:a] 3)
               (page/next)
               (page/prev)
               (page/next)
               (page/refine (hashify :a [3 4 5 6 7 8]) [:a] 3)
               (page/next)
               (page/prev)
               (page/refine (hashify :a [3 4 5 6 7 8 9]) [:a] 3)
               (page/refine (hashify :a [2 4 7 9]) [:a] 3)
               (page/serialize))))))

(defn paginated-names [sort-keys expr]
  (let [pagination (page/parse expr)]
    (map :name (last (page/paginate-and-sort pagination test-data sort-keys 3)))))

(deftest paginated-names-test
  (is (= ["Alice" "Bob" "Chris"]
         (paginated-names [:name :id] [:open])))

  (is (= ["Alice"]
         (paginated-names [:name :id] [:before ["Bob" 64] false])))

  (is (= ["John" "Mary" "Petrus"]
         (paginated-names [:name :id] [:after ["Jake" 2] false])))

  ;; Note - not limiting to the right
  (is (= ["Mary" "Hake" "Sxymum"]
         (paginated-names [:age :id] [:range [14 17] [14 25]]))))
