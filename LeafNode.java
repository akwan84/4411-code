import java.util.*;

public class LeafNode {
    List<Integer> keys;
    List<Integer> slotArr;

    public static final int MAX = 4;

    public LeafNode() {
        this.keys = new ArrayList<>();
        this.slotArr = new ArrayList<>();
    }

    public void insert(int key) {
        keys.add(key);
        int index = keys.size() - 1;

        int l = 0, r = slotArr.size();
        while (l < r) {
            int mid = (l + r) / 2;
            if (keys.get(slotArr.get(mid)) < key) {
                l = mid + 1;
            } else {
                r = mid; // Potential insertion point
            }
        }

        slotArr.add(l, index);
    }

    public void split(InternalNode parent) {
        LeafNode newNode = new LeafNode();

        int promote = keys.get(slotArr.get(MAX/2));

        for(int i = MAX/2; i < MAX; i++) {
            newNode.keys.add(keys.get(slotArr.get(i)));
            newNode.slotArr.add(newNode.keys.size() - 1);
        }

        List<Integer> newKeys = new ArrayList<>();
        List<Integer> newSlotArr = new ArrayList<>();

        for(int i = 0; i < MAX/2; i++) {
            newKeys.add(keys.get(slotArr.get(i)));
            newSlotArr.add(newKeys.size() - 1);
        }

        keys = newKeys;
        slotArr = newSlotArr;


        /* add the promoted key to the parent */
        parent.keys.add(promote);
        int slotArrIndex = parent.keys.size() - 1;

        int l = 0, r = parent.keySlotArr.size();
        while (l < r) {
            int mid = (l + r) / 2;
            if (parent.keys.get(parent.keySlotArr.get(mid)) < promote) {
                l = mid + 1;
            } else {
                r = mid; // Potential insertion point
            }
        }

        parent.keySlotArr.add(l, slotArrIndex);

        /* add the new node to the parent */
        if(parent.leafChildren.size() == 0) {
            parent.leafChildren.add(this);
            parent.leafChildren.add(newNode);

            parent.childrenSlotArr.add(0);
            parent.childrenSlotArr.add(1);
        } else {
            parent.leafChildren.add(newNode);
            int childrenSlotArrIndex = parent.leafChildren.size() - 1;

            l = 0; 
            r = parent.childrenSlotArr.size();
            while (l < r) {
                int mid = (l + r) / 2;
                LeafNode searchNode = parent.leafChildren.get(parent.childrenSlotArr.get(mid));
                if (searchNode.keys.get(searchNode.slotArr.get(searchNode.slotArr.size() - 1)) < promote) {
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
            }

            parent.childrenSlotArr.add(l, childrenSlotArrIndex);
        }
    }

    public static void main(String[] args) 
    {
        LeafNode leaf = new LeafNode();
        InternalNode parent = new InternalNode();

        leaf.insert(1);
        leaf.insert(2);
        leaf.insert(11);
        leaf.insert(12);

        leaf.split(parent);

        parent.leafChildren.get(1).insert(9);
        parent.leafChildren.get(1).insert(8);

        parent.leafChildren.get(1).split(parent);

        parent.leafChildren.get(1).insert(5);
        parent.leafChildren.get(1).insert(4);

        parent.leafChildren.get(1).split(parent);

        //System.out.println(parent.leafChildren.get(1).keys.get(3));

        for(int x : parent.childrenSlotArr) {
            for(int y : parent.leafChildren.get(x).slotArr) {
                System.out.println(parent.leafChildren.get(x).keys.get(y));
            }
        }
    }
}
