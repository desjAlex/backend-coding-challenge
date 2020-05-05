import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * A singleton containing details for an arbitrary number of <code>City</code> objects. 
 * 
 * This class is backed by a <code>Radix Tree</code> and is protected by a Readers-Writer lock for thread safety.
 */
public class CityDirectory 
{

    
    public static void main(String[] args)
    {
        try
        {
            CityDirectory.loadFromTSV("/cities_canada-usa.tsv");
            //CityDirectory.reset();

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            QueryResponse results = CityDirectory.query("Londo", 43.70011, -79.4163);
            String jsonResponse = gson.toJson(results);
            System.out.println(jsonResponse);
        }
        catch (IOException ignored) {}
    }
    
    // Singleton instance is instantiated at compile time. 
    private static final RadixTree<City> instance = new RadixTree<>();
    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    /**
     * Add a city to the directory.
     * 
     * @param city The city to be added.
     * @return <code>true</code> if the contents of the directory changed as a result of this action, <code>false</code> otherwise.
     */
    public static boolean add(City city)
    {
        rwl.writeLock().lock();
        boolean addSuccess = instance.add(city.getFullName(), city);
        rwl.writeLock().unlock();
        return addSuccess;
    }

    /**
     * Get all cities whose names begin with a specified string.
     * 
     * @param cityName The search string. 
     * @return A List containing the matching cities.
     */
    public static List<City> getAll(String cityName)
    {
        rwl.readLock().lock();
        List<City> returnedCities = instance.getAll(cityName);
        rwl.readLock().unlock();
        return returnedCities;
    }

    /**
     * Remove a city from the directory.
     *
     * @param city The city to be removed.
     * @return <code>true</code> if the contents of the directory changed as a result of this action, <code>false</code> otherwise.
     */
    public static boolean remove(City city)
    {
        rwl.writeLock().lock();
        boolean removeSuccess = instance.remove(city.getFullName(), city);
        rwl.writeLock().unlock();
        return removeSuccess;
    }


    /**
     * Query the directory for cities whose names begin with a specified string. 
     * Results are scored based on population and sorted, then stored as objects specifically suited for JSON serialization. 
     * 
     * @param cityName The search string.
     * @return The response data as a formatted object. 
     */
    public static QueryResponse query(String cityName)
    {
        return new QueryResponse(getAll(cityName));
    }

    /**
     * Query the directory for cities whose names begin with a specified string.
     * Results are scored based on population and distance from a specified position, sorted, 
     * then stored as objects specifically suited for JSON serialization. 
     *
     * @param cityName The search string.
     * @param latitude The latitude of the query position, in degrees.
     * @param longitude The longitude of the query position, in degrees.
     * @return The response data as a formatted object. 
     */
    public static QueryResponse query(String cityName, double latitude, double longitude)
    {
        return new QueryResponse(getAll(cityName), latitude, longitude);
    }

    /**
     * Populate the directory with records loaded from a tab-separated value (.tsv) file located at the specified
     * file path.
     * 
     * @param filepath The relative location of the TSV file. 
     * @throws IOException If the file cannot be found, loaded, or parsed.
     */
    public static void loadFromTSV(String filepath) throws IOException 
    {
        try ( InputStream csvStream = CityDirectory.class.getResourceAsStream(filepath);
              BufferedReader br = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8));
              CSVParser parser = new CSVParser(br, CSVFormat.TDF.withQuote(null).withHeader()))
        {
            rwl.writeLock().lock();
            for (CSVRecord record : parser)
            {
                City newCity = parseCity(record);
                instance.add(newCity.getFullName(), newCity);
            }
        }
        finally
        {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Resets the directory by removing all cities it currently contains.
     */
    public static void reset()
    {
        rwl.writeLock().lock();
        List<City> allCities = instance.getAll("");
        for (City city : allCities)
        {
            remove(city);
        }
        rwl.writeLock().unlock();
    }
    
    private static City parseCity(CSVRecord cityRecord) throws IllegalArgumentException
    {
        // Parse a given TSV record, normalize and adjust the data representation, and return a new City object with the data.
        String name = cityRecord.get("ascii");
        String province = cityRecord.get("admin1");
        String country = cityRecord.get("country");
        double latitude = Double.parseDouble(cityRecord.get("lat"));
        double longitude = Double.parseDouble(cityRecord.get("long"));
        long population = Long.parseLong(cityRecord.get("population"));
        
        String numericRegex = "\\d+";
        if (province.matches(numericRegex))
        {
            switch (Integer.parseInt(province))
            {
                case 1: 
                    province = "AB";
                    break;
                case 2: 
                    province = "BC";
                    break;
                case 3: 
                    province = "MB";
                    break;
                case 4: 
                    province = "NB";
                    break;
                case 5: 
                    province = "NL";
                    break;
                case 7: 
                    province = "NS";
                    break;
                case 8: 
                    province = "ON";
                    break;
                case 9: 
                    province = "PE";
                    break;
                case 10: 
                    province = "QC";
                    break;
                case 11: 
                    province = "SK";
                    break;
                case 12: 
                    province = "YT";
                    break;
                case 13: 
                    province = "NT";
                    break;
                case 14: 
                    province = "NU";
            }
        }
        
        if (country.equals("US"))
        {
            country = "USA";
        }
        else if (country.equals("CA"))
        {
            country = "Canada";
        }
            
        return new City(name, province, country, latitude, longitude, population);
    }
    
    static class QueryResponse
    {
        // An object for storing the result of a query made on the directory. This object is formatted in such a way
        // as to generate neat JSON objects. 
        private final List<CityResult> suggestions;
        
        private QueryResponse(List<City> cities)
        {
            long populationSum = cities.stream().map(City::getPopulation).reduce((long) 0, Long::sum);
            
            suggestions = cities.stream()
                    .map(x -> new CityResult(x, populationSum))
                    .filter(y -> y.score >= 0.1)
                    .sorted(Collections.reverseOrder())
                    .collect(Collectors.toList());
            suggestions.forEach(CityResult::truncateScore);
        }
        
        private QueryResponse(List<City> cities, double qLatitude, double qLongitude)
        {
            suggestions = cities.stream()
                    .map(x -> new CityResult(x, qLatitude, qLongitude))
                    .filter(y -> y.score >= 0.1)
                    .sorted(Collections.reverseOrder())
                    .collect(Collectors.toList());
            suggestions.forEach(CityResult::truncateScore);
        }
    }
    
    static class CityResult implements Comparable<CityResult>
    {
        // An object for storing an individual city result of a query made on the directory. This object also contains a
        // score for a given result based on population and (optionally) geographic position. This object is formatted 
        // in such a way as to generate neat JSON objects. 
        private final String name;
        private final String latitude;
        private final String longitude;
        private double score;
        
        private CityResult(City city, double populationSum)
        {
            this.name = city.getFullName();
            this.latitude = formatDouble(city.getLatitude());
            this.longitude = formatDouble(city.getLongitude());

            this.score = populationScore(city.getPopulation(), populationSum);
        }
        
        private CityResult(City city, double qLatitude, double qLongitude)
        {
            this.name = city.getFullName();
            this.latitude = formatDouble(city.getLatitude());
            this.longitude = formatDouble(city.getLongitude());

            this.score = relevanceScore(city.distanceFrom(qLatitude, qLongitude), city.getPopulation());
        }

        @Override
        public int compareTo(CityResult other)
        {
            return Double.compare(this.score, other.score);
        }

        private void truncateScore()
        {
            score = Math.floor(score*10.0)/10.0;
        }
        
        private double populationScore(double population, double logPopulationSum)
        {
            // Computes a score for a given result based on the population of this city compared to the 
            // total population of all cities returned by the query.
            return (Math.log10(population) - 3)/(Math.log10(logPopulationSum) - 3);
        }
        
        private double relevanceScore(double distance, double population)
        {
            // Computes a score for a given result based on distance from a specified position. This score decays 
            // exponentially with distance; larger cities will decay slower than smaller ones.
            double logPop = Math.log10(population);
            double scale = Math.log(0.5)/(100.0*logPop);

            return Math.exp(scale*distance);
        }
        
        private String formatDouble(double input)
        {
            return new DecimalFormat("##.#####").format(input);
        }
    }
}
