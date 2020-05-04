/**
 * An immutable structure for storing details and properties of cities.
 * Functionality for computing the distance from any longitude and latitude, or from another City, is provided. 
 */
public class City 
{
    private static final double AVG_EARTH_RADIUS_IN_KM = 6371.0;
    private static final double EPS_DISTANCE = 1.0; // Distance within which two cities with identical identifiers (name, province, country) will be considered "equal"
    
    private final String name;
    private final String province;
    private final String country;
    private final double latitude;
    private final double longitude;
    private final long population;
    
    public City(String name, String province, String country, double latitude, double longitude, long population)
    {
        this.name = name;
        this.province = province;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.population = population;
    }

    /**
     * Combines name, province (or state), and country together to form a single, complete identifier.
     * 
     * @return The <code>String</code> representing the unified name.
     */
    public String getFullName()
    {
        return String.join(", ", new String[]{name, province, country});
    }
    
    public double getLatitude()
    {
        return latitude;
    }
    
    public double getLongitude()
    {
        return longitude;
    }
    
    public long getPopulation()
    {
        return population;
    }


    /**
     * Computes the great-circle distance to this City from a position specified by a longitude and latitude.
     * 
     * Great-circle distance is the shortest distance between two points on the surface of a sphere. 
     * This is an implementation of the haversine formula.
     * 
     * @param latitude The latitude of the position in degrees.
     * @param longitude The longitude of the position in degrees.
     * @return Distance between this city and the given position in km.
     * @see <a href="https://en.wikipedia.org/wiki/Great-circle_distance">Wikipedia article on Great-circle Distance</a>
     */
    public double distanceFrom(double latitude, double longitude)
    {
        double lat1 = degToRad(this.latitude);
        double lat2 = degToRad(latitude);
        double deltaLat = lat2 - lat1;
        double deltaLong = degToRad(longitude - this.longitude);
        
        double hav = haversine(deltaLat) + Math.cos(lat1)*Math.cos(lat2)*haversine(deltaLong);
        
        return AVG_EARTH_RADIUS_IN_KM*arcHaversine(hav);
    }

    /**
     * Computes the great-circle distance to this City from another one.
     * 
     * See {@link #distanceFrom(double, double)} for more details.
     * 
     * @param other The City from which to compute the distance.
     * @return Distance between this city and the given one in km.
     */
    public double distanceFrom(City other)
    {
        return distanceFrom(other.latitude, other.longitude);
    }

    /**
     * Indicates whether another City object is "equal" to this one.
     * 
     * This implementation allows for slight inaccuracies in position. However, as a consequence, it no longer
     * satisfies the transitivity property. 
     * 
     * @param other The other City for which the comparison is to be made.
     * @return <code>true</code>, if the cities are "equal", otherwise <code>false</code>.
     * @see Object#equals
     */
    public boolean equals(City other)
    {
        if (other == this)
        {
            return true;
        }
        else if (other == null)
        {
            return false;
        }
        else
        {
            return  this.name.equals(other.name) &&
                    this.province.equals(other.province) &&
                    this.country.equals(other.country) &&
                    distanceFrom(other) < EPS_DISTANCE;
        }
    }
    
    private double haversine(double radians)
    {
        return Math.sin(radians/2.0)*Math.sin(radians/2.0);
    }
    
    private double arcHaversine(double havAngle)
    {
        return 2.0*Math.asin(Math.min(1.0, Math.sqrt(havAngle)));
    }
    
    private double degToRad(double degrees)
    {
        return (Math.PI/180.0)*degrees;
    }
}
