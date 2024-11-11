# C++ B+ Tree
As of right now, there is only 1 file, `bplus_tree.cpp`, which is a B+ tree written in C++ with insertion and search capabilities. The code keeps track of the number of read and write operations, and allows you to set an appropriate delay for read and write operations to simulate NVMM speeds. To compile and run the code:
```
g++ bptree.o bplus_tree.cpp
./bptree.o
```