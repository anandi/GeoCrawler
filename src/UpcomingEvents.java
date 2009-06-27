/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;
import org.json.me.JSONObject;
import org.json.me.JSONException;
import org.json.me.JSONArray;

/**
 *
 * @author anandi
 */
public class UpcomingEvents extends MapItemSource {
    public static final String UPCOMING_QUERY_URL = "http://upcoming.yahooapis.com/services/rest/?api_key=";
    public static final String UPCOMING_DEFAULT_METHOD = "event.getBestInPlace";

    protected String method;

    UpcomingEvents() {
        super();
        method = UPCOMING_DEFAULT_METHOD;
    }

    public boolean runUpdate(LocationData loc) {
        if (loc == null)
            return false;

        String query = UPCOMING_QUERY_URL + GeoCrawlerKey.UPCOMING_API_KEY;
        query = query.concat("&format=json");
        query = query.concat("&method=").concat(method);
        query = query.concat("&location=").concat(Double.toString(loc.getLatitude())).concat(",").concat(Double.toString(loc.getLongitude()));

        //Now make the query and get back the data...
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(query);
        } catch (IOException iox) {
            System.err.println("Upcoming query failed.");
            return false;
        }
        if (response == null)
            return false;

        try {
            JSONObject parsedResponse = new JSONObject(response);
            //We need to get the "rsp" field, which is the root structure.
            parsedResponse = parsedResponse.getJSONObject("rsp");
            if (!parsedResponse.getString("stat").equals("ok")) {
                System.err.println("Upcoming reported failure");
                return false;
            }
            JSONArray events = parsedResponse.getJSONArray("event");
            int eventCount = events.length();
            if (eventCount == 0) {
                System.err.println("Found 0 Upcoming events");
                return true;
            }

            //Parse successful. Now make the stuff into MapItems.
            items = new MapItem[eventCount];
            for (int i = 0 ; i < eventCount ; i++) {
                JSONObject event = events.getJSONObject(i);
                UpcomingMapItem umi = UpcomingMapItem.fromJSON(event);
                if (umi != null)
                    umi.resolveAddr(loc); //We don't always trust the lat-lon.
                items[i] = umi;
            }
        } catch (JSONException jsox) {
            System.err.println("Failed to parse Upcoming response");
            System.err.println("Exception: "+jsox.getMessage());
            return false;
        }
        return true;
    }
}
