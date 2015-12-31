(ns amantha.grids.pagination
  (:refer-clojure :exclude [next])
  (:require
    [amantha.utils :refer [p p*]]
    [amantha.grids.sorting :refer [->lower compare-keys sort-by-keys]]))

;; (def fast-sort (memoize sort-by-keys))
(def fast-sort sort-by-keys)

(declare ->PageAfter)

;; NOTE: Probably should absorb sort-keys

(defprotocol IPagination
  (next [_])
  (prev [_])
  (next-count [_ data sort-keys])
  (prev-count [_ data sort-keys])
  (serialize [_])
  (paginate [_ data sort-keys limit]))

(defn- -paginate [drop? data sort-keys limit & [flip]]
  (as-> data %
    (fast-sort sort-keys %)
    (if drop? (remove drop? %) %)
    ((if flip take-last take) limit %)))

(defn- -build-drop-fn [compare-sym inclusive? sort-keys value]
  (let [keys  (map ->lower sort-keys)
        ->tup #(map % keys)
        pass? (case [(if (= > compare-sym) ">" "<") inclusive?]
                [">" true] neg?
                ["<" true] pos?
                [">" false] #(<= % 0)
                ["<" false] #(>= % 0))]
    #(pass? ((compare-keys sort-keys) (->tup %) value))))

(defrecord FirstPage []
  IPagination
  (next [this] (throw (ex-info "Not supported" this)))
  (prev [this] (throw (ex-info "Not supported" this)))
  (next-count [_ _ _] nil)
  (prev-count [_ _ _] nil)
  (serialize [_] [:first])
  (paginate [_ data sort-keys limit]
    (-paginate nil data sort-keys limit)))

(def open (FirstPage.))

(extend-protocol IPagination
  nil
  (next [this] (next open))
  (prev [this] (prev open))
  (next-count [_ data sort-keys] (next-count open data sort-keys))
  (prev-count [_ data sort-keys] (prev-count open data sort-keys))
  (serialize [_] (serialize open))
  (paginate [_ data sort-keys limit]
    (paginate open data sort-keys limit)))

(defrecord LastPage []
  IPagination
  (next [this] (throw (ex-info "Not supported" this)))
  (prev [this] (throw (ex-info "Not supported" this)))
  (next-count [_ _ _] nil)
  (prev-count [_ _ _] nil)
  (serialize [_] [:last])
  (paginate [_ data sort-keys limit]
    (-paginate nil data sort-keys limit true)))

(defrecord PageBefore [value inclusive]
  IPagination
  (next [_] (->PageAfter value (not inclusive)))
  (prev [this] (throw (ex-info "Not supported" this)))
  (next-count [_ data sort-keys]
    (count (remove (-build-drop-fn < inclusive sort-keys value) data)))
  (prev-count [_ _ _] nil)
  (serialize [_] [:before value inclusive])
  (paginate [_ data sort-keys limit]
    (-paginate (-build-drop-fn < inclusive sort-keys value)
               data sort-keys limit true)))

(defrecord PageAfter [value inclusive]
  IPagination
  (next [this] (throw (ex-info "Not supported" this)))
  (prev [_] (->PageBefore value (not inclusive)))
  (next-count [_ _ _] nil)
  (prev-count [_ data sort-keys] (count (remove (-build-drop-fn > inclusive sort-keys value) data)))
  (serialize [_] [:after value inclusive])
  (paginate [_ data sort-keys limit]
    (-paginate (-build-drop-fn > inclusive sort-keys value)
     data sort-keys limit)))

(defrecord PageRange [from to]
  IPagination
  (next [_] (->PageAfter to false))
  (prev [_] (->PageBefore from false))
  (next-count [_ data sort-keys]
    (count (remove (-build-drop-fn > false sort-keys to) data)))
  (prev-count [_ data sort-keys]
    (count (remove (-build-drop-fn < false sort-keys from) data)))
  (serialize [_] [:range from to])
  (paginate [_ data sort-keys limit]
    ;; NB: not it does NOT filter on the right (greedy expansion)
    (-paginate (-build-drop-fn > true sort-keys from)
               data sort-keys limit)))

(defn parse [[type x y]]
  (case type
    :open   (FirstPage.)
    :first  (FirstPage.)
    :last   (LastPage.)
    :before (PageBefore. x y)
    :after  (PageAfter. x y)
    :range  (PageRange. x y)
    (FirstPage.)))

(defn refinement [paginated sort-keys]
  (if (seq paginated)
    (let [keys (map ->lower sort-keys)
          skim #(map % keys)]
      (->PageRange (skim (first paginated))
                   (skim (last paginated))))
    (->FirstPage)))

(defn refine [pagination data sort-keys limit]
  (refinement (paginate pagination data sort-keys limit)
              sort-keys))

;; examples:

;; if client wants to keep all data on screen
;; - do not apply refine, data will stretch
;; - if wanting to only stretch to a point, can truncate or refine

;; can choose to refine on client or server - generally unless
;; explicitly in "offline" mode, want to also refine on server (to not miss
;; data)

;; without refining, we can safely go back/forward with cache without ever
;; missing data

;; can ping server (or be notified of changes) for next-count and prev-count
;; to drive breadcrumbs AND disabling of controls


;; Data windowing

(defn paginate-and-sort [pagination data sort-keys limit]
  (let [pagination (or pagination (FirstPage.))
        paginated  (paginate pagination data sort-keys limit)
        refined    (refinement paginated sort-keys)]
    [refined paginated]))
