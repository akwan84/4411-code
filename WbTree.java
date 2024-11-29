import java.util.*;

public class WbTree {

    public static int numReads = 0;
    public static int numWrites = 0;

    LeafNode oldRoot;
    InternalNode root;
    int numLevels;
    String lastState;

    public static final int MAX = 4;

    public WbTree() {
        oldRoot = new LeafNode();
        root = new InternalNode();
        numLevels = 1;
        lastState = "";
    }

    public boolean search(int key) {
        if(numLevels == 1) {
            return searchLeaf(key, oldRoot);
        }else{
            InternalNode cur = root;
            LeafNode target = new LeafNode();
            for(int i = 0; i < numLevels - 1; i++) {
                int index = cur.keys.size() - 1;
                while(index >= 0 && key < cur.keys.get(cur.keySlotArr.get(index))) { //1 read
                    index--;
                    WbTree.numReads++;
                }

                if(cur.leafChildren.size() != 0) {
                    target = cur.leafChildren.get(cur.childrenSlotArr.get(index + 1)); //1 read
                }else{
                    cur = cur.internalChildren.get(cur.childrenSlotArr.get(index + 1)); //1 read
                }
                WbTree.numReads++;
            }
            return searchLeaf(key, target);
        }
    }

    private boolean searchLeaf(int key, LeafNode node) {
        int l = 0;
        int r = node.keys.size() - 1;

        while(l <= r) {
            int mid = (l + r) / 2;
            if(node.keys.get(node.slotArr.get(mid)) == key) return true; 

            if(node.keys.get(node.slotArr.get(mid)) < key) { //1 read
                l = mid + 1;
            }else{
                r = mid - 1;
            }
            WbTree.numReads++;
        }
        return false;
    }

    public void insert(int key) {
        if(numLevels == 1 && oldRoot.keys.size() < MAX) {
            oldRoot.insert(key);
        } else {
            if(numLevels == 1) {
                oldRoot.split(root);
                numLevels++;
            } else if (root.keys.size() == MAX){
                InternalNode newRoot = new InternalNode();
                root.split(newRoot);
                root = newRoot;
                numLevels++;
            }
            root.insert(key);
        }
        lastState = serialize();
    }

    private String serialize() {
        StringBuilder str = new StringBuilder();
        if(numLevels == 1) {
            for(int i = 0; i < oldRoot.keys.size(); i++) {
                str.append(oldRoot.keys.get(oldRoot.slotArr.get(i)));
                str.append(",");
            }
            return str.toString().substring(0, str.length() - 1);
        }else{
            Queue<InternalNode> queue = new LinkedList<>();
            Queue<LeafNode> queue2 = new LinkedList<>();

            queue.add(root);

            while(!queue.isEmpty()) {
                for(int j = queue.size(); j > 0; j--) {
                    InternalNode cur = queue.poll();

                    if(cur.leafChildren.size() == 0) {
                        for(int i = 0; i < cur.childrenSlotArr.size(); i++) {
                            queue.add(cur.internalChildren.get(cur.childrenSlotArr.get(i)));
                        }
                    }else{
                        for(int i = 0; i < cur.childrenSlotArr.size(); i++) {
                            queue2.add(cur.leafChildren.get(cur.childrenSlotArr.get(i)));
                        }
                    }
                }
            }

            while(!queue2.isEmpty()) {
                LeafNode cur = queue2.poll();

                for(int i = 0; i < cur.keys.size(); i++) {
                    str.append(cur.keys.get(cur.slotArr.get(i)));
                    str.append(",");
                }
            }
            return str.toString().substring(0, str.length() - 1);
        }
    }

    public void rebuild() {
        List<Integer> keys = new ArrayList<>();
        
        /* Parse the last state of the tree to get the keys to rebuild the tree with */
        StringBuilder curNum = new StringBuilder();
        for(char c : lastState.toCharArray()) {
            if(c != ',') {
                curNum.append(c);
            }else{
                keys.add(Integer.parseInt(curNum.toString()));
                curNum = new StringBuilder();
            }
        }

        keys.add(Integer.parseInt(curNum.toString()));



        /* Rebuild Leaf Nodes */
        List<LeafNode> leaves = new ArrayList<>();
        LeafNode cur = new LeafNode(); //assume this is created in NVM

        for(int key : keys) {
            if(cur.keys.size() >= MAX/2) { //fill every leaf node to 50% capacity
                leaves.add(cur);
                cur = new LeafNode();
            }

            cur.keys.add(key); //1 write
            cur.slotArr.add(cur.keys.size() - 1);
            WbTree.numWrites++;
        }
        leaves.add(cur);

        //Small enough that the tree only consists of 1 leaf node
        if(leaves.size() == 1) {
            oldRoot = leaves.get(0);
            numLevels = 1;
            return;
        }



        /* Build first level of internal nodes from leaf nodes */
        List<InternalNode> newInternals = new ArrayList<>();
        InternalNode curInternal = new InternalNode();
        for(LeafNode leaf : leaves) {
            if(curInternal.leafChildren.size() == 0) {
                curInternal.leafChildren.add(leaf); // 1 write
                curInternal.childrenSlotArr.add(0);
                WbTree.numWrites++;
            }else{
                if(curInternal.keys.size() >= MAX/2) {
                    newInternals.add(curInternal);
                    curInternal = new InternalNode();
                    
                    curInternal.leafChildren.add(leaf); //1 write
                    curInternal.childrenSlotArr.add(0);
                    
                    WbTree.numWrites++;
                } else {
                    curInternal.leafChildren.add(leaf); //1 write
                    curInternal.childrenSlotArr.add(curInternal.leafChildren.size() - 1);

                    curInternal.keys.add(leaf.keys.get(0)); //1 write
                    curInternal.keySlotArr.add(curInternal.keys.size() - 1);

                    WbTree.numWrites++;
                }
            }
        }

        if(curInternal.childrenSlotArr.size() == 1) {
            //can not have an internal node with 1 child, move it over to the previous node
            LeafNode n = curInternal.leafChildren.get(0);

            InternalNode last = newInternals.get(newInternals.size() - 1);
            
            last.leafChildren.add(n); //1 write
            last.childrenSlotArr.add(last.leafChildren.size() - 1);

            last.keys.add(n.keys.get(0)); //1 write
            last.keySlotArr.add(last.keys.size() - 1);

            WbTree.numWrites++;
        }else{
            newInternals.add(curInternal);
        }

        
        
        /* Build the rest of the internal nodes until only 1 remains */
        int level = 2;
        while(newInternals.size() > 1) {
            List<InternalNode> nextLevel = new ArrayList<>();
            curInternal = new InternalNode();

            for(int i = 0; i < newInternals.size(); i++) {
                if(curInternal.internalChildren.size() == 0) {
                    curInternal.internalChildren.add(newInternals.get(i)); //1 write
                    curInternal.childrenSlotArr.add(0);

                    WbTree.numWrites++;
                } else {
                    if(curInternal.keys.size() >= MAX/2) {
                        nextLevel.add(curInternal);
                        curInternal = new InternalNode();

                        curInternal.internalChildren.add(newInternals.get(i)); //1 write
                        curInternal.childrenSlotArr.add(0);

                        WbTree.numWrites++;
                    } else {
                        InternalNode last = curInternal.internalChildren.get(curInternal.internalChildren.size() - 1); //1 read
                        int lastKey = last.keys.get(last.keys.size() - 1); //1 read
                        int firstKey = newInternals.get(i).keys.get(0); //1 read

                        int mid = (int)Math.ceil(((double)(firstKey + lastKey)) / 2.0);

                        curInternal.internalChildren.add(newInternals.get(i)); //1 write
                        curInternal.childrenSlotArr.add(curInternal.internalChildren.size() - 1);

                        curInternal.keys.add(mid); //1 write
                        curInternal.keySlotArr.add(curInternal.keys.size() - 1);

                        WbTree.numReads += 3;
                        WbTree.numWrites += 2;
                    }
                }
            }

            if(curInternal.internalChildren.size() == 1) {
                InternalNode lastNextLevel = nextLevel.get(nextLevel.size() - 1); //1 read
                InternalNode n = curInternal.internalChildren.get(0); //1 read
                InternalNode adjChild = lastNextLevel.internalChildren.get(lastNextLevel.internalChildren.size() - 1); //1 read

                int lastKey = adjChild.keys.get(adjChild.keys.size() - 1); //1 read
                int firstKey = n.keys.get(0); //1 read

                int mid = (int)Math.ceil(((double)(firstKey + lastKey)) / 2.0);

                lastNextLevel.internalChildren.add(n); //1 write
                lastNextLevel.childrenSlotArr.add(lastNextLevel.internalChildren.size() - 1);

                lastNextLevel.keys.add(mid); //1 write
                lastNextLevel.keySlotArr.add(lastNextLevel.keys.size() - 1);

                WbTree.numReads += 5;
                WbTree.numWrites += 2;
            }else{
                nextLevel.add(curInternal);
            }
            level++;
            newInternals = nextLevel;
        }

        root = newInternals.get(0);
        numLevels = level;
    }

    public void printTree() {
        Queue<InternalNode> queue = new LinkedList<>();
        Queue<LeafNode> queue2 = new LinkedList<>();

        queue.add(root);

        while(!queue.isEmpty()) {
            for(int j = queue.size(); j > 0; j--) {
                InternalNode cur = queue.poll();

                if(cur.leafChildren.size() == 0) {
                    for(int i = 0; i < cur.childrenSlotArr.size(); i++) {
                        queue.add(cur.internalChildren.get(cur.childrenSlotArr.get(i)));
                    }
                }else{
                    for(int i = 0; i < cur.childrenSlotArr.size(); i++) {
                        queue2.add(cur.leafChildren.get(cur.childrenSlotArr.get(i)));
                    }
                }

                for(int i = 0; i < cur.keySlotArr.size(); i++) {
                    System.out.print(cur.keys.get(cur.keySlotArr.get(i)) + " ");
                }
                System.out.print("      ");
            }
            System.out.println();
        }

        while(!queue2.isEmpty()) {
            LeafNode cur = queue2.poll();

            for(int i = 0; i < cur.keys.size(); i++) {
                System.out.print(cur.keys.get(cur.slotArr.get(i)) + " ");
            }
            System.out.print("      ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        WbTree tree = new WbTree();

        int numKeys = 30;
        int numShuffles = 100;
        int start = 5;
        int maxGap = 5;

        Random r = new Random();

        int[] keys = new int[numKeys];
        keys[0] = start;
        for(int i = 1; i < numKeys; i++) {
            keys[i] = keys[i-1] + r.nextInt(maxGap) + 1;
        }

        for(int i = 0; i < numShuffles; i++) {
            int x = r.nextInt(numKeys);
            int y = r.nextInt(numKeys);

            int temp = keys[x];
            keys[x] = keys[y];
            keys[y] = temp;
        }

        for(int i = 0; i < numKeys; i++) {
            tree.insert(keys[i]);
        }
        System.out.println(WbTree.numReads);
        System.out.println(WbTree.numWrites);

        //tree.printTree();
        //System.out.println();
        tree.rebuild();
        //System.out.println(WbTree.numReads);
        //System.out.println(WbTree.numWrites);
        //tree.printTree();

        //System.out.println(tree.search(47));
        //System.out.println(tree.search(4));
        //System.out.println(tree.search(155));
        
    }
}
