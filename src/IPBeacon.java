/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;
import org.json.me.JSONObject;
import org.json.me.JSONException;

/**
 *
 * @author anandi
 */
public class IPBeacon extends LocationBeacon {
    private static String IP_INFO_DB_URL = "http://ipinfodb.com/ip_query.php?output=json";

    public IPBeacon() {
        this.errorInMeter = 6000; //Should actually be meaningfully set by resolve!
    }

    public boolean initialze() {
        //Nothing to initialize.
        return true;
    }

    /* Expected response is like:
     { "Ip" : "59.162.228.158",
       "Status" : "OK",
       "CountryCode" : "IN",
       "CountryName" : "India",
       "RegionCode" : "19",
       "RegionName" : "Karnataka",
       "City" : "Bangalore",
       "ZipPostalCode" : "",
       "Latitude" : "12.9833",
       "Longitude" : "77.5833",
       "Gmtoffset" : "5.5",
       "Dstoffset" : "5.5" } */
    public boolean update() {
        //Access the web service and get back a JSON response...
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(IPBeacon.IP_INFO_DB_URL);
        } catch (IOException iox) {
            System.err.println("IP lookup query failed.");
            return false;
        }
        if (response == null)
            return false;

        try {
            JSONObject parsedResponse = new JSONObject(response);
            if (!parsedResponse.getString("Status").equals("OK")) {
                System.err.println("IP lookup reported failure");
                return false;
            }

            String latStr = parsedResponse.getString("Latitude");
            String lonStr = parsedResponse.getString("Longitude");
            if ((latStr == null) || (lonStr == null)) {
                System.err.println("IP lookup did not return lat-lon");
                return false;
            }
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            this.setLatLon(lat, lon);
        } catch (JSONException jsox) {
            System.err.println("Failed to parse IP lookup response");
            System.err.println("Exception: "+jsox.getMessage());
            return false;
        }

        return true;
    }
}
