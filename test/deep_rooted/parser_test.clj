(ns deep-rooted.parser-test
  (:require [deep-rooted.parser :as sut]
            [deep-rooted.factories :refer [order]]
            [clojure.test :refer :all]))

(deftest parse-input-line-test
  (testing "It should correctly parse a valid demand order"
    (let [input-line "d1 09:47 tomato 110/kg 1kg"
          input-order (order 1 "tomato" :demand 110 1 "09:47")]
      (is (= (sut/parse-input-line input-line) input-order))))

  (testing "It should correctly parse a valid supply order"
    (let [input-line "s3 22:50 potato 19/kg 50kg"
          input-order (order 3 "potato" :supply 19 50 "22:50")]
      (is (= (sut/parse-input-line input-line) input-order))))

  (testing "It should throw if the order type is neither demand nor supply"
    (let [input-line "g2 08:50 carrot 23/kg 150kg"]
      (is (thrown? java.lang.AssertionError
                   (sut/parse-input-line input-line)))))

  (testing "It should throw if id is not an integer"
    (let [input-line "dr 08:15 spaghetti 2/kg 15kg"]
      (is (thrown? java.lang.NumberFormatException
                   (sut/parse-input-line input-line)))))
  
  (testing "It should throw if it is not a valid time"
    (let [input-line "d2 8:15 spaghetti 2/kg 15kg"]
      (is (thrown? java.time.format.DateTimeParseException
                   (sut/parse-input-line input-line)))))
  
  (testing "It should throw if price is not an integer per kg"
    (let [input-line "d2 08:15 spaghetti 20/g 15kg"]
      (is (thrown? java.lang.NumberFormatException
                   (sut/parse-input-line input-line)))))

  (testing "It should throw if qty is not an integer kg"
    (let [input-line "d2 08:15 spaghetti 20/kg 15lb"]
      (is (thrown? java.lang.NumberFormatException
                   (sut/parse-input-line input-line))))))

