#include <iostream>
#include <vector>
#include <algorithm>
#include <thread>
#include <chrono>

using namespace std;

const int MAX = 4; // maximum keys per node 
const int READ_TIME = 10;
const int WRITE_TIME = 50;
int numReads = 0;
int numWrites = 0;

class LeafNode;
class InternalNode;

class InternalNode {
    public:
        vector<int> keys;
        vector<int> keySlotArr;

        vector<InternalNode> internalChildren;
        vector<LeafNode*> leafChildren;

        vector<int> childrenSlotArr;
};

class LeafNode {
    public:
        vector<int> keys;
        vector<int> slotArr;

        void insert(int key);
        void split(InternalNode* parent, int index);
};

void LeafNode::insert(int key) {
    keys.push_back(key);
    int index = keys.size() - 1;

    // Binary search to find the correct insertion point in slotArr
    int l = 0, r = slotArr.size();
    while (l < r) {
        int mid = (l + r) / 2;
        if (keys[slotArr[mid]] < key) {
            l = mid + 1;
        } else {
            r = mid; // Potential insertion point
        }
    }

    // Insert the pointer into the slotArr at the found position
    slotArr.insert(slotArr.begin() + l, index);
}

void LeafNode::split(InternalNode* parent, int index) {
    LeafNode* newNode;

    int promote = keys[slotArr[(MAX/2) - 1]]; //key to be promoted

    for(int i = MAX/2; i < MAX; i++) {
        newNode->keys.push_back(keys[slotArr[i]]);
        newNode->slotArr.push_back(newNode->keys.size() - 1);
    }

    vector<int> newKeys;
    vector<int> newSlotArr;

    for(int i = 0; i < MAX/2; i++) {
        newKeys.push_back(keys[slotArr[i]]);
        newSlotArr.push_back(newKeys.size() - 1);
    }

    keys = newKeys;
    slotArr = newSlotArr;

    /* insert key into parent node */
    parent->keys.push_back(promote);
    int slotArrIndex = parent->keys.size() - 1;

    cout << "here\n";

    // Binary search to find the correct insertion point in keySlotArr
    int l = 0, r = parent->keySlotArr.size();
    while (l < r) {
        int mid = (l + r) / 2;
        if (parent->keys[parent->keySlotArr[mid]] < promote) {
            l = mid + 1;
        } else {
            r = mid; // Potential insertion point
        }
    }

    // Insert the pointer into the slotArr at the found position
    parent->keySlotArr.insert(parent->keySlotArr.begin() + l, slotArrIndex);

    cout << "here\n";
    //insert new pointer into parent node

}

int main() {
    LeafNode node;
    InternalNode* parent = new InternalNode();

    node.insert(5);
    node.insert(10);
    node.insert(2);
    node.insert(7);

    //node->split(parent, 0);

    //cout << parent->leafChildren.size() << "\n";

    return 0;
}