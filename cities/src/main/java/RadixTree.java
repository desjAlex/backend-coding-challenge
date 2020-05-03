import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RadixTree<T>
{
    private final RadixNode<T> root = new RadixNode<>();
    
    public boolean add(String key, T value) throws IllegalArgumentException
    {
        key = clean(key);

        RadixNode<T> cursor = root;
        do
        {
            String radix = cursor.radix;
            
            if (key.equals(radix))
            {
                return cursor.add(value);
            }
            else if (key.startsWith(radix))
            {
                key = key.substring(radix.length());
            }
            else
            {
                int overlap = cursor.radixOverlap(key);

                key = key.substring(overlap);
                cursor.splitRadix(overlap);
            }
            
            cursor = cursor.getChild(key);
            
        } while (!key.isEmpty());
        
        return false;
    }
    
    public List<T> get(String key)
    {
        key = clean(key);
        RadixNode<T> keyMatch = getNode(key);
        
        if (keyMatch == null)
        {
            return new ArrayList<>();
        }
        else
        {
            return keyMatch.getAllValues();
        }
    }
    
    public boolean remove(String key, T value)
    {
        key = clean(key);
        RadixNode<T> keyMatch = getNode(key);
        
        if (keyMatch == null)
        {
            return false;
        }
        else
        {
            return keyMatch.tryRemove(value);
        }
    }
    
    private String clean(String dirtyKey)
    {
        return dirtyKey.toLowerCase().replaceAll("[^a-z]", " ").trim();
    }
    
    private RadixNode<T> getNode(String key)
    {
        RadixNode<T> cursor = root;
        do
        {
            String radix = cursor.radix;

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

    private static class RadixNode<T>
    {
        private RadixNode<T> parent;
        private String radix;
        private final List<T> values = new ArrayList<>();
        private List<RadixNode<T>> children = null;
        
        private RadixNode()
        {
            this.parent = null;
            this.radix = "";
        }
        
        private RadixNode(RadixNode<T> parent, String radix)
        {
            this.parent = parent;
            this.radix = radix;
        }
        
        private boolean add(T value)
        {
            if (value == null || values.contains(value))
            {
                return false;
            }
            else
            {
                values.add(value);
                return true;
            }
        }
        
        private void add(List<T> values)
        {
            for (T value : values)
            {
                this.add(value);
            }
        }
        
        private void add(String key, T value)
        {
            if (key.equals(radix))
            {
                values.add(value);
            }
            else
            {
                if (key.startsWith(radix))
                {
                    key = key.substring(radix.length());
                }
                else
                {
                    int overlap = radixOverlap(key);

                    key = key.substring(overlap);
                    splitRadix(overlap);
                }
                getChild(key).add(key, value);
            }
        }
        
        private boolean tryRemove(T value)
        {
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
                else if (childCount == 0)
                {
                    parent.removeBranch(radix);
                    parent.tryMerge();
                }
            }
        }
        
        private void mergeUp()
        {
            if (parent != null)
            {
                radix = parent.radix + radix;
                parent = parent.parent;
                parent.setChild(this);
            }
        }
        
        private List<T> getAllValues()
        {
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
            int keyIndex = getKeyIndex(key);
            children.set(keyIndex, null);
        }
        
        private void setChild(RadixNode<T> newChild)
        {
            int keyIndex = getKeyIndex(newChild.radix);
            children.set(keyIndex, newChild);
        }
        
        private RadixNode<T> getChild(String key) throws IllegalArgumentException
        {
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
        
        private int getKeyIndex(String key) throws IllegalArgumentException
        {
            if (key.isEmpty())
            {
                throw new IllegalArgumentException();
            }
            
            int keyIndex;
            char keyChar = key.charAt(0);

            if (keyChar == ' ')
            {
                keyIndex = 26;
            }
            else
            {
                keyIndex = keyChar - 'a';
            }

            if (keyIndex < 0 || keyIndex > 26)
            {
                throw new IllegalArgumentException();
            }
            
            return keyIndex;
        }
        
        private void splitRadix(int length) throws IllegalArgumentException
        {
            if (length <= 0 || length >= radix.length())
            {
                throw new IllegalArgumentException();
            }
            
            String newRadix = radix.substring(0, length);
            String newKey = radix.substring(length);

            List<RadixNode<T>> prevChildren = children;
            children = null;
            RadixNode<T> splitChild = getChild(newKey);
            splitChild.setChildren(prevChildren);
            splitChild.add(values);
            values.clear();
            
            this.radix = newRadix;
        }
        
        private int radixOverlap(String key)
        {
            int index = 0;
            
            while (index < radix.length() && index < key.length())
            {
                if (radix.charAt(index) != key.charAt(index))
                {
                    break;
                }
                
                index++;
            }
            
            return index;
        }
        
        private int countChildren()
        {
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
        
        private void setChildren(List<RadixNode<T>> newChildren)
        {
            children = newChildren;
        }
        
        private void lazyInitChildren()
        {
            if (children == null)
            {
                children = new ArrayList<>(Collections.nCopies(27, null));
            }
        }
    }
}
