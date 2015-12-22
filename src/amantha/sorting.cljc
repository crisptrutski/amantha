(ns amantha.sorting)

;; generic
;; ----------------------------

(defn update-opt-list*
  "Given a list of keys, and a new key
   If value `match?`-es head of list, `toggle` the value
   Otherwise place key at front of list, and remove all other `match?`-es."
  [existing-keys keys toggle match?]
  (let [is-key?       #(some (partial match? %) keys)
        num           (count keys)
        head          (take num existing-keys)
        tail          (drop num existing-keys)
        filtered-tail (remove is-key? existing-keys)]
    (if (every? identity (map match? keys head))
      (concat (map toggle head) filtered-tail)
      (concat keys filtered-tail))))

(defn update-opt-list
  "Update list of keys to include new key or vector of keys.
   If a vector, apply as regular set of updates from right to left.
   See `update-opt-list` to understand what update for a single key looks like."
  [existing-keys key-s toggle match?]
  (let [keys (if (keyword? key-s) [key-s] key-s)]
    (update-opt-list* existing-keys keys toggle match?)))

;; helpers
;; ----------------------------

(defn match-sort-keys
  "Relaxed equality check to ignore case"
  [& keys]
  (apply = (map #(.toLowerCase (name %)) keys)))

(defn toggle-case
  "Uppercase to lowercase, and vice versa"
  [s]
  (let [lc (.toLowerCase s)]
    (if (= s lc) (.toUpperCase s) lc)))

(def toggle-keyword-case (comp keyword toggle-case name))

(defn asc?
  "Interpret :ascending and :DESCENDING convention based on keyword case"
  [key]
  (let [n (name key)]
    (= (.toLowerCase n) n)))

(def desc? (comp not asc?))

(defn ->lower
  "Normalize keyword to lowercase (eg. for lookup)"
  [k]
  (keyword (.toLowerCase (name k))))

(defn sym->compare
  "Map :UPPER or :lower keys to associated comparator (see asc? and desc?)"
  [sym]
  (if (asc? sym) compare (comp - compare)))

(defn sort-by-key
  "Sort collection of maps according to particular cased-key"
  [rows sort-key]
  (sort-by (->lower sort-key)
           (sym->compare sort-key)
           rows))

(defn compare-keys
  "Build single-step comparison function over multiple cased-keys"
  [sort-keys]
  #(or (first
         (remove zero?
                 (map (fn [key a b]( (sym->compare key) a b))
                      sort-keys %1 %2)))
       0))

;; API
;; ----------------------------

(defn sort-by-keys
  "Sort collection of maps according to list of cased-keys in priority"
  [keys rows]
  (reduce sort-by-key rows (reverse keys)))

(defn update-sort-keys
  "Update sort-keys in response to new key.
   If key is highest priority toggle asc/desc, otherwise set key as highest priority"
  [sort-keys key_s]
  (update-opt-list sort-keys key_s toggle-keyword-case match-sort-keys))
