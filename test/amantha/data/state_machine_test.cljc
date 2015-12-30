(ns amantha.data.state-machine-test
  (:require
    [amantha.data.state-machine :as sm]
    #?(:clj [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is testing]])))

;; sufficient machine

(def spec
  [{:id      :state/a
    :label   "Core 1"
    :type    :central
    :actions {:action/do-1 {:label      "Continue"
                            :type       :next
                            :effects    [:effect-1, :effect-2]
                            :target     :state/b}
              :action/fail {:label      "Give up"
                            :type       :other
                            :effects    [:effect-3]
                            :target     :state/d}}}
   {:id      :state/b
    :label   "Core 2"
    :type    :central
    :actions {:action/undo {:label      "Back"
                            :type       :undo
                            :target     :state/a}
              :action/do-2 {:label      "Continue"
                            :type       :next
                            :validation (constantly false)
                            :effects    [:effect-2 :effect-3]
                            :target     :state/c}}}
   {:id      :state/c
    :type    :central
    :label   "Core 3"}
   {:id      :state/d
    :label   "Failure"
    :actions {:action/undo {:label      "Retry"
                            :type       :undo
                            :target     :state/a}}}])

;; edge case machines

(def ambiguous
  [{:id 1, :actions {0 {:target 2}
                     1 {:target 2}}}])

(def out-of-order
  [{:id 1, :type :central}
   {:id 2, :type :central, :actions {:next {:target 1}}}])

(def ambiguous-out-of-order
  [{:id 1, :type :central}
   {:id 2, :type :central, :actions {:next {:target 1}
                                     :also {:target 1}}}])

;; tests

(deftest valid?-test
  (is (sm/valid? spec))
  (is (not (sm/valid? ambiguous)))
  (is (not (sm/valid? out-of-order)))
  (is (not (sm/valid? ambiguous-out-of-order))))

(deftest errors-test
  (is (empty? (sm/errors spec)))

  (is (= #{:ambiguous-routes}     (sm/errors ambiguous)))
  (is (= #{:central-out-of-order} (sm/errors out-of-order)))
  (is (= #{:ambiguous-routes
           :central-out-of-order} (sm/errors ambiguous-out-of-order))))

(deftest find-action-test
  (is (= :action/do-1 (sm/find-action spec :state/a :state/b)))
  (is (= :action/undo (sm/find-action spec :state/b :state/a)))
  (is (nil? (sm/find-action spec :state/c :state/b))))

(deftest labelled-actions-test
  (is (= [{:label "Continue", :type :next,  :action :action/do-1}
          {:label "Give up",  :type :other, :action :action/fail}]
         (sm/labelled-actions spec :state/a)))

  (is (= [{:label "Back",     :type :undo, :action :action/undo}
          {:label "Continue", :type :next, :action :action/do-2}]
         (sm/labelled-actions spec :state/b))))

(deftest destination-test
  (is (= :state/b (sm/destination spec :state/a :action/do-1)))
  (is (= :state/d (sm/destination spec :state/a :action/fail)))
  (is (= :state/a (sm/destination spec :state/b :action/undo)))
  (is (= :state/c (sm/destination spec :state/b :action/do-2)))
  (is (nil? (sm/destination spec :state/c :action/do-3)))
  (is (= :state/a (sm/destination spec :state/d :action/undo))))

(deftest effects-test
  (is (= [:effect-1 :effect-2] (sm/effects spec :state/a :action/do-1)))
  (is (= [:effect-3] (sm/effects spec :state/a :action/fail)))
  (is (empty? (sm/effects spec :state/b :action/undo)))
  (is (= [:effect-2 :effect-3] (sm/effects spec :state/b :action/do-2)))
  (is (nil? (sm/effects spec :state/c :action/do-3)))
  (is (nil? (sm/effects spec :state/d :action/undo))))

(deftest multi-group-by-test
  (testing "Without value-fn"
    (is (= {1 [{:a [1]} {:a [1 2]}]
            2 [{:a [2]} {:a [1 2]}]}
           (sm/multi-group-by :a [{:a [1 2]}
                                  {:a [1]}
                                  {:a [2]}]))))
  (testing "With value-fn"
    (is (= {1 [[1] [1 2]]
            2 [[2] [1 2]]}
           (sm/multi-group-by :a [{:a [1 2]}
                                  {:a [1]}
                                  {:a [2]}] :a)))))

(deftest build-test
  (is (= [{:id      :a
           :actions {:c {:sources    [:a :b]
                         :target     :c
                         :validation identity}}}
          {:id      :b
           :actions {:c {:sources    [:a :b]
                         :target     :c
                         :validation identity}
                     :d {:sources    [:b :c]
                         :target     :a
                         :validation juxt}}}
          {:id :c
           :actions {:d {:sources    [:b :c]
                         :target     :a
                         :validation juxt}}}]
         (sm/build [{:id :a}, {:id :b}, {:id :c}]
                   {:c {:sources [:a :b], :target :c, :validation ::a}
                    :d {:sources [:b :c], :target :a, :validation ::b}}
                   {::a identity
                    ::b juxt}))))

(defn build-eg [status]
  (sm/entity-state-machine
   :repair
   [{:id      :a
     :actions {:c {:sources    [:a :b]
                   :target     :c
                   :effects    [:x :y]
                   :validation identity}}}
    {:id      :b
     :actions {:c {:sources    [:a :b]
                   :target     :c
                   :effects    [:x :y]
                   :implicit   true
                   :validation identity}
               :d {:sources    [:b :c]
                   :target     :a
                   :effects    [:y :z]
                   :validation juxt}}}
    {:id :c
     :actions {:d {:sources    [:b :c]
                   :effects    [:y :z]
                   :target     :a
                   :validation juxt}}}]
   {:db ::db
    :repair {:id 3 :status status}}))

(deftest process-action-test
  (is (nil? (sm/process-action (build-eg :a) :a)))

  (is (= [[:x 3] [:y 3] [:update-repair-status 3 :c]]
         (sm/process-action (build-eg :a) :c)))
  (is (= [[:x 3] [:y 3] [:update-repair-status 3 :c]]
         (sm/process-action (build-eg :b) :c)))
  (is (nil? (sm/process-action (build-eg :c) :c)))

  (is (nil? (sm/process-action (build-eg :a) :d)))
  (is (= [[:x 3] [:y 3] [:update-repair-status 3 :c]]
         (sm/process-action (build-eg :b) :d)))
  (is (= [[:y 3] [:z 3] [:update-repair-status 3 :a]]
         (sm/process-action (build-eg :c) :d))))
