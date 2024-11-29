import java.util.*;

public class WbTree {
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

    public String serialize() {
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

        /*for(int i = 0; i < keys.size(); i++) {
            System.out.print(keys.get(i) + " ");
        }
        System.out.println();*/

        List<LeafNode> leaves = new ArrayList<>();
        LeafNode cur = new LeafNode();
        //fill every leaf node to 50% capacity
        for(int key : keys) {
            if(cur.keys.size() >= MAX/2) {
                leaves.add(cur);
                cur = new LeafNode();
            }

            cur.keys.add(key);
            cur.slotArr.add(cur.keys.size() - 1);
        }
        leaves.add(cur);

        /*for(LeafNode node : leaves) {
            for(int x : node.slotArr) {
                System.out.print(node.keys.get(x) + " ");
            }
            System.out.print("     ");
        }
        System.out.println();*/

        if(leaves.size() == 1) {
            oldRoot = leaves.get(0);
            numLevels = 1;
            return;
        }

        //build internal nodes from the leaves
        List<InternalNode> newInternals = new ArrayList<>();
        InternalNode curInternal = new InternalNode();
        for(LeafNode leaf : leaves) {
            if(curInternal.leafChildren.size() == 0) {
                curInternal.leafChildren.add(leaf);
                curInternal.childrenSlotArr.add(0);
            }else{
                if(curInternal.keys.size() >= MAX/2) {
                    newInternals.add(curInternal);
                    curInternal = new InternalNode();
                    
                    curInternal.leafChildren.add(leaf);
                    curInternal.childrenSlotArr.add(0);
                } else {
                    curInternal.leafChildren.add(leaf);
                    curInternal.childrenSlotArr.add(curInternal.leafChildren.size() - 1);

                    curInternal.keys.add(leaf.keys.get(0));
                    curInternal.keySlotArr.add(curInternal.keys.size() - 1);
                }
            }
        }

        if(curInternal.childrenSlotArr.size() == 1) {
            //can not have an internal node with 1 child, move it over to the previous node
            LeafNode n = curInternal.leafChildren.get(0);

            InternalNode last = newInternals.get(newInternals.size() - 1);
            
            last.leafChildren.add(n);
            last.childrenSlotArr.add(last.leafChildren.size() - 1);

            last.keys.add(n.keys.get(0));
            last.keySlotArr.add(last.keys.size() - 1);
        }else{
            newInternals.add(curInternal);
        }

        int level = 2;
        while(newInternals.size() > 1) {
            List<InternalNode> nextLevel = new ArrayList<>();
            curInternal = new InternalNode();

            for(int i = 0; i < newInternals.size(); i++) {
                if(curInternal.internalChildren.size() == 0) {
                    curInternal.internalChildren.add(newInternals.get(i));
                    curInternal.childrenSlotArr.add(0);
                } else {
                    if(curInternal.keys.size() >= MAX/2) {
                        nextLevel.add(curInternal);
                        curInternal = new InternalNode();

                        curInternal.internalChildren.add(newInternals.get(i));
                        curInternal.childrenSlotArr.add(0);
                    } else {
                        InternalNode last = curInternal.internalChildren.get(curInternal.internalChildren.size() - 1);
                        int lastKey = last.keys.get(last.keys.size() - 1);
                        int firstKey = newInternals.get(i).keys.get(0);

                        int mid = (int)Math.ceil(((double)(firstKey + lastKey)) / 2.0);

                        curInternal.internalChildren.add(newInternals.get(i));
                        curInternal.childrenSlotArr.add(curInternal.internalChildren.size() - 1);

                        curInternal.keys.add(mid);
                        curInternal.keySlotArr.add(curInternal.keys.size() - 1);
                    }
                }
            }

            if(curInternal.internalChildren.size() == 1) {
                InternalNode lastNextLevel = nextLevel.get(nextLevel.size() - 1); //parent to add to
                InternalNode n = curInternal.internalChildren.get(0); //child to add
                InternalNode adjChild = lastNextLevel.internalChildren.get(lastNextLevel.internalChildren.size() - 1);

                int lastKey = adjChild.keys.get(adjChild.keys.size() - 1);
                int firstKey = n.keys.get(0);

                int mid = (int)Math.ceil(((double)(firstKey + lastKey)) / 2.0);

                lastNextLevel.internalChildren.add(n);
                lastNextLevel.childrenSlotArr.add(lastNextLevel.internalChildren.size() - 1);

                lastNextLevel.keys.add(mid);
                lastNextLevel.keySlotArr.add(lastNextLevel.keys.size() - 1);
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

        tree.printTree();
        System.out.println();
        tree.rebuild();
        tree.printTree();
        
    }
}
