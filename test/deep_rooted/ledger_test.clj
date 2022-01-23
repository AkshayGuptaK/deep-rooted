(ns deep-rooted.ledger-test
  (:require [deep-rooted.ledger :as sut]
            [deep-rooted.factories :refer [order ledger]]
            [clojure.test :refer :all]))

(deftest add-order-test
  (testing "It should create demand and supply queues for that produce if not existing"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          ledger-map (sut/add-order! {} test-order)]
      (is (every?
           #(get-in ledger-map %) [[:tomato :demand] [:tomato :supply]]) )))

  (testing "It should add the order to the appropriate queue"
   (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          ledger-map (sut/add-order! {} test-order)]
     (is (=
          (-> ledger-map
              (get-in [:tomato :demand])
              (.peek)
              (deref))
          test-order)))))

(deftest prioritized-order-test
  (testing "It should return the order if only a single order is in the queue"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/prioritized-order ledger-map :tomato :demand)]
      (is (= priority-order test-order))))

  (testing "It should return nil if the queue is empty"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/prioritized-order ledger-map :tomato :supply)]
      (is (= priority-order nil))))

  (testing "It should return nil if the produce has no entries"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/prioritized-order ledger-map :potato :supply)]
      (is (= priority-order nil))))

  (testing "It should return the order with the highest price from the demand queue"
    (let [test-order1 (order 1 "tomato" :demand 10 100 "09:05")
          test-order2 (order 2 "tomato" :demand 20 100 "09:06")
          ledger-map (ledger [test-order1 test-order2]) 
          priority-order (sut/prioritized-order ledger-map :tomato :demand)]
      (is (= priority-order test-order2))))

  (testing "It should return the order with the lowest price from the supply queue"
    (let [test-order1 (order 1 "tomato" :supply 10 100 "09:07")
          test-order2 (order 2 "tomato" :supply 20 100 "09:06")
          ledger-map (ledger [test-order1 test-order2]) 
          priority-order (sut/prioritized-order ledger-map :tomato :supply)]
      (is (= priority-order test-order1))))

  (testing "It should return the order with the earlier time if prices are equal"
    (let [test-order1 (order 1 "tomato" :supply 20 100 "09:07")
          test-order2 (order 2 "tomato" :supply 20 100 "09:06")
          ledger-map (ledger [test-order1 test-order2]) 
          priority-order (sut/prioritized-order ledger-map :tomato :supply)]
      (is (= priority-order test-order2))))

  (testing "It should return the order which was entered first if both time and price are equal"
    (let [test-order1 (order 1 "tomato" :supply 20 100 "09:06")
          test-order2 (order 2 "tomato" :supply 20 100 "09:06")
          ledger-map (ledger [test-order1 test-order2]) 
          priority-order (sut/prioritized-order ledger-map :tomato :supply)]
      (is (= priority-order test-order1)))))

(deftest remove-prioritized-order-test
  (testing "It should remove and return the order if only a single order is in the queue"
    (let [test-order (order 1 "tomato" :demand 20 5 "19:05")
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/remove-prioritized-order! ledger-map :tomato :demand)
          next-order (sut/prioritized-order ledger-map :tomato :demand)]
      (is (= priority-order test-order))
      (is (= next-order nil))))

  (testing "It should return nil if the queue is empty"
    (let [test-order (order 1 "tomato" :demand 1 1 "19:05")
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/remove-prioritized-order! ledger-map :tomato :supply)]
      (is (= priority-order nil))))

  (testing "It should return nil if the produce has no entries"
    (let [test-order (order 1 "tomato" :demand 1000 1000 "19:05")
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/remove-prioritized-order! ledger-map :potato :supply)]
      (is (= priority-order nil))))

  (testing "It should remove and return the order with the highest price from the demand queue"
    (let [test-order1 (order 1 "tomato" :demand 100 40 "09:05")
          test-order2 (order 2 "tomato" :demand 201 20 "09:16")
          ledger-map (ledger [test-order1 test-order2])
          priority-order (sut/remove-prioritized-order! ledger-map :tomato :demand)
          next-order (sut/prioritized-order ledger-map :tomato :demand)]
      (is (= priority-order test-order2))
      (is (= next-order test-order1))))

  (testing "It should remove and return the order with the lowest price from the supply queue"
    (let [test-order1 (order 1 "tomato" :supply 10 100 "09:07")
          test-order2 (order 2 "tomato" :supply 25 100 "08:03")
          ledger-map (ledger [test-order1 test-order2])
          priority-order (sut/remove-prioritized-order! ledger-map :tomato :supply)
          next-order (sut/prioritized-order ledger-map :tomato :supply)]
      (is (= priority-order test-order1))
      (is (= next-order test-order2))))

  (testing "It should remove and return the order with the earlier time if prices are equal"
    (let [test-order1 (order 1 "tomato" :supply 20 100 "09:07")
          test-order2 (order 2 "tomato" :supply 20 50 "06:06")
          ledger-map (ledger [test-order1 test-order2])
          priority-order (sut/remove-prioritized-order! ledger-map :tomato :supply)
          next-order (sut/prioritized-order ledger-map :tomato :supply)]
      (is (= priority-order test-order2))
      (is (= next-order test-order1))))

  (testing "It should remove and return the order which was entered first if both time and price are equal"
    (let [test-order1 (order 1 "tomato" :supply 20 100 "09:06")
          test-order2 (order 22 "tomato" :supply 20 500 "09:06")
          ledger-map (ledger [test-order1 test-order2])
          priority-order (sut/remove-prioritized-order! ledger-map :tomato :supply)
          next-order (sut/prioritized-order ledger-map :tomato :supply)]
      (is (= priority-order test-order1))
      (is (= next-order test-order2)))))

(deftest update-prioritized-order-quantity-test
  (testing "It should increment the order quantity if a positive change is given"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          qty-change 20
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/update-prioritized-order-quantity! ledger-map :tomato :demand qty-change)]
      (is (= (:quantity priority-order) (+ (:quantity test-order) qty-change)))))

  (testing "It should decrement the order quantity if a negative change is given"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          qty-change -15
          ledger-map (sut/add-order! {} test-order)
          priority-order (sut/update-prioritized-order-quantity! ledger-map :tomato :demand qty-change)]
      (is (= (:quantity priority-order) (+ (:quantity test-order) qty-change)))))
  
  (testing "It should throw exception if the queue is empty"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          ledger-map (sut/add-order! {} test-order)]
      (is (thrown? java.lang.AssertionError
                   (sut/update-prioritized-order-quantity! ledger-map :tomato :supply 5)))))

  (testing "It should throw exception if the produce has no entries"
    (let [test-order (order 1 "tomato" :demand 10 100 "09:05")
          ledger-map (sut/add-order! {} test-order)]
      (is (thrown? java.lang.AssertionError
                   (sut/update-prioritized-order-quantity! ledger-map :potato :supply -5))))))

(deftest integration
  (testing "It should return all orders bearing same price and time in the original ordering"
    (let [ledger-map ()
          test-order1 (order 1 "tomato" :supply 20 100 "09:06")
          test-order2 (order 2 "tomato" :supply 20 100 "09:06")
          test-order3 (order 3 "tomato" :supply 20 100 "09:06")
          test-order4 (order 4 "tomato" :supply 20 100 "09:06")
          test-order5 (order 5 "tomato" :supply 20 100 "09:06")
          test-order6 (order 6 "tomato" :supply 20 100 "09:06")
          test-order7 (order 7 "tomato" :supply 20 100 "09:06")
          test-order8 (order 8 "tomato" :supply 20 100 "09:06")
          test-order9 (order 9 "tomato" :supply 20 100 "09:06")
          ledger-map (ledger [test-order1
                              test-order2
                              test-order3
                              test-order4
                              test-order5
                              test-order6
                              test-order7
                              test-order8
                              test-order9])
          priority-order1 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order2 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order3 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order4 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order5 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order6 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order7 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order8 (sut/remove-prioritized-order! ledger-map :tomato :supply)
          priority-order9 (sut/remove-prioritized-order! ledger-map :tomato :supply)]
      (is (= priority-order1 test-order1))
      (is (= priority-order2 test-order2))
      (is (= priority-order3 test-order3))
      (is (= priority-order4 test-order4))
      (is (= priority-order5 test-order5))
      (is (= priority-order6 test-order6))
      (is (= priority-order7 test-order7))
      (is (= priority-order8 test-order8))
      (is (= priority-order9 test-order9)))))
