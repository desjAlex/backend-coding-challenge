public class City 
{
    private static final double AVG_EARTH_RADIUS = 6371.0;
    private static final double EPS_DISTANCE = 1.0;
    
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
    
    public double distanceFrom(double latitude, double longitude)
    {
        double lat1 = degToRad(this.latitude);
        double lat2 = degToRad(latitude);
        double deltaLat = lat2 - lat1;
        double deltaLong = degToRad(longitude - this.longitude);
        
        double hav = haversine(deltaLat) + Math.cos(lat1)*Math.cos(lat2)*haversine(deltaLong);
        
        return AVG_EARTH_RADIUS*arcHaversine(hav);
    }
    
    public double distanceFrom(City other)
    {
        return distanceFrom(other.latitude, other.longitude);
    }
    
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
