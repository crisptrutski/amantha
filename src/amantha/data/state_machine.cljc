(ns amantha.data.state-machine)

(defn- find-state [machine state]
  (first (filter (comp #{state} :id) machine)))

(defn- find-edge [machine state action]
  (get (:actions (find-state machine state)) action))

(defn- -labelled-actions
  "Given a map (or vec of vecs) of {action-name action-attributes},
   return a seq of {:action, :label} maps."
  [action-attrs]
  (for [[action attrs] action-attrs
        :let [label (:label attrs (name action))
              type  (:type attrs :other)]]
    {:action action, :label label, :type type}))

(defn multi-graph?
  "There exists more than one route in a given direction between same two states."
  [machine]
  (some (fn [state]
          (let [targets (map :target (vals (:actions state)))
                counts  (vals (frequencies targets))]
            (< 1 (reduce max 0 counts))))
        machine))

(defn- central-states-sorted?
  "Central states are not in canonical order (ie. each is the :next of the last)"
  [machine]
  (let [central-states (filter (comp #{:central} :type) machine)]
    (some
      (fn [[state next-key]]
        (let [edges (vals (:actions state))]
          (not= next-key (:target (first (filter (comp #{:next} :type) edges))))))
      (map vector central-states (map :id (rest central-states))))))

(def error-types
  {:ambiguous-routes     multi-graph?
   :central-out-of-order central-states-sorted?})

(defn errors [machine]
  (let [fail? #(let [f (error-types %)] (f machine))]
    (into #{} (filter fail?) (keys error-types))))

(defn valid? [machine]
  (empty? (errors machine)))

(defn find-action
  "Return action going from states `key-a` to `key-b`, if it exists."
  [machine key-a key-b]
  (let [actions (:actions (find-state machine key-a))]
    (first (remove nil?
                   (for [[action info] actions]
                     (if (= key-b (:target info))
                       action))))))

(defn labelled-actions
  "Return a seq of the actions from `key` state, with their labels and type."
  [machine key]
  (-labelled-actions (:actions (find-state machine key))))

(defn destination
  "Resolve what state is reached when taking `action` from `state`"
  [machine state action]
  (:target (find-edge machine state action)))

(defn effects
  "Resolve what effects must occur when taking `action` from `state`."
  [machine state action]
  (:effects (find-edge machine state action)))

;; higher-level helpers

(defn- actionable?
  "Can action be taken?"
  [action-data pred-arg]
  (when action-data
    (let [f (:validation action-data)]
      (or (not f) (f pred-arg)))))

(defn -implicit-action
  "Return implicit action that should be taken, if relevant."
  [actions pred-arg]
  (let [valid (filter #(actionable? (second %) pred-arg) actions)]
    (cond (second valid) (prn "Warning - multiple implicit states to take")
          (seq valid) (ffirst valid)
          :else nil)))

(defn -handle-action
  "If actionable, return list of events to dispatch."
  [spec state action pred-arg effect-f switch-f]
  (let [{:keys [effects target] :as action-spec} (get-in state [:actions action])]
    (when (actionable? action-spec pred-arg)
      (conj (mapv effect-f effects)
            (switch-f target)))))


;; API

(defprotocol StateMachine
  (current-state [_])
  (handle-action [_ action])
  (implicit-action [_]))

(defn process-action
  "Take action, if possible. If implicit action should be triggered, that superceeds.
   Should really warn when taking implicit action."
  [state-machine action]
  (let [action (or (implicit-action state-machine) action)]
    (handle-action state-machine action)))


;; Implementation

(defrecord ValueStateMachine [spec ->state ->effect ->switch value]
  StateMachine
  (current-state [_]
    (find-state spec (->state value)))

  (handle-action [this action]
    (let [state (current-state this)]
      (-handle-action spec state action
                      value
                      (->effect value)
                      (->switch value))))

  (implicit-action [this]
    (let [state    (current-state this)
          actions  (:actions state)
          implicit (filter (comp :implicit second) actions)]
      (-implicit-action implicit value))))


;; Constructors

(defn multi-group-by
  "Similar to group-by, but index by every value in the collection returned by `group-fn`.
   Takes an optional function to map values through after grouping."
  ([f xs]
   (multi-group-by f xs identity))

  ([f xs xform]
   (reduce
     (fn [h x] (let [v (xform x)] (reduce (fn [h y] (update h y #(conj % v))) h (f x))))
     {} xs)))

(defn build
  "Build state machine specification from normalized definition."
  [states actions predicates]
  (let [actions   (into {} (map #(update-in % [1 :validation] predicates) actions))
        by-source (multi-group-by (comp :sources second) actions first)
        expand    (fn [{id :id}]
                    (into {} (map #(vector % (actions %)) (by-source id))))]
    (mapv #(assoc % :actions (expand %)) states)))

(defn entity-state-machine
  "Convention based state machine usable for workflows"
  [type spec value]
  (assert (contains? value :db))
  (assert (contains? value type))
  (ValueStateMachine. spec
                      ;; get current state
                      (comp #(:status % (:id (first spec))) :repair)
                      ;; build effect event
                      (fn [{{id :id} type}]
                        (fn [effect]
                          (vector effect id)))
                      ;; build state update event
                      (fn [{{id :id} type}]
                        (fn [target]
                          (vector (keyword (str "update-" (name type) "-status"))
                                  id target)))
                      value))
