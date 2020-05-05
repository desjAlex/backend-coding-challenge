import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "main.java.CitySuggestionServlet")
public class CitySuggestionServlet extends HttpServlet 
{
    public void init()
    {
        try
        {
            CityDirectory.loadFromTSV("/cities_canada-usa.tsv");
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException 
    {
        PrintWriter out = response.getWriter();
        response.setCharacterEncoding("UTF-8");
        
        String queryCity = request.getParameter("q");
        String queryLatitude = request.getParameter("latitude");
        String queryLongitude = request.getParameter("longitude");

        CityDirectory.QueryResponse qResponse = null;
        String responseBody = "";
        if (queryCity == null)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        else if (queryLatitude != null && !queryLatitude.isEmpty() && queryLongitude != null && !queryLongitude.isEmpty())
        {
            try 
            {
                double latitude = Double.parseDouble(queryLatitude);
                double longitude = Double.parseDouble(queryLongitude);
                if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180)
                {
                    response.setStatus(422);
                }
                else
                {
                    qResponse = CityDirectory.query(queryCity, latitude, longitude);
                }
            }
            catch(NumberFormatException ignored) 
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
        else
        {
            qResponse = CityDirectory.query(queryCity);
        }
        
        if (qResponse != null)
        {
            response.setContentType("application/json");
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            responseBody = gson.toJson(qResponse);
        }
        
        out.print(responseBody);
        out.flush();
    }
}
