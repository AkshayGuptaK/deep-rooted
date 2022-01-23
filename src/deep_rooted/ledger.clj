(ns deep-rooted.ledger
  "Contains functions for the maintenance and data retrieval of orders from a ledger.
  Orders are prioritized by price, time, and then order of publish.
  The ledger is a map containing priority queues, which are backed by heaps for logarithmic time insertion and removal."
  (:import [java.util PriorityQueue]))

(defn- compare-orders-by-time-and-id [order1 order2]
  (let [{id1 :id time1 :time} @order1 
        {id2 :id time2 :time} @order2
        id-difference  (- id1 id2)
        time-difference (.compareTo time1 time2)] 
    (if (= time-difference 0)
      id-difference
      time-difference)))

(defn- price-difference [ordering order1 order2]
  (let [{price1 :price} @order1 
        {price2 :price} @order2]
    (case ordering
      "asc" (- price1 price2)
      "desc" (- price2 price1))))

(defn- compare-orders [ordering order1 order2]
  (let [price-differential (price-difference ordering order1 order2)]
    (if (= price-differential 0)
      (compare-orders-by-time-and-id order1 order2)
      price-differential)))

(defn- demand-supply-queues []
  {:demand (PriorityQueue. (partial compare-orders "desc"))
   :supply (PriorityQueue. (partial compare-orders "asc"))})

(defn- atom-state-if-exists [atom]
  (when atom @atom))

(defn add-order! [ledger-map {:keys [produce order-type] :as order}]
  "Adds an `order` to the `ledger-map`, creating priority queues for demand and supply if they don't exist.
   The order is added as an atom so that a persistent identity exists for it when the stored order needs to be updated.
   This function mutates a priority queue in the ledger in place, and is hence impure. Returns the updated ledger."
  (let [ledger-map (if (contains? ledger-map produce)
                     ledger-map
                     (assoc ledger-map produce (demand-supply-queues)))]
    (-> ledger-map 
        (get-in [produce order-type])
        (.add (atom order)))
    ledger-map))

(defn prioritized-order [ledger-map produce order-type]
  "Returns the highest priority order for the given `produce` and `order-type`, or nil if no relevant order is found."
  (when (contains? ledger-map produce)
    (-> ledger-map
        (get-in [produce order-type])
        (.peek)
        (atom-state-if-exists))))

(defn update-prioritized-order-quantity! [ledger-map produce order-type change-in-qty]
  "Updates the quantity of the highest priority order for the given `produce` and `order-type` by the amount
   specified in `change-in-qty`. A relevant order must exist or an AssertionError will be thrown.
   This function mutates the contents of a priority queue in place, and is hence impure. Returns updated order."
  (assert (contains? ledger-map produce) "Ledger-Map does not contain any entries for that produce")
  (let [entry-atom (-> ledger-map
                       (get-in [produce order-type])
                       (.peek))]
    (assert (not (nil? entry-atom)) "Ledger-Map does not contain any entries for that produce and order type")
    (swap! entry-atom #(merge-with + % {:quantity change-in-qty}))))

(defn remove-prioritized-order! [ledger-map produce order-type]
  "Removes the highest priority order and returns it for the given `produce` and `order-type`.
   This function mutates the contents of a priority queue in place, and is hence impure.
   Returns nil if no relevant order is found."
  (when (contains? ledger-map produce)
    (-> ledger-map
        (get-in [produce order-type])
        (.poll)
        (atom-state-if-exists))))
