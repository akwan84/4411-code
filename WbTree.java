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

        tree.insert(9);
        tree.insert(1);
        tree.insert(13);
        tree.insert(4);
        tree.insert(16);

        tree.insert(12);
        tree.insert(20);

        tree.printTree();

        
    }
}
