# deep-rooted

## Prompt

Implement a demand-supply matching program for an online market maker.

1. Farmers/Traders publish the availability of the produce with details - quantity and price.These are supply orders.
2. Customers/Traders publish their requirement for the produce - quantity and the best price they can offer. These are demand orders.

All the requirements and the availability are stored in the demand-supply ledger. The application will match the demand with the supply in the ledger. Whenever a new supply or demand is published; matching is done. If the requirement cannot be satisfied it continues to remain in the ledger. No expiry is supported. The program must follow the rules below:

1. Priority must be given to "lower supply price - higher demand price" matching. Hence maximizing the profit for the market maker.
2. The supplier is always given the price he has asked for regardless of the price offered on the demand side.
3. Within the same supply/demand price, first-in-first out rule on time must be followed. First supply must be matched to the first demand.
4. A trade is generated when a buy price is greater than or equal to a sell price. As mentioned earlier the trade is recorded at the price of the supply regardless of the price of the demand.

Write a program that accepts supply/demand orders from standard input and writes trades to standard output.

The input format will be `<order-id> <time> <produce> <price/kg> <quantity in kg>`
where order-ids starting with
- `s` are supply orders
- `d` are demand orders
  
The output format should be `<demand-order-id> <supply-order-id> <price/kg> <quantity in kg>`

**Example 1**

Input
```
  s1 09:45 tomato 24/kg 100kg
  s2 09:46 tomato 20/kg 90kg
  d1 09:47 tomato 22/kg 110kg
  d2 09:48 tomato 21/kg 10kg
  d3 09:49 tomato 21/kg 40kg
  s3 09:50 tomato 19/kg 50kg
```
Output should be
```
  d1 s2 20/kg 90kg
  d1 s3 19/kg 20kg
  d2 s3 19/kg 10kg
  d3 s3 19/kg 20kg
```  

**Example 2**

Input
```
  d1 09:47 tomato 110/kg 1kg
  d2 09:45 potato 110/kg 10kg
  d3 09:48 tomato 110/kg 10kg
  s1 09:45 potato 110/kg 1kg
  s2 09:45 potato 110/kg 7kg
  s3 09:45 potato 110/kg 2kg
  s4 09:45 tomato 110/kg 11kg
  ```
Output should be
```
  d2 s1 110/kg 1kg
  d2 s2 110/kg 7kg 
  d2 s3 110/kg 2kg 
  d1 s4 110/kg 1kg 
  d3 s4 110/kg 10kg
```

## Example Usage

With Leiningen
`lein run < test-input.txt`

With Java
`java -jar target/uberjar/deep-rooted-0.1.0-SNAPSHOT-standalone.jar < test-input.txt`

## Data Structures

The data structure underpinning this program is implemented in the `Ledger` module. Prioritisation is done using a `Java PriorityQueue` which is internally backed by a heap, allowing for logarithmic time insertion.

Since the queue is not a persistent data structure, unlike those provided by Clojure, queuing and dequeuing is an in-place mutation. Functions that are impure due to making these types of mutation are indicated with a `!`. 

## Assumptions

The application makes the following assumptions about the nature of the input:
- All prices and quantities will be in `kg`
- All publishes happen on the same day
- The number next to the order type, e.g. the `1` in `d1`, represents the order in which publishes of that type appear in the input i.e. there will not be a `s2` in the input before an `s1`, although the time of `s2` could be earlier than `s1`

