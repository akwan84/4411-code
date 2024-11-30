import java.util.*;

public class SortedInternalNode {

    public static final int MAX = 4;

    List<Integer> keys;

    List<SortedInternalNode> internalChildren;
    List<LeafNode> leafChildren;

    public SortedInternalNode(){
        keys = new ArrayList<>();
        internalChildren = new ArrayList<>();
        leafChildren = new ArrayList<>();
    }

    public void insert(int key) {
        //convert to a binary search later
        int i = keys.size() - 1;

        while(i >= 0 && keys.get(i) > key) { //no NVM reads because of slot array use
            i--;
        }
        i++;

        if(leafChildren.size() != 0) {
            if(leafChildren.get(i).keys.size() == MAX) { 
                leafChildren.get(i).split(this);
                
                i = keys.size() - 1;

                while(i >= 0 && keys.get(i) > key) {
                    i--;
                }
                i++;
            }
            leafChildren.get(i).insert(key); //1 read
        }else{
            if(internalChildren.get(i).keys.size() == MAX) { //1 read
                internalChildren.get(i).split(this); //1 read
                
                i = keys.size() - 1;

                while(i >= 0 && keys.get(i) > key) {
                    i--;
                }
                i++;
            }
            internalChildren.get(i).insert(key); //1 read
        }
    }

    public void split(SortedInternalNode parent) {
        //create the new node
        SortedInternalNode newNode = new SortedInternalNode(); //assuming this is being created and stored in NVM

        int promote = keys.get(MAX/2);

        //put the larger half of the keys into the new node
        for(int i = MAX/2 + 1; i < MAX; i++) {
            newNode.keys.add(keys.get(i));
        }

        //restructure the smaller half of the keys
        List<Integer> newKeys = new ArrayList<>();

        for(int i = 0; i < MAX/2; i++) {
            newKeys.add(keys.get(i));
        }

        keys = newKeys;

        //split the pointers
        if(leafChildren.size() != 0) { //this internal node had leaf children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.leafChildren.add(leafChildren.get(i)); //1 write
            }

            //restructure the smaller half of the keys
            List<LeafNode> newChildren = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(leafChildren.get(i));
            }

            leafChildren = newChildren;
        } else { //internal node had internal children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.internalChildren.add(internalChildren.get(i)); //1 write
            }

            //restructure the smaller half of the keys
            List<SortedInternalNode> newChildren = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(internalChildren.get(i));
            }

            internalChildren = newChildren;
        }

        //key promotion
        if(parent.keys.size() == 0) { //parent is a brand new internal node
            parent.keys.add(promote);

            parent.internalChildren.add(this); //1 write
            parent.internalChildren.add(newNode); //1 write

        } else { //parent already has this node, need to find the right insertion point for newNode

            int l = 0, r = parent.keys.size();
            while (l < r) {
                int mid = (l + r) / 2;
                if (parent.keys.get(mid) < promote) { //1 read
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
                WbTree.numReads++;
            }

            parent.keys.add(l, promote); //1 write

            
            //find the right spot for newNode in the parent
            l = 0; 
            r = parent.internalChildren.size();
            while (l < r) {
                int mid = (l + r) / 2;
                SortedInternalNode searchNode = parent.internalChildren.get(mid); //1 read
                if (searchNode.keys.get(searchNode.keys.size() - 1) < promote) {
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
            }

            parent.internalChildren.add(l, newNode);
        }
    }

    public static void main(String[] args) {
        
    }
}
