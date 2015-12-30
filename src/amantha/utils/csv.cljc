(ns amantha.utils.csv
  (:require
    [clojure.string :as str]
    #?(:clj [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest testing is]])))

(defn- escape-char? [s]
  (some #{\, \" \return \newline} s))

(defn- escape-char [s]
  (str \"
       (str/escape s {\"       "\\\""
                      \return  "\\r"
                      \newline "\\n"})
       \"))

(defn- escape [s]
  (let [s (str s)]
    (if (escape-char? s) (escape-char s) s)))

(defn data->csv [data]
  (let [fields (keys (first data))]
    (str/join "\n"
              (cons
                (str/join "," (map (comp escape name) fields))
                (for [row data]
                  (str/join "," (map #(escape (get row %)) fields)))))))

;;

(deftest data->csv-test
  (testing "Quote and escape only when necessary"
    (is (= "a,b,\"a,b\"\n1,[2 3],3\n\",\",dog,c"
           (data->csv [{:a 1, :b [2, 3] (keyword "a,b") 3}
                       {:a ",", :b "dog", (keyword "a,b") \c}])))))
