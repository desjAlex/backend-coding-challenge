import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class CityDirectoryTest
{
    @BeforeEach
    void init()
    {
        CityDirectory.reset();
    }
    
    @Test
    void addAndRemove()
    {
        City testCity = randomCity();
        
        assertTrue(CityDirectory.add(testCity));
        assertFalse(CityDirectory.add(testCity));

        assertTrue(CityDirectory.remove(testCity));
        assertFalse(CityDirectory.remove(testCity));
    }

    @Test
    void getAll()
    {
        ArrayList<City> testCities = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++)
        {
            City testCity = randomCity();
            testCities.add(testCity);
            CityDirectory.add(testCity);
        }

        List<City> getCities = CityDirectory.getAll("");
        for (City city : testCities)
        {
            assertTrue(getCities.contains(city));
        }
    }

    @Test
    void loadFromTSV() throws IOException
    {
        CityDirectory.loadFromTSV("/test_cities.tsv");
        assertEquals(7237, CityDirectory.getAll("").size());
    }

    @Test
    void reset()
    {
        for (int i = 0; i < 1000; i++)
        {
            City testCity = randomCity();
            CityDirectory.add(testCity);
        }
        
        CityDirectory.reset();
        assertEquals(0, CityDirectory.getAll("").size());
    }

    private City randomCity()
    {
        // Generates a city object with random (but valid) field values.
        String name = randomString('a', 'z', 3, 20);
        String province = randomString('a', 'z', 2, 3);
        String country = randomString('a', 'z', 3, 20);
        double latitude = ThreadLocalRandom.current().nextDouble(-90.0, 90.0);
        double longitude = ThreadLocalRandom.current().nextDouble(-180.0, 180.0);
        long population = ThreadLocalRandom.current().nextLong(1000000000);
        
        return new City(name, province, country, latitude, longitude, population);
    }
    
    private String randomString(int from, int to, int minLength, int maxLength)
    {
        // Generates a random string containing characters ranging between the provided limits (ascii , inclusive)
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