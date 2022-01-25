# deep-rooted

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

