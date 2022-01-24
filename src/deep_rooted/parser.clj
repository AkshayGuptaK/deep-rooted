(ns deep-rooted.parser
  (:require [clojure.string :as str])
  (:import [java.time LocalTime]))

(def order-type-str-to-order-type
  {:d :demand
   :s :supply})

(defn- parse-identifier [identifier]
  (let [[order-type-str id-str] (str/split identifier #"")
        order-type ((keyword order-type-str) order-type-str-to-order-type)
        id (Integer/parseInt id-str)]
    (assert (not (nil? order-type)) "Invalid order type provided")
    {:id id
     :order-type order-type}))

(defn- parse-price [price-str]
  (-> price-str
      (str/replace #"/kg" "")
      Integer/parseInt))

(defn- parse-qty [qty-str]
  (-> qty-str
      (str/replace #"kg" "")
      Integer/parseInt))

(defn parse-input-line [line]
  "Parses a line of input into an order map, throwing exceptions if input is invalid"
  (let [[identifier time-str produce-str price-str qty-str] (str/split line #" ")
        {:keys [order-type id]} (parse-identifier identifier)
        time (LocalTime/parse time-str)
        produce (keyword produce-str)
        price (parse-price price-str)
        qty (parse-qty qty-str)]
    {:id id
     :produce produce
     :order-type order-type
     :price price
     :quantity qty
     :time time}))

