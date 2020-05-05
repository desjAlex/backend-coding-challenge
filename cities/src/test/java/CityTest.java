import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityTest
{
    @Test
    void distanceFrom()
    {
        City cityA = new City("a", "b", "c", 27.0, 35.0, 1);
        City cityB = new City("a", "b", "c", -27.0, 90.0, 1);
        
        assertEquals(cityA.distanceFrom(cityA), 0.0, 0.01);
        assertEquals(cityA.distanceFrom(cityB), cityB.distanceFrom(cityA), 0.01);
    }
}