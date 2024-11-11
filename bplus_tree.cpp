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

//class to define a node
class BPTreeNode {
    public:
        bool isLeaf; //whether the node is a leaf
        std::vector<int> keys; //keys in the node
        std::vector<BPTreeNode*> children; //pointers to child nodes
        BPTreeNode* next; //next pointer for leaf nodes

        BPTreeNode(bool leaf) {
            isLeaf = leaf; 
            next = nullptr;
        }

        //class methods
        void insertNonFull(int key); //insert a key into a non-full node
        void splitChild(int i, BPTreeNode* child); //method to split node
        BPTreeNode* search(int key); //recursive search method
};

//class to define the tree
class BPTree {
    private:
        BPTreeNode* root; //root node

    public:
        //constructor to initialize the root of the tree, which is initially a leaf node
        BPTree() {
            root = new BPTreeNode(true);
        }

        //class methods
        void insert(int key);
        BPTreeNode* search(int key);
        void printTree(BPTreeNode* node, int level = 0);
        BPTreeNode* getRoot() { return root; }
};

void BPTreeNode::insertNonFull(int key) {
    int i = keys.size() - 1; //

    if (isLeaf) { //if the node is a leaf, insert the key into the right spot then we're done
        keys.push_back(key);
        for(int x = keys.size() - 1; x > 0; x--) {
            if(keys[x] < keys[x-1]) {
                int temp = keys[x];
                keys[x] = keys[x-1];
                keys[x-1] = temp;

                numReads += 2;
                numWrites += 2;

                std::this_thread::sleep_for(std::chrono::milliseconds((WRITE_TIME * 2) + (READ_TIME * 2)));
            }else{
                break;
            }
        }
    } else { //in any other case
        //find the appropriate node to insert the key into
        while (i >= 0 && keys[i] > key) {
            i--;
            numReads++;
            std::this_thread::sleep_for(std::chrono::milliseconds(READ_TIME));
        }
        i++;
        
        //if the selected node is at capacity, split the node
        if (children[i]->keys.size() == MAX) {
            splitChild(i, children[i]);
            if (keys[i] < key) i++;

            numReads++;
            std::this_thread::sleep_for(std::chrono::milliseconds(READ_TIME));
        }

        //recursively call this method with the appropriate child node until a leaf is reached
        children[i]->insertNonFull(key);
    }
}

void BPTreeNode::splitChild(int i, BPTreeNode* child) {
    //initilaize 2nd node which will be on the same level as child
    BPTreeNode* newChild = new BPTreeNode(child->isLeaf);
    int mid = MAX / 2;

    //take the largest half of the keys and add them to newChild
    newChild->keys.assign(child->keys.begin() + mid, child->keys.end());
    child->keys.resize(mid);

    numReads += mid;
    numWrites += mid;
    std::this_thread::sleep_for(std::chrono::milliseconds((WRITE_TIME * mid) + (READ_TIME * mid)));

    //if the node is not a leaf, take the right half of the child pointers and add them to newChild
    if (!child->isLeaf) {
        newChild->children.assign(child->children.begin() + mid, child->children.end());
        child->children.resize(mid);

        numReads += mid;
        numWrites += mid;
        std::this_thread::sleep_for(std::chrono::milliseconds((WRITE_TIME * mid) + (READ_TIME * mid)));
    }

    //add the new key and child pointer
    children.insert(children.begin() + i + 1, newChild);
    keys.insert(keys.begin() + i, newChild->keys[0]);

    numWrites += 2;
    std::this_thread::sleep_for(std::chrono::milliseconds((WRITE_TIME * 2)));

    //set the next pointer if the node is a leaf
    if (child->isLeaf) {
        newChild->next = child->next;
        child->next = newChild;

        numWrites += 2;
        std::this_thread::sleep_for(std::chrono::milliseconds((WRITE_TIME * 2)));
    }
}

void BPTree::insert(int key) {
    BPTreeNode* rootNode = root;

    if (rootNode->keys.size() == MAX) {
        BPTreeNode* newRoot = new BPTreeNode(false);
        newRoot->children.push_back(root);
        newRoot->splitChild(0, root);
        root = newRoot;
    }
    root->insertNonFull(key);
}

BPTreeNode* BPTreeNode::search(int key) {
    int i = 0;
    while (i < keys.size() && key > keys[i]) {
        i++;

        numReads++;
        std::this_thread::sleep_for(std::chrono::milliseconds(READ_TIME));
    }

    if (i < keys.size() && key == keys[i] && isLeaf) {
        return this;
    } else if (isLeaf) {
        return nullptr;
    }

    return children[i]->search(key);
}

BPTreeNode* BPTree::search(int key) {
    return root->search(key);
}

void BPTree::printTree(BPTreeNode* node, int level) {
    if (node) {
        std::cout << "Level " << level << ": ";
        for (int key : node->keys)
            std::cout << key << " ";
        std::cout << "\n";
        
        for (auto* child : node->children)
            printTree(child, level + 1);
    }
}

int main() {
    BPTree tree;
    
    int values[] = {10, 20, 5, 6, 12, 30, 7, 17};
    
    auto start = std::chrono::high_resolution_clock::now();
    for (int val : values) {
        tree.insert(val);
    }
    auto end = std::chrono::high_resolution_clock::now();

    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    std::cout << "Execution Time: " << duration.count() << "\n";
    std::cout << "Number of Reads: " << numReads << "\n";
    std::cout << "Number of Writes: " << numWrites << "\n\n";

    std::cout << "B+ Tree Structure:\n";
    tree.printTree(tree.getRoot());

    int keyToSearch = 6;
    BPTreeNode* result = tree.search(keyToSearch);
    if (result) {
        std::cout << "\nKey " << keyToSearch << " found in the tree.\n";
    } else {
        std::cout << "\nKey " << keyToSearch << " not found in the tree.\n";
    }

    return 0;
}