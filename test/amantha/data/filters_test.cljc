(ns amantha.data.filters-test
  (:require
    [amantha.data.filters]
    [amantha.grids.filters :refer [filter-once filter-data]]
    [clojure.string :as str]
    #?(:clj [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is testing]])))

(def ^:private str-rev (comp str/join reverse))

(def ^:private boom! #(throw %))

(deftest filter-once-test
  (testing ":default"
    (is (= [:unchanged] (filter-once [:unchanged] [:ignored [:unknown]]))))

  (testing ":include-string"
    (is (= [:no-match]
           (filter-once [:no-match]
                        [boom! [:include-string]])))
    (is (= ["dniw" "swolliw"]
           (filter-once (map str-rev ["the" "wind" "in" "the" "willows"])
                        [str-rev [:include-string "^w"]])))
    (is (= [{:a "willows"}]
           (filter-once (map #(hash-map :a %) ["the" "wind" "in" "the" "willows"])
                        [:a [:include-string "w" "^%sil"]])))
    (is (= []
           (filter-once ["the" "wind" "in" "the" "willows"]
                        [identity [:include-string "^z"]]))))

  (testing ":date-range"
    (is (= [{:a #inst "2014-01-01"} {:a #inst "2014-04-09"}]
           (filter-once [{:a #inst "2013-01-01"}
                         {:a #inst "2014-01-01"}
                         {:a #inst "2014-04-09"}
                         {:a #inst "2015-04-09"}]
                        [:a [:date-range #inst "2014-01-01" #inst "2014-12-31"]]))))

  (testing ":equal"
    (is (= [:no-value] (filter-once [:no-value] [boom! [:equal]])))
    (is (= [\a \A] (filter-once "abcBAC" [(comp keyword #(.toLowerCase %) str) [:equal :a]]))))

  (testing ":>="
    (is (= [:no-num] (filter-once [:no-num] [boom! [:>=]])))
    (is (= [2 3 4] (filter-once (range 5) [inc [:>= "3"]]))))

  (testing ":<="
    (is (= [:no-num] (filter-once [:no-num] [boom! [:<=]])))
    (is (= [0 1 2] (filter-once (range 5) [inc [:<= 3]])))))

(testing ":any-of"
  (let [sample-data [{:id "1" :branch "Cape Town"}
                     {:id "2" :branch "Canal Walk"}
                     {:id "3" :branch "Stellenbosch"}
                     {:id "4" :branch "Cape Town"}
                     {:id "5" :branch "Stellenbosch"}
                     {:id "6" :branch "Stellenbosch"}]
        id-list     (partial map :id)
        all-ids     (id-list sample-data)]
    (is (= all-ids
           (id-list (filter-once sample-data [:branch [:any-of nil]]))))
    (is (= ["1" "4"]
           (id-list (filter-once sample-data [:branch [:any-of ["Cape Town"]]]))))
    (is (= ["2"]
           (id-list (filter-once sample-data [:branch [:any-of ["Canal Walk"]]]))))
    (is (= ["3" "5" "6"]
           (id-list (filter-once sample-data [:branch [:any-of ["Stellenbosch"]]]))))
    (is (= ["1" "3" "4" "5" "6"]
           (id-list (filter-once sample-data [:branch [:any-of ["Cape Town" "Stellenbosch"]]]))))
    (is (= ["2" "3" "5" "6"]
           (id-list (filter-once sample-data [:branch [:any-of ["Canal Walk" "Stellenbosch"]]]))))
    (is (= all-ids
           (id-list (filter-once sample-data [:branch [:any-of ["Cape Town" "Canal Walk" "Stellenbosch"]]]))))))

(deftest filter-data-test
  (is (= [{:a 1, :b "cat", :c :++}
          {:a 2, :b "rat", :c :++}]
         (filter-data
           [{:a 1, :b "cat", :c :++}
            {:a 2, :b "cot", :c :++}
            {:a 2, :b "rat", :c :++}
            {:a 3, :b "bat", :c :--}
            {:a 4, :b "hit", :c :++}
            {:a 5, :b "cut", :c :--}]
           [{:key :a, :filters [[:<= 3]]}
            {:key :b, :filters [[:include-string "a"]]}
            {:key :c, :filters [[:equal :++]]}]))))
