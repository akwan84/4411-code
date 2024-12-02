# C++ B+ Tree
As of right now, there is only 1 file, `bplus_tree.cpp`, which is a B+ tree written in C++ with insertion and search capabilities. The code keeps track of the number of read and write operations, and allows you to set an appropriate delay for read and write operations to simulate NVMM speeds. To compile and run the code:
```
g++ -o bptree.o bplus_tree.cpp
./bptree.o
```

# Java wB+ Tree
Due to the complexity of this structure, we were unable to get this structure implemented in C++ like the rest of the structures, rather using Java to not have to deal with the complex pointer arithmetic required when using C++. To run the implemented wB+ tree:
```
javac *.java
java WbTree
```
For the modified wB+ tree with sorted internal nodes:
```
javac *.java
java SWbTree
```