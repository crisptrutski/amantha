(ns amantha.utils.roman)

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

(defn from-number [num]
  (if-not (pos? num)
    "--"
    (letfn [(rn [r o]
              (if (= r 0)
                o
                (let [d (first (filter #(<= (first %) r) roman-vals))]
                  (rn (- r (first d)) (str o (second d))))))]
      (rn num ""))))

