import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadixTreeTest
{
    @Test
    void addAlphaKeys()
    {
        RadixTree<String> tree = new RadixTree<>();
        for (int i = 0; i < 1000; i++)
        {
            String tString = randomString('a', 'z', 1, 20);
            tree.add(tString, tString);
            assertTrue(tree.contains(tString, tString), "Key: " + tString);
        }
    }
    
    @Test
    void addAsciiKeys()
    {
        RadixTree<String> tree = new RadixTree<>();
        for (int i = 0; i < 1000; i++)
        {
            String tString = randomString(32, 126, 1, 20);
            tree.add(tString, tString);
            assertTrue(tree.contains(tString, tString), "Key: " + tString);
        }
    }

    @Test
    void addThrowsExceptions()
    {
        RadixTree<String> tree = new RadixTree<>();
        assertThrows(NullPointerException.class, () -> tree.add(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> tree.add("key", null));
    }

    @Test
    void containsThrowsExceptions()
    {
        RadixTree<String> tree = new RadixTree<>();
        assertThrows(NullPointerException.class, () -> tree.contains(null, "value"));
    }

    @Test
    void removeThrowsExceptions()
    {
        RadixTree<String> tree = new RadixTree<>();
        assertThrows(NullPointerException.class, () -> tree.remove(null, "value"));
    }

    @Test
    void getAllThrowsExceptions()
    {
        RadixTree<String> tree = new RadixTree<>();
        assertThrows(NullPointerException.class, () -> tree.getAll(null));
    }
    
    @Test
    void addAndRemove()
    {
        RadixTree<String> tree = new RadixTree<>();
        String[] tStrings = new String[1000];
        for (int i = 0; i < 1000; i++)
        {
            String tString = randomString(32, 126, 1, 20);
            tStrings[i] = tString;
            tree.add(tString, tString);
        }
        
        for (int j = 0; j < 1000; j++)
        {
            boolean stillContains = tree.contains(tStrings[j], tStrings[j]);
            assertEquals(stillContains, tree.remove(tStrings[j], tStrings[j]), "Key: " + tStrings[j]);
        }
        
        assertEquals(0, tree.getAll("").size());
    }

    @Test
    void addAndRemoveDuplication()
    {
        RadixTree<String> tree = new RadixTree<>();
        for (int i = 0; i < 1000; i++)
        {
            String tString = randomString('a', 'z', 1, 20);
            assertTrue(tree.add(tString, tString), "Key: " + tString);
            assertTrue(tree.contains(tString, tString), "Key: " + tString);
            assertFalse(tree.add(tString, tString), "Key: " + tString);
            assertTrue(tree.contains(tString, tString), "Key: " + tString);

            assertTrue(tree.remove(tString, tString), "Key: " + tString);
            assertFalse(tree.contains(tString, tString), "Key: " + tString);
            assertFalse(tree.remove(tString, tString), "Key: " + tString);
            assertFalse(tree.contains(tString, tString), "Key: " + tString);
        }
    }
    
    @Test
    void getAll()
    {
        RadixTree<String> tree = new RadixTree<>();
        ArrayList<String> tStrings = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++)
        {
            String tString = randomString(32, 126, 1, 20);
            tStrings.add(tString);
            tree.add(tString, tString);
        }

        List<String> getStrings = tree.getAll("");
        for (String tString : tStrings)
        {
            assertTrue(getStrings.contains(tString), "Key: " + tString);
        }
    }

    @Test
    void getWithSubstring()
    {
        RadixTree<String> tree = new RadixTree<>();
        ArrayList<String> tStrings = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++)
        {
            String tString = randomString(32, 126, 20, 21);
            tStrings.add(tString);
            tree.add(tString, tString);
        }
        
        for (int j = 0; j < 1000; j++)
        {
            int subLength = ThreadLocalRandom.current().nextInt(10);
            String substring = tStrings.get(j).substring(0, subLength);
            assertTrue(tree.getAll(substring).contains(tStrings.get(j)));
        }
    }

    @Test
    void iterator()
    {
        RadixTree<String> tree = new RadixTree<>();
        for (int i = 0; i < 1000; i++)
        {
            String tString = randomString(32, 126, 1, 20);
            tree.add(tString, tString);
        }

        List<String> getStrings = tree.getAll("");
        int index = 0;
        for (String tValue : tree)
        {
            assertEquals(tValue, getStrings.get(index), "Key: " + tValue);
            index++;
        }
    }
    
    private String randomString(int from, int to, int minLength, int maxLength)
    {
        // Generates a random string containing characters ranging between the provided limits (ascii value, inclusive)
        // Length ranges between minLength (inclusive) and maxLength (exclusive)
        assert from < to;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        
        int length = rng.nextInt(minLength, maxLength);
        char[] workingString = new char[length];
        
        for (int i = 0; i < length; i++)
        {
            workingString[i] = (char)rng.nextInt(from, to + 1);
        }
        
        return new String(workingString);
    }
}