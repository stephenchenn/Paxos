To compile:
    javac -d bin/  *.java

To run Paxos:
    java -cp  bin/ Paxos

To test Paxos:
    java -cp  bin/ Test

Notes:
 - program logs can be found at output.txt
 - when program starts, you will be prompted to enter a number from 1-3(M1-M3), this indicates which councilor proposes first
 - the test harness tests 3 cases of different proposers (M1,M2,M3) proposing first