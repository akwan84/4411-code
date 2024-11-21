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
        
        int bitmap;
        std::vector<int> slotArr;

        WBTreeNode (bool leaf){
            isLeaf = leaf;
            next = nullptr;
            count = 0;
            bitmap = 0;
        }

        void insert(int key); //insert a key into a non-full node
};

void WBTreeNode::insert(int key) {
    if(isLeaf) {
        keys.push_back(key);
        int index = keys.size() - 1;

        // Binary search to find the correct insertion point in slotArr
        int l = 0;
        int r = slotArr.size();
        while (l < r) {
            int mid = (l + r) / 2;
            if (keys[slotArr[mid]] < key) { // Compare values pointed by slotArr[mid]
                l = mid + 1;
            } else {
                r = mid; // Potential insertion point
            }
        }

        // Insert the pointer into the slotArr at the found position
        slotArr.insert(slotArr.begin() + l, index);
    }
}

int main() {
    WBTreeNode node = new WBTreeNode(true);

    node.insert(5);
    node.insert(2);
    node.insert(3);

    node.insert(13);
    node.insert(4);

    // Display the keys in slotArr (sorted order)
    std::cout << "Sorted keys: ";
    for (int ptr : node.slotArr) {
        std::cout << node.keys[ptr] << " ";
    }
    std::cout << std::endl;

    return 0;
}