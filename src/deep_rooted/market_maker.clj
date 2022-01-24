(ns deep-rooted.market-maker
  (:require [deep-rooted.ledger :as ledger]))

(defn- matching-supply-order [ledger-map {:keys [produce price]}]
  (let [available-supply 
        (ledger/prioritized-order ledger-map produce :supply)]
    (when (and available-supply (<= (:price available-supply) price))
      available-supply)))

(defn- matching-demand-order [ledger-map {:keys [produce price]}]
  (let [available-demand 
        (ledger/prioritized-order ledger-map produce :demand)]
    (when (and available-demand (>= (:price available-demand) price))
      available-demand)))

(defn matching-order [ledger-map {:keys [order-type] :as order}]
  (case order-type
    :demand (matching-supply-order ledger-map order)
    :supply (matching-demand-order ledger-map order)))

(defn update-or-remove-matching-order! [ledger-map produce order-type available-qty required-qty]
  (if (> available-qty required-qty)
          (ledger/update-prioritized-order-quantity! ledger-map produce order-type (- required-qty))
          (ledger/remove-prioritized-order! ledger-map produce order-type)))

(defn- trade [{:keys [id price order-type] :as order}
              {matching-id :id matching-price :price :as matching-order}
              trade-qty]
  (-> (case order-type
        :demand {:demand-id id :supply-id matching-id :price matching-price}
        :supply {:demand-id matching-id :supply-id id :price price})
      (merge {:quantity trade-qty})))

(defn process-order-and-determine-trades! [ledger-map {:keys [produce quantity] :as order}]
  "Determines what, if any, trades result from the publish of a new order and updates the ledger accordingly.
   Returns the updated ledger and any trades made. This function modifies the ledger in place and is hence impure."
  (loop [required-qty quantity 
         available-match (matching-order ledger-map order)
         trades []]
    (if (nil? available-match)
      {:ledger-map (->> {:quantity required-qty}
                        (merge order)
                        (ledger/add-order! ledger-map))
       :trades trades}
      (let [available-qty (:quantity available-match)
            trade-qty (min available-qty required-qty)
            trades (conj trades (trade order available-match trade-qty))
            new-requirement (- required-qty available-qty)]
        (update-or-remove-matching-order! ledger-map produce (:order-type available-match) available-qty required-qty)
        (if (> new-requirement 0)
          (recur new-requirement (matching-order ledger-map order) trades)
          {:ledger-map ledger-map
           :trades trades})))))
