#include <iostream>
#include <vector>
#include <algorithm>
#include <thread>
#include <chrono>

const int MAX = 4; // maximum keys per node 
const int READ_TIME = 10;
const int WRITE_TIME = 50;
int numReads = 0;
int numWrites = 0;

class WBTreeNode {
    public:
        bool isLeaf;
        int count;
        std::vector<int> keys;
        std::vector<WBTreeNode*> children;
        WBTreeNode* next;
        
        std::vector<bool> bitmap;
        std::vector<int*> slotArr;

        WBTreeNode (bool leaf){
            isLeaf = leaf;
            next = nullptr;
            count = 0;

            for(int i = 0; i < MAX; i++) {
                bitmap.push_back(false);
            }
        }

        void insert(int key); //insert a key into a non-full node
};

int main() {
    WBTreeNode node = new WBTreeNode(false);
    for(int i = 0; i < MAX; i++) {
        std::cout << node.bitmap[i] << " ";
    }
}