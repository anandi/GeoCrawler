/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;
import java.util.Vector;
import net.oauth.j2me.OAuthParameterEncoder;

/**
 *
 * @author anandi
 * This is a utility class that provides for a geo-coding interface.
 */
public class GeoCoder {
    private static String YAHOO_GEO_CODER_URL="http://local.yahooapis.com/MapsService/V1/geocode?appid=";

    //Returns a Vector of LocationData. Check for null!
    public static Vector addressToLocation(Hashtable param) {
        //I am borrowing the URL encoder from the OAuth JAR. Wish Java had a
        //independent one in HTTPUtil library!
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();
        String url = GeoCoder.YAHOO_GEO_CODER_URL + GeoCrawlerKey.YAHOO_REST_MAP_KEY;

        Enumeration keys = param.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String value = (String)param.get(key);
            url = url.concat("&"+key+"=").concat(encoder.encode(value));
        }

        //Now make the query and get back the data...
//        System.err.println("GeoCoder: GET "+url);
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(url);
        } catch (IOException iox) {
            return null;
        }
        if (response == null)
            return null;

        //Response looks something like:
        /*<ResultSet xsi:schemaLocation="urn:yahoo:maps http://api.local.yahoo.com/MapsService/V1/GeocodeResponse.xsd">
             <Result precision="zip">
                 <Latitude>12.933220</Latitude>
                 <Longitude>77.621910</Longitude>
                 <Address/>
                 <City>Koramangala, Bangalore, Karnataka</City>
                 <State>India</State>
                 <Zip/>
                 <Country>IN</Country>
             </Result>
         </ResultSet>*/
        //Since SAX fails miserably on the simulator, I use naive parse! This is
        //messy. The best I can say is: trust me!
//        System.err.println("GeoCoder: Response: "+response);
        int start = response.indexOf("<Latitude>", 0);
        Vector locations = new Vector();
        while (start != -1) {
            start += 10; //strlen("<Latitude>");
            int end = response.indexOf("</Latitude>", start);
            if (end == -1)
                break;
            String latStr = response.substring(start, end);
            start = response.indexOf("<Longitude>", end + 11);
            if (start == -1)
                break;
            start += 11;
            end = response.indexOf("</Longitude>", start);
            if (end == -1)
                break;
            String lonStr = response.substring(start, end);

            start = response.indexOf("<Latitude>", end + 12);

            //Check if these are parsable!
            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                
                locations.addElement(new LocationData(lat, lon, 0));
            } catch (Exception e) {
                continue;
            }
        }

        if (locations.size() == 0)
            return null;
        return locations;
    }
}
