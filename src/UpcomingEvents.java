/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import org.json.me.JSONObject;
import org.json.me.JSONException;
import org.json.me.JSONArray;
import java.util.Date;

/**
 *
 * @author anandi
 */
public class UpcomingEvents extends MapItemSource {
    public static final String UPCOMING_QUERY_URL = "http://upcoming.yahooapis.com/services/rest/?api_key=";
    public static final String UPCOMING_DEFAULT_METHOD = "event.getBestInPlace";
    public static final int UPCOMING_RADIUS = 50; //Km.

    private static final String NAME = "Upcoming Events";
    private static final String KEY = "Upcoming";
    private static final int DEFAULT_REFRESH_MODE = MapItemSource.REFRESH_MANUAL;

    protected String method;

    UpcomingEvents() {
        super(UpcomingEvents.NAME, UpcomingEvents.KEY,
              UpcomingEvents.DEFAULT_REFRESH_MODE);
        method = UPCOMING_DEFAULT_METHOD;
    }

    public boolean runUpdate(LocationData loc) {
        if (refreshMode == MapItemSource.REFRESH_REQUESTED)
            refreshMode = MapItemSource.REFRESH_MANUAL;

        if (loc == null)
            return false;

        String query = UPCOMING_QUERY_URL + GeoCrawlerKey.UPCOMING_API_KEY;
        query = query.concat("&format=json");
        query = query.concat("&method=").concat(method);
        query = query.concat("&location=").concat(Double.toString(loc.getLatitude())).concat(",").concat(Double.toString(loc.getLongitude()));

        //Now make the query and get back the data...
        String response = this.getHTTP(query);
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
                items = new MapItem[0];
                updateRunHistory(loc);
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
        updateRunHistory(loc);
        return true;
    }

    public boolean needsRefresh(LocationData loc) {
        if (refreshMode == MapItemSource.REFRESH_REQUESTED)
            return true;
        if (refreshMode == MapItemSource.REFRESH_MANUAL)
            return false;

        //If we are more than a day since last update, let us update once again.
        if ((items == null) || (lastRun == null) || (this.lastLoc == null))
            return true; //Never ran.

        Date now = new Date();
        long d = now.getTime() - lastRun.getTime();
        if (d > (24 * 3600 * 1000))
            return true;

        //OK, we are not that stale. But, are we far away enough?
        if (lastLoc.equals(loc))
            return false;
        if (lastLoc.getDistance(loc) > UpcomingEvents.UPCOMING_RADIUS)
            return true;

        return false;
    }
}
