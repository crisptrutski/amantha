(ns amantha-example.roman)

(def roman-vals
  (into (sorted-map-by >)
        {1000 "M"
         500  "D"
         350  "LC"
         100  "C"
         50   "L"
         40   "XL"
         10   "X"
         5    "V"
         4    "IV"
         1    "I"
         9    "IX"
         900  "CM"
         90   "XC"}))

(defn greedy-pair
  "Find the largest [value symbol] pair <= num"
  [num]
  (first (filter (comp (partial >= num) first) roman-vals)))

(defn from-number [num]
  (if-not (pos? num)
    "--"
    (loop [r num o ""]
      (if-not (pos? r) o
        (let [[val sym] (greedy-pair r)]
          (recur (- r val) (str o sym)))))))
