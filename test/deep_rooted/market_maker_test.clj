(ns deep-rooted.market-maker-test
  (:require [deep-rooted.market-maker :as sut]
            [deep-rooted.ledger :as ledger-fns]
            [deep-rooted.factories :refer [order ledger trade]]
            [clojure.test :refer :all]))

(deftest matching-order-test
  (testing "It should return a matching supply order for a demand when supplied price is lower"
    (let [supply-order (order 1 "tomato" :supply 10 20 "09:05")
          demand-order (order 1 "tomato" :demand 15 25 "09:10")
          ledger-map (ledger [supply-order])]
      (is (= supply-order (sut/matching-order ledger-map demand-order)))))

  (testing "It should return a matching supply order for a demand when supplied price is equal"
    (let [supply-order (order 1 "tomato" :supply 15 20 "09:05")
          demand-order (order 1 "tomato" :demand 15 25 "09:10")
          ledger-map (ledger [supply-order])]
      (is (= supply-order (sut/matching-order ledger-map demand-order)))))

  (testing "It should return nil when matching supply order for a demand has higher price"
    (let [supply-order (order 1 "tomato" :supply 20 20 "09:05")
          demand-order (order 1 "tomato" :demand 15 25 "09:10")
          ledger-map (ledger [supply-order])]
      (is (nil? (sut/matching-order ledger-map demand-order)))))

  (testing "It should return a matching demand order for a supply when demanded price is higher"
    (let [demand-order (order 1 "tomato" :demand 25 25 "09:00")
          supply-order (order 1 "tomato" :supply 20 20 "09:05")
          ledger-map (ledger [demand-order])]
      (is (= demand-order (sut/matching-order ledger-map supply-order)))))

  (testing "It should return a matching demand order for a supply when demanded price is equal"
    (let [demand-order (order 1 "tomato" :demand 25 25 "09:00")
          supply-order (order 1 "tomato" :supply 25 20 "09:05")
          ledger-map (ledger [demand-order])]
      (is (= demand-order (sut/matching-order ledger-map supply-order)))))

  (testing "It should return nil when matching demand order for a supply has lower price"
    (let [demand-order (order 1 "tomato" :demand 15 25 "09:00")
          supply-order (order 1 "tomato" :supply 20 20 "09:05")
          ledger-map (ledger [demand-order])]
      (is (nil? (sut/matching-order ledger-map supply-order))))))

(deftest update-or-remove-matching-order-test
  (testing "It should update the matching order's quantity in the ledger when it's quantity is greater than the requirement"
    (let [supply-qty 100 
          demand-qty 40
          supply-order (order 1 "tomato" :supply 20 supply-qty "09:00")
          ledger-map (ledger [supply-order])
          updated-order (sut/update-or-remove-matching-order! ledger-map :tomato :supply supply-qty demand-qty)]
      (is (= (:quantity updated-order) (- supply-qty demand-qty)))))

  (testing "It should remove the matching order from the ledger when it's quantity is equal to the requirement"
    (let [supply-qty 40 
          demand-qty 40
          supply-order (order 1 "tomato" :supply 20 supply-qty "09:00")
          ledger-map (ledger [supply-order])
          removed-order (sut/update-or-remove-matching-order! ledger-map :tomato :supply supply-qty demand-qty)]
      (is (= supply-order removed-order))))

  (testing "It should remove the matching order from the ledger when it's quantity is lesser than the requirement"
    (let [supply-qty 30 
          demand-qty 40
          supply-order (order 1 "tomato" :supply 20 supply-qty "09:00")
          ledger-map (ledger [supply-order])
          removed-order (sut/update-or-remove-matching-order! ledger-map :tomato :supply supply-qty demand-qty)]
      (is (= supply-order removed-order)))))

(deftest process-order-and-determine-trades-test
  (testing "It should add the order to the ledger if no matching fulfilment order is found"
    (let [demand-order (order 1 "potato" :demand 15 50 "08:00")
          {:keys [ledger-map trades]} (sut/process-order-and-determine-trades! {} demand-order)
          added-demand-order (ledger-fns/prioritized-order ledger-map :potato :demand)]
      (is (= added-demand-order demand-order))
      (is (= trades []))))
  
  (testing "It should add the order with remaining qty if matching orders total less than required qty"
    (let [supply-qty 30
          demand-qty 50
          surplus-requirement (- demand-qty supply-qty)
          supply-price 10
          supply-order (order 1 "potato" :supply supply-price supply-qty "07:30")
          demand-order (order 1 "potato" :demand 15 demand-qty "08:00")
          ledger-map (ledger [supply-order])
          {:keys [ledger-map trades]} (sut/process-order-and-determine-trades! ledger-map demand-order)
          added-demand-order (ledger-fns/prioritized-order ledger-map :potato :demand)]
      (is (= (:quantity added-demand-order) surplus-requirement))
      (is (= trades [(trade 1 1 supply-price supply-qty)]))))

  (testing "It should return a single trade if the first matching order fulfils the requirement entirely"
    (let [supply-qty 50
          demand-qty 50
          supply-price 10
          supply-order (order 1 "potato" :supply supply-price supply-qty "07:30")
          demand-order (order 1 "potato" :demand 15 demand-qty "08:00")
          ledger-map (ledger [supply-order])
          {:keys [ledger-map trades]} (sut/process-order-and-determine-trades! ledger-map demand-order)
          added-demand-order (ledger-fns/prioritized-order ledger-map :potato :demand)]
      (is (nil? added-demand-order))
      (is (= trades [(trade 1 1 supply-price supply-qty)]))))

  (testing "It should return multiple trades if multiple matching orders are needed to fulfil the requirement"
    (let [supply-qty1 20
          supply-qty2 30
          demand-qty 50
          supply-price1 10
          supply-price2 15
          supply-order1 (order 1 "potato" :supply supply-price1 supply-qty1 "07:30")
          supply-order2 (order 2 "potato" :supply supply-price2 supply-qty2 "07:45")
          demand-order (order 1 "potato" :demand 15 demand-qty "08:00")
          ledger-map (ledger [supply-order1 supply-order2])
          {:keys [ledger-map trades]} (sut/process-order-and-determine-trades! ledger-map demand-order)
          added-demand-order (ledger-fns/prioritized-order ledger-map :potato :demand)]
      (is (nil? added-demand-order))
      (is (= trades [(trade 1 1 supply-price1 supply-qty1)
                     (trade 1 2 supply-price2 supply-qty2)])))))

(deftest process-orders-test
  (testing "It should return no trades if no orders are given"
    (is (= (sut/process-orders []) [])))
 
  (testing "It should return correct trades for a series of orders"
    (let [orders [(order 1 "tomato" :supply 24 100 "09:45")
                  (order 2 "tomato" :supply 20 90 "09:46")
                  (order 1 "tomato" :demand 22 110 "09:47")
                  (order 2 "tomato" :demand 21 10 "09:48")
                  (order 3 "tomato" :demand 21 40 "09:49")
                  (order 3 "tomato" :supply 19 50 "09:50")]
          trades [(trade 1 2 20 90)
                  (trade 1 3 19 20)
                  (trade 2 3 19 10)
                  (trade 3 3 19 20)]]
      (is (= (sut/process-orders orders) trades)))))
