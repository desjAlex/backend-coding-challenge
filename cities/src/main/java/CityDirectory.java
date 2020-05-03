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
import java.util.stream.Collectors;

public class CityDirectory 
{
    private static final RadixTree<City> instance = new RadixTree<>();
    
    public static boolean add(City newCity)
    {
        return instance.add(newCity.getFullName(), newCity);
    }
    
    public static List<City> getAll(String cityName)
    {
        return instance.get(cityName);
    }
    
    public static boolean remove(City city)
    {
        return instance.remove(city.getFullName(), city);
    }

    public static QueryResponse query(String cityName)
    {
        return new QueryResponse(getAll(cityName));
    }
    
    public static QueryResponse query(String cityName, double latitude, double longitude)
    {
        return new QueryResponse(getAll(cityName), latitude, longitude);
    }
    
    public static void loadFromTSV(String filepath) throws IOException 
    {
        try ( InputStream csvStream = CityDirectory.class.getResourceAsStream(filepath);
              BufferedReader br = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8));
              CSVParser parser = new CSVParser(br, CSVFormat.TDF.withQuote(null).withHeader()))
        {
            for (CSVRecord record : parser)
            {
                City newCity = parseCity(record);
                instance.add(newCity.getFullName(), newCity);
            }
        }
    }
    
    private static City parseCity(CSVRecord cityRecord) throws IllegalArgumentException
    {
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
    
    public static void main(String[] args)
    {
        try
        {
            CityDirectory.loadFromTSV("/cities_canada-usa.tsv");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonResponse = gson.toJson(CityDirectory.query("Lond"));//, 43.70011, -79.4163));
            System.out.println(jsonResponse);
        }
        catch (IOException ignored) {}
    }
    
    static class QueryResponse
    {
        private final List<CityResult> suggestions;
        
        public QueryResponse(List<City> cities)
        {
            long populationSum = cities.stream().map(City::getPopulation).reduce((long) 0, Long::sum);
            
            suggestions = cities.stream().map(x -> new CityResult(x, populationSum))
                                         .filter(y -> y.score >= 0.1)
                                         .collect(Collectors.toList());
            
            suggestions.sort(Collections.reverseOrder());
            suggestions.forEach(CityResult::roundScore);
        }
        
        public QueryResponse(List<City> cities, double qLatitude, double qLongitude)
        {
            suggestions = cities.stream().map(x -> new CityResult(x, qLatitude, qLongitude))
                                         .filter(y -> y.score >= 0.1)
                                         .collect(Collectors.toList());
            suggestions.sort(Collections.reverseOrder());
            suggestions.forEach(CityResult::roundScore);
        }
    }
    
    static class CityResult implements Comparable<CityResult>
    {
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

        public int compareTo(CityResult other)
        {
            return Double.compare(this.score, other.score);
        }

        private void roundScore()
        {
            score = Math.round(score*10.0)/10.0;
        }
        
        private double populationScore(double population, double logPopulationSum)
        {
            return (Math.log10(population) - 3)/(Math.log10(logPopulationSum) - 3);
        }
        
        private double relevanceScore(double distance, double population)
        {
            double logPop = Math.log10(population);
            double scale = Math.log(0.4)/(100.0*logPop);

            return Math.exp(scale*distance);
        }
        
        private String formatDouble(double input)
        {
            return new DecimalFormat("##.#####").format(input);
        }
    }
}
