import java.util.*;

public class InternalNode {

    public static final int MAX = 4;

    List<Integer> keys;
    List<Integer> keySlotArr;

    List<InternalNode> internalChildren;
    List<LeafNode> leafChildren;

    List<Integer> childrenSlotArr;

    public InternalNode(){
        keys = new ArrayList<>();
        keySlotArr = new ArrayList<>();
        internalChildren = new ArrayList<>();
        leafChildren = new ArrayList<>();
        childrenSlotArr = new ArrayList<>();
    }

    public void insert(int key) {
        //convert to a binary search later
        int i = keys.size() - 1;

        while(i >= 0 && keys.get(keySlotArr.get(i)) > key) { //no NVM reads because of slot array use
            i--;
        }
        i++;

        if(leafChildren.size() != 0) {
            if(leafChildren.get(childrenSlotArr.get(i)).keys.size() == MAX) { //1 read
                leafChildren.get(childrenSlotArr.get(i)).split(this); //1 read
                
                i = keys.size() - 1;

                while(i >= 0 && keys.get(keySlotArr.get(i)) > key) {
                    i--;
                }
                i++;
            }
            leafChildren.get(childrenSlotArr.get(i)).insert(key); //1 read
            
            WbTree.numReads += 2;
        }else{
            if(internalChildren.get(childrenSlotArr.get(i)).keys.size() == MAX) { //1 read
                internalChildren.get(childrenSlotArr.get(i)).split(this); //1 read
                
                i = keys.size() - 1;

                while(i >= 0 && keys.get(keySlotArr.get(i)) > key) {
                    i--;
                }
                i++;
            }
            internalChildren.get(childrenSlotArr.get(i)).insert(key); //1 read

            WbTree.numReads += 2;
        }
        WbTree.numReads += 1;
    }

    public void split(InternalNode parent) {
        //create the new node
        InternalNode newNode = new InternalNode(); //assuming this is being created and stored in NVM

        int promote = keys.get(keySlotArr.get(MAX/2));

        //put the larger half of the keys into the new node
        for(int i = MAX/2 + 1; i < MAX; i++) {
            newNode.keys.add(keys.get(keySlotArr.get(i))); //1 write
            newNode.keySlotArr.add(newNode.keys.size() - 1); //slot array that can be loaded into RAM
            WbTree.numWrites++;
        }

        //restructure the smaller half of the keys
        List<Integer> newKeys = new ArrayList<>();
        List<Integer> newSlotArr = new ArrayList<>();

        for(int i = 0; i < MAX/2; i++) {
            newKeys.add(keys.get(keySlotArr.get(i)));
            newSlotArr.add(newKeys.size() - 1);
        }

        keys = newKeys;
        keySlotArr = newSlotArr;

        //split the pointers
        if(leafChildren.size() != 0) { //this internal node had leaf children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.leafChildren.add(leafChildren.get(childrenSlotArr.get(i))); //1 write
                newNode.childrenSlotArr.add(newNode.leafChildren.size() - 1); //slot array which can be loaded into RAM
                WbTree.numWrites++;
            }

            //restructure the smaller half of the keys
            List<LeafNode> newChildren = new ArrayList<>();
            List<Integer> newChildrenSlotArr = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(leafChildren.get(childrenSlotArr.get(i)));
                newChildrenSlotArr.add(newChildren.size() - 1);
            }

            leafChildren = newChildren;
            childrenSlotArr = newChildrenSlotArr;
        } else { //internal node had internal children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.internalChildren.add(internalChildren.get(childrenSlotArr.get(i))); //1 write
                newNode.childrenSlotArr.add(newNode.internalChildren.size() - 1); //slot array which can be loaded into RAM
                WbTree.numWrites++;
            }

            //restructure the smaller half of the keys
            List<InternalNode> newChildren = new ArrayList<>();
            List<Integer> newChildrenSlotArr = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(internalChildren.get(childrenSlotArr.get(i)));
                newChildrenSlotArr.add(newChildren.size() - 1);
            }

            internalChildren = newChildren;
            childrenSlotArr = newChildrenSlotArr;
        }

        //key promotion
        if(parent.keys.size() == 0) { //parent is a brand new internal node
            parent.keys.add(promote); //1 write
            parent.keySlotArr.add(0); //slot array loaded into RAM

            parent.internalChildren.add(this); //1 write
            parent.internalChildren.add(newNode); //1 write

            parent.childrenSlotArr.add(0); //slot array loaded into RAM
            parent.childrenSlotArr.add(1);

            WbTree.numWrites += 3;
        } else { //parent already has this node, need to find the right insertion point for newNode
            //add the promoted key
            parent.keys.add(promote); //1 write
            WbTree.numWrites++;
            int slotArrIndex = parent.keys.size() - 1;

            int l = 0, r = parent.keySlotArr.size();
            while (l < r) {
                int mid = (l + r) / 2;
                if (parent.keys.get(parent.keySlotArr.get(mid)) < promote) { //1 read
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
                WbTree.numReads++;
            }

            parent.keySlotArr.add(l, slotArrIndex); //1 write
            WbTree.numWrites++;

            //find the right spot for newNode in the parent
            parent.internalChildren.add(newNode); //1 write
            WbTree.numWrites++;

            int childrenSlotArrIndex = parent.internalChildren.size() - 1;

            l = 0; 
            r = parent.childrenSlotArr.size();
            while (l < r) {
                int mid = (l + r) / 2;
                InternalNode searchNode = parent.internalChildren.get(parent.childrenSlotArr.get(mid)); //1 read
                if (searchNode.keys.get(searchNode.keySlotArr.get(searchNode.keySlotArr.size() - 1)) < promote) {
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
                WbTree.numReads++;
            }

            parent.childrenSlotArr.add(l, childrenSlotArrIndex);
        }
    }

    private static LeafNode randomLeaf(int[] keys) {
        Random rand = new Random();
        int n = keys.length;

        int[] keyCopy = new int[n];

        for(int i = 0; i < n; i++) {
            keyCopy[i] = keys[i];
        }

        for(int i = 0; i < 100; i++) {
            int x = rand.nextInt(n);
            int y = rand.nextInt(n);

            int temp = keys[x];
            keys[x] = keys[y];
            keys[y] = temp;
        }

        LeafNode l = new LeafNode();
        for(int x : keys) {
            l.keys.add(x);
        }

        for(int x : keyCopy) {
            for(int i = 0; i < n; i++) {
                if(x == l.keys.get(i)) {
                    l.slotArr.add(i);
                    break;
                }
            }
        }
        
        return l;
    }

    /*public static void main(String[] args) {
        InternalNode node = new InternalNode();
        InternalNode parent = new InternalNode();

        LeafNode l1 = randomLeaf(new int[]{1, 3, 5, 6});
        LeafNode l2 = randomLeaf(new int[]{7, 9, 10, 12});
        LeafNode l3 = randomLeaf(new int[]{14, 16, 18, 21});
        LeafNode l4 = randomLeaf(new int[]{24, 28, 31, 32});
        LeafNode l5 = randomLeaf(new int[]{36, 38, 40, 44});

        node.keys.add(14);
        node.keys.add(7);
        node.keys.add(36);
        node.keys.add(24);

        node.keySlotArr.add(1);
        node.keySlotArr.add(0);
        node.keySlotArr.add(3);
        node.keySlotArr.add(2);

        node.leafChildren.add(l1);
        node.leafChildren.add(l4);
        node.leafChildren.add(l3);
        node.leafChildren.add(l5);
        node.leafChildren.add(l2);

        node.childrenSlotArr.add(0);
        node.childrenSlotArr.add(4);
        node.childrenSlotArr.add(2);
        node.childrenSlotArr.add(1);
        node.childrenSlotArr.add(3);

        InternalNode in2 = new InternalNode();
        InternalNode in3 = new InternalNode();

        in2.keys.add(56);
        in2.keySlotArr.add(0);

        in3.keys.add(80);
        in3.keySlotArr.add(0);

        parent.keys.add(75);
        parent.keys.add(50);

        parent.keySlotArr.add(1);
        parent.keySlotArr.add(0);

        parent.internalChildren.add(in2);
        parent.internalChildren.add(in3);
        parent.internalChildren.add(node);

        parent.childrenSlotArr.add(2);
        parent.childrenSlotArr.add(0);
        parent.childrenSlotArr.add(1);

        node.split(parent);
        //System.out.println(parent.keys.get(0));

        //loop through the parent's children
        for(int i = 0; i < 2; i++) {
            InternalNode curNode = parent.internalChildren.get(parent.childrenSlotArr.get(i));
            for(int j = 0; j < curNode.childrenSlotArr.size(); j++) {
                LeafNode curLeaf = curNode.leafChildren.get(curNode.childrenSlotArr.get(j));
                for(int k = 0; k < curLeaf.keys.size(); k++) {
                    System.out.println(curLeaf.keys.get(curLeaf.slotArr.get(k)));
                }
                System.out.println();
            }
        }

        /*for(int x : parent.childrenSlotArr){
            for(int y : parent.internalChildren.get(x).keySlotArr) {
                System.out.println(parent.internalChildren.get(x).keys.get(y));
            }
        }
        
    }*/

    public static void main(String[] args) {
        LeafNode n = new LeafNode();

        n.insert(1);
        n.insert(5);
        n.insert(3);
        n.insert(19);

        InternalNode root = new InternalNode();
        n.split(root);

        root.insert(13);
        root.insert(2);
        root.insert(-1);
        root.insert(-2);

        LeafNode l1 = root.leafChildren.get(root.childrenSlotArr.get(0));
        LeafNode l2 = root.leafChildren.get(root.childrenSlotArr.get(1));
        LeafNode l3 = root.leafChildren.get(root.childrenSlotArr.get(2));

        for(int x : l1.slotArr) {
            System.out.println(l1.keys.get(x));
        }

        System.out.println();
        for(int x : l2.slotArr) {
            System.out.println(l2.keys.get(x));
        }

        System.out.println();
        for(int x : l3.slotArr) {
            System.out.println(l3.keys.get(x));
        }

        System.out.println();
        for(int x : root.keySlotArr) {
            System.out.println(root.keys.get(x));
        }
    }
}
