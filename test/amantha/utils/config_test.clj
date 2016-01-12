(ns amantha.utils.config-test
  (:require
    [amantha.utils.config :refer :all]
    [amantha.utils.server :as server :refer [get-free-port! guess-host-name]]
    [clojure.test :refer :all]
    [environ.core :refer [env]])
  (:import
    [java.net ServerSocket]))

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

(deftest app-name-test
  (testing "matches up to 2 segments from calling namespace"
    (is (= "zip" (with-ns zip (app-name))))
    (is (= "bss.regular" (with-ns bss.regular (app-name))))
    (is (= "zip.zap" (with-ns zip.zap.rank.ronk (app-name))))))

(deftest gen-conf-test
  (testing "put it all together"
    (let [env (assoc env :lein-version "2.5.1")]
      (is (= {:host         (guess-host-name)
              :service-name "amantha.utils"
              :lein-version (:lein-version env)
              :port         231}
             (gen-conf
               ;; defaults
               {:host (guess-host-name)
                :port 231}
               ;; env
               env
               ;; env whitelist
               [:lein-version])))))

  (testing "ensures there's a port"
    (is (:port (server/ensure-port (gen-conf {} []))))))

(deftest override-test
  (testing "only zaps where non nil"
    (is (= {:a 3, :b 6, :c 4, :d nil}
           (override {:a 0, :c 4, :d nil}
                     {:a 3, :b 6, :c nil})))))
