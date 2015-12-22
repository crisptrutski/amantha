(ns amantha.config-test
  (:require [amantha.config :refer :all]
            [clojure.test :refer :all]
            [environ.core :refer [env]])
  (:import [java.net ServerSocket]))

;; helpers

(defmacro with-ns [ns & body]
  `(binding [*ns* '~ns]
     ~@body))

;; tests

(deftest get-free-port!-test
  (testing "port is really free"
    (let [port (get-free-port!)]
      (is (number? port))
      (is (nil? (let [s (ServerSocket. port)]
                  ;; has not thrown
                  (.close s)))))))

(deftest guess-host-name-test
  (testing "best effort for portable..."
    (is (string? (guess-host-name)))))

;(deftest version-test
  ;(testing "just check it looks good"
    ;(is (re-matches #"\d+\.\d+\.\d+(-\w*)?" version))))

(deftest app-name-test
  (testing "flexible but stupid"
    (is (= "zip" (with-ns zip (app-name))))
    (is (= "bss.regular" (with-ns bss.regular (app-name))))
    (is (= "zip.zap" (with-ns zip.zap.rank.ronk (app-name))))))

(deftest gen-conf-test
  (testing "put it all together"
    (is (= {;; :version      version
            :host         (guess-host-name)
            :service-name "amantha"
            :lein-version (:lein-version env) ;; "2.5.1"
            :port         231}
           (gen-conf
            ;; defaults
            {;;:version      "not used"
             :port         231}
            ;; env whitelist
            [:lein-version]
            ;; is-dev?
            false
            ;; int-keys
            [:port]))))

  (testing "unparseable int-keys are removed"
    (is (not (contains?
               (gen-conf {}
                         [:lein-version]
                         false
                         [:lein-version])
               :lein-version))))

  (testing "unparesable int-keys may fall back to default"
    (is (= "2.5.5"
           (:lein-version (gen-conf {:lein-version "2.5.5"}
                                    [:lein-version]
                                    false
                                    [:lein-version])))))

  (testing "ensures there's a port"
    (is (:port (gen-conf {} [])))))

(deftest override-test
  (testing "only zaps where non nil"
    (is (= {:a 3, :b 6, :c 4, :d nil}
           (override {:a 1, :c 4, :d nil}
                     {:a 3, :b 6, :c nil})))))

(deftest parse-ints-test
  (let [test-map {:a "5" :b 3 :c nil :d "" :e "str"}]
    (is (= test-map
           (parse-ints [] test-map))
        "empty key list doesn't change things")
    (is (= test-map
           (parse-ints nil test-map))
        "nil key list doesn't change things")
    (is (= test-map
           (parse-ints [:f] test-map))
        "keys not present in the map at all have no effect")
    (is (= {:a 5 :b 3}
           (parse-ints (keys test-map) test-map))
        "removes all non-ints")
    (is (= {:a 5 :b 3 :e "str"}
           (parse-ints [:a :b :c :d] test-map))
        "intermediate case: parse some, remove some, leave some")))
