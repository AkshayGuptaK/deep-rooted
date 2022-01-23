(ns deep-rooted.factories
  (:require [deep-rooted.ledger :as ledger-fns])
  (:import [java.time LocalTime]))

(defn order [id produce order-type price quantity time]
  {:id id 
   :produce (keyword produce)
   :order-type order-type
   :price price
   :quantity quantity
   :time (LocalTime/parse time)})

(defn ledger [orders]
  (reduce ledger-fns/add-order! {} orders))
