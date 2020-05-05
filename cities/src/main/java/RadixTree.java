import java.util.*;

/**
 * A search tree data structure that exclusively uses <code>Strings</code> as keys.
 * 
 * A Radix Tree is a space-optimized Prefix Tree (Trie). 
 * 
 * Keys are used to traverse the tree, and values are stored in the terminal node of the search. Each node 
 * contains a prefix string, a "bucket" for storing values, and an array of pointers to the children of that node. This 
 * array has a specific capacity equal to the number of unique permitted characters in a key. In this case, the 26 
 * characters of the alphabet, plus the space character. 
 * 
 * Traversal begins at the root node, which contains an empty prefix "". The first character in the key determines which 
 * of the children to expand. The child node will contain a non-empty prefix beginning with the character corresponding
 * to its position. If the search key begins with the prefix at this node, this prefix is removed from the search key
 * and search proceeds deeper based on the first character of the shortened string; otherwise, the key does not exist
 * in the tree and the search terminates. 
 * 
 * Tree operations occur in O(n) time, where n corresponds to the number of characters in the key. In most instances,
 * actual operations are quicker since nodes often contain a prefix spanning several characters.
 * 
 * This data structure is well-suited for searching by prefixes. All keys beginning with a given prefix will share
 * a parent node that matches that prefix. 
 * 
 * In this implementation, different values may share a key, but key-value pairs must be unique. Null is not permitted
 * as either a key or a value.
 * 
 * @param <T> The type of the value stored in this Radix Tree
 * @see <a href="https://en.wikipedia.org/wiki/Trie">Wikipedia article on Prefix Trees</a>
 */
public class RadixTree<T> implements Iterable<T>
{
    private final RadixNode<T> root;
    
    public RadixTree()
    {
        root = new RadixNode<>();
    }

    /**
     * Add a value to the tree with the specified key.
     * 
     * @param key The <code>String</code> used to locate the value in the tree.
     * @param value The value to be stored.
     * @return <code>true</code> if the contents of the tree are changed based on this action, <code>false</code> otherwise.
     * @throws IllegalArgumentException If a <code>null</code> value is provided
     * @throws NullPointerException If <code>null</code> is provided as the key.
     */
    public boolean add(String key, T value) throws IllegalArgumentException, NullPointerException
    {
        key = clean(key);

        RadixNode<T> cursor = root;
        do
        {
            String prefix = cursor.prefix;
            
            if (key.equals(prefix))
            {
                return cursor.add(value);
            }
            else if (key.startsWith(prefix))
            {
                key = key.substring(prefix.length());
                cursor = cursor.getChild(key);
            }
            else
            {
                int overlap = cursor.radixOverlap(key);
                cursor = cursor.split(overlap);
            }
            
        } while (!key.isEmpty());
        
        return false;
    }

    /**
     * Find all values whose keys begin with the provided key. Results will be returned in alphabetical order.
     * 
     * @param key The <code>String</code> used to locate the values in the tree.
     * @return A list containing the values, empty if none were found. 
     * @throws NullPointerException If <code>null</code> is provided as the key.
     */
    public List<T> getAll(String key) throws NullPointerException
    {
        key = clean(key);
        RadixNode<T> keyMatch = getNodePartial(key);
        
        if (keyMatch == null)
        {
            return new ArrayList<>();
        }
        else
        {
            return keyMatch.getAllValues();
        }
    }

    /**
     * Detects whether a specified value is contained in the tree with a certain key.
     *
     * @param key The <code>String</code> used to locate the value in the tree.
     * @param value The value in question.
     * @return <code>true</code> if the value is found at this position, <code>false</code> otherwise.
     * @throws NullPointerException If <code>null</code> is provided as the key.
     */
    public boolean contains(String key, T value) throws NullPointerException
    {
        key = clean(key);
        RadixNode<T> keyMatch = getNodeExact(key);

        if (keyMatch == null)
        {
            return false;
        }
        else
        {
            return keyMatch.contains(value);
        }
    }

    /**
     * Removes the matching value in the tree found at the given key, if it exists.
     * 
     * @param key The <code>String</code> used to locate the value in the tree.
     * @param value The value to be removed.
     * @return <code>true</code> if the contents of the tree are changed based on this action, <code>false</code> otherwise.
     * @throws NullPointerException If <code>null</code> is provided as the key.
     */
    public boolean remove(String key, T value) throws NullPointerException
    {
        key = clean(key);
        RadixNode<T> keyMatch = getNodePartial(key);
        
        if (keyMatch == null)
        {
            return false;
        }
        else
        {
            return keyMatch.tryRemove(value);
        }
    }

    /**
     * Returns an iterator over the values in this tree in alphabetical order. 
     * Behaviour is undefined under concurrent modification of the data structure. 
     * 
     * @return An iterator over the values in this tree.
     */
    @Override
    public Iterator<T> iterator()
    {
        return new RadixIterator();
    }
    
    private String clean(String dirtyKey) throws NullPointerException
    {
        // Normalize case, remove all illegal characters, and trim whitespace. 
        return dirtyKey.toLowerCase().replaceAll("[^a-z]", " ").trim();
    }
    
    private RadixNode<T> getNodePartial(String key) throws NullPointerException
    {
        // Returns the node matching the provided key, or null if one doesn't exist.
        // Matches can be partial, as in the node may only start with the search key, it needs not be a full match.
        // This is useful for finding all entries for a partial search key.
        RadixNode<T> cursor = root;
        do
        {
            String radix = cursor.prefix;

            if (key.equals(radix) || radix.startsWith(key))
            {
                return cursor;
            }
            else if (key.startsWith(radix))
            {
                key = key.substring(radix.length());
                cursor = cursor.getChild(key);
            }
            else
            {
                break;
            }
        } while (!key.isEmpty());
        
        return null;
    }

    private RadixNode<T> getNodeExact(String key) throws NullPointerException
    {
        // Returns the node matching the provided key, or null if one doesn't exist.
        // Matches must be complete, the search key must exactly match a node.
        RadixNode<T> cursor = root;
        do
        {
            String radix = cursor.prefix;

            if (key.equals(radix))
            {
                return cursor;
            }
            else if (key.startsWith(radix))
            {
                key = key.substring(radix.length());
                cursor = cursor.getChild(key);
            }
            else
            {
                break;
            }
        } while (!key.isEmpty());

        return null;
    }

    private class RadixIterator implements Iterator<T>
    {
        /* This iterator advances itself ahead of time and keeps track of the last value it encountered.
           Since finding the next value is not trivial, this ensures that hasNext() can operate quickly, without having 
           to traverse the structure as well. 
        */
        private RadixNode<T> cursor = root;
        private int valueIndex = 0;
        private int childIndex = 0;
    
        private T next = null;
        private boolean hasNext = true;
    
        private RadixIterator()
        {
            advance();
        }
        
        @Override
        public boolean hasNext()
        {
            return hasNext;
        }
    
        @Override
        public T next() throws NoSuchElementException
        {
            if (hasNext)
            {
                T nextValue = next;
                advance();
                return nextValue;
            }
            else
            {
                throw new NoSuchElementException();
            }
        }
    
        private void advance()
        {
            // Update the position of the iterator in preparation for the next iteration.
            while(cursor != root || childIndex < 27)
            {
                if (childIndex == 0 && valueIndex < cursor.values.size())
                {
                    next = cursor.values.get(valueIndex);
                    valueIndex++;
                    return;
                }
                else
                {
                    valueIndex = 0;
                    
                    if (childIndex < 27)
                    {
                        if (cursor.countChildren() > 0)
                        {
                            for (; childIndex < 27; childIndex++)
                            {
                                if (cursor.children.get(childIndex) != null)
                                {
                                    cursor = cursor.children.get(childIndex);
                                    childIndex = 0;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            childIndex = 27;
                        }
                    }
                    else
                    {
                        childIndex = RadixNode.getKeyIndex(cursor.prefix) + 1;
                        cursor = cursor.parent;
                    }
                }
            }
            
            hasNext = false;
        }
    }
    
    private static class RadixNode<T>
    {
        // The node class of which the Radix Tree is comprised. 
        // Nodes are "lazy initialized" in that the array of children is not instantiated until the first child is 
        // added. This is a space optimization to prevent each leaf node (of which there may be many) from containing 27 
        // pointers that they don't need. 
        private RadixNode<T> parent;
        private String prefix;
        private final List<T> values = new ArrayList<>();
        private List<RadixNode<T>> children = null;
        
        private RadixNode()
        {
            this.parent = null;
            this.prefix = "";
        }
        
        private RadixNode(RadixNode<T> parent, String prefix)
        {
            this.parent = parent;
            this.prefix = prefix;
        }
        
        private boolean add(T value) throws IllegalArgumentException
        {
            // The value cannot be null, nor can it already exist in this node. 
            if (value == null)
            {
                throw new IllegalArgumentException();
            }
            else if (values.contains(value))
            {
                return false;
            }
            else
            {
                values.add(value);
                return true;
            }
        }
        
        private boolean contains(T value)
        {
            return values.contains(value);
            
        }
        
        private boolean tryRemove(T value)
        {
            // If the value is stored in this node, remove it.
            if (values.remove(value))
            {
                tryMerge();
                return true;
            }
            else
            {
                return false;
            }
        }
        
        private void tryMerge()
        {
            // When a value is removed, nodes can be merged under certain circumstances. 
            // If no values remain and those node has exactly one child, this node can be merged with its child.
            // If no values remain and this node has zero children, this node can be removed. 
            //      If the parent node is then left with no values and exactly one child, it can be merged with that child.
            if (values.size() == 0)
            {
                int childCount = countChildren();
                if (childCount == 1)
                {
                    for (RadixNode<T> child : children)
                    {
                        if (child != null)
                        {
                            child.mergeUp();
                            break;
                        }
                    }
                }
                else if (childCount == 0 && parent != null)
                {
                    parent.removeBranch(prefix);
                    parent.tryMerge();
                }
            }
        }
        
        private void mergeUp()
        {
            // Merge this node into its parent, replacing the parent node object.
            // Merge occurs in this direction since only one child pointer has to be updated, instead of updating the
            // parent pointer of all children if merging occurred downward.
            if (parent != null && parent.parent != null)
            {
                prefix = parent.prefix + prefix;
                parent = parent.parent;
                parent.setChild(this);
            }
        }
        
        private List<T> getAllValues()
        {
            // Recursively gather all values in nodes that are descendents of this one. 
            List<T> rValues = new ArrayList<>(values);
            
            if (children != null)
            {
                for (RadixNode<T> child : children)
                {
                    if (child != null)
                    {
                        rValues.addAll(child.getAllValues());
                    }
                }
            }
            
            return rValues;
        }
        
        private void removeBranch(String key) throws IllegalArgumentException
        {
            // Remove the child of this node that corresponds to the provided key.
            int keyIndex = getKeyIndex(key);
            if (children != null)
            {
                children.set(keyIndex, null);
            }
        }
        
        private void setChild(RadixNode<T> newChild) throws IllegalArgumentException
        {
            // Overwrite the child of this node corresponding to the given node.
            int keyIndex = getKeyIndex(newChild.prefix);
            lazyInitChildren();
            children.set(keyIndex, newChild);
        }
        
        private RadixNode<T> getChild(String key) throws IllegalArgumentException
        {
            // Return the child of this node corresponding to the given key.
            // Create it if it doesn't yet exist.
            int keyIndex = getKeyIndex(key);
            
            lazyInitChildren();
            RadixNode<T> child = children.get(keyIndex);
            
            if (child == null)
            {
                child = new RadixNode<>(this, key);
                children.set(keyIndex, child);
            }
            
            return child;
        }
        
        private RadixNode<T> split(int length) throws IllegalArgumentException
        {
            // Split this node by truncating its prefix at the specified length, creating a new node with this prefix
            // and pushing the current node down as its child with the removed string segment as its new prefix. 
            if (length <= 0 || length >= prefix.length())
            {
                throw new IllegalArgumentException();
            }
            
            String newRadix = prefix.substring(0, length);
            String newKey = prefix.substring(length);
            
            RadixNode<T> splitParent = new RadixNode<>(parent, newRadix);
            this.prefix = newKey;

            parent.setChild(splitParent);
            splitParent.setChild(this);
            this.parent = splitParent;
            
            return splitParent;
        }
        
        private int radixOverlap(String key)
        {
            // Return the number of overlapping characters between this nodes prefix and the given key.
            int index = 0;
            
            while (index < prefix.length() && index < key.length())
            {
                if (prefix.charAt(index) != key.charAt(index))
                {
                    break;
                }
                
                index++;
            }
            
            return index;
        }
        
        private int countChildren()
        {
            // Determine how many children currently exist for this node.
            int childCount = 0;
            
            if (children != null)
            {
                for (RadixNode<T> child : children)
                {
                    if (child != null)
                    {
                        childCount++;
                    }
                }
            }
            
            return childCount;
        }
        
        private void lazyInitChildren()
        {
            // Initialize the array of children if it doesn't yet exist. 
            if (children == null)
            {
                children = new ArrayList<>(Collections.nCopies(27, null));
            }
        }

        private static int getKeyIndex(String key) throws IllegalArgumentException
        {
            // Identify the array index matching the provided key.
            if (key.isEmpty())
            {
                throw new IllegalArgumentException();
            }

            int keyIndex;
            char keyChar = key.charAt(0);

            if (keyChar == ' ')
            {
                keyIndex = 0;
            }
            else
            {
                keyIndex = keyChar - 'a' + 1;
            }

            if (keyIndex < 0 || keyIndex > 26)
            {
                throw new IllegalArgumentException();
            }

            return keyIndex;
        }
    }
}
