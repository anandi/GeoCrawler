/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Hashtable;
import net.oauth.j2me.OAuthParameterEncoder;
import java.io.IOException;
import org.json.me.JSONObject;
import org.json.me.JSONException;

/**
 *
 * @author anandi
 */
/*
 {
   "id":2871518,
   "name":"Green Day Vancouver",
   "description":"Green Day Tickets Vancouver\n...",
   "start_date":"2009-07-04",
   "end_date":"",
   "start_time":"19:00:00",
   "end_time":-1,
   "personal":0,
   "selfpromotion":0,
   "metro_id":14,
   "venue_id":43599,
   "user_id":751952,
   "category_id":1,
   "date_posted":"2009-06-09 01:50:11",
   "watchlist_count":2,
   "url":"http:\/\/www.zimbio.com\/Tickets+Online\/articles\/82\/Green+Day+Tickets+Vancouver",
   "distance":8.3,
   "distance_units":"miles",
   "latitude":49.2879,
   "longitude":-123.1103,
   "geocoding_precision":"address",
   "geocoding_ambiguous":0,
   "venue_name":"General Motors Place",
   "venue_address":"800 Griffiths Way",
   "venue_city":"Vancouver",
   "venue_state_name":"British Columbia",
   "venue_state_code":"bc",
   "venue_state_id":55,
   "venue_country_name":"Canada",
   "venue_country_code":"ca",
   "venue_country_id":2,
   "venue_zip":"",
   "ticket_url":"http:\/\/www.online-buyer.net\/green_day_tickets_vancouver_upc",
   "ticket_price":"",
   "ticket_free":0,
   "photo_url":"",
   "num_future_events":0,
   "start_date_last_rendition":"Jul 4, 2009",
   "utc_start":"2009-07-05 02:00:00 UTC",
   "utc_end":"2009-07-05 05:00:00 UTC"
 }*/
public class UpcomingMapItem extends MapItem {
    private static String YAHOO_GEO_CODER_URL="http://local.yahooapis.com/MapsService/V1/geocode?appid=";
    private static String[] UPCOMING_DISPLAYABLES = {
        "name", "start_date", "end_date", "start_time", "end_time", "url",
        "venue_name", "venue_address", "ticket_url", "description"
    };
    private static String[] DISPLAY_KEY = { //Keep in sync with UPCOMING_DISPLAYABLES!
        "Event", "From", "Till", "Starts At", "Ends At", "URL",
        "Venue", "Address", "Ticket", "Description"
    };
    private static String[] INTERNAL_KEYS = {
        "venue_city", "venue_state_name", "venue_zip"
    };

    private Hashtable info;

    public static UpcomingMapItem fromJSON(JSONObject event) {
        if (event == null)
            return null;

        UpcomingMapItem item = null;
        try {
            double lat = Double.parseDouble(event.getString("latitude"));
            double lon = Double.parseDouble(event.getString("longitude"));
            item = new UpcomingMapItem(null, lat, lon);

            for (int i = 0 ; i < UpcomingMapItem.UPCOMING_DISPLAYABLES.length ; i++) {
                String key = UpcomingMapItem.UPCOMING_DISPLAYABLES[i];
                if (event.has(key))
                    item.addInfo(key, event.getString(key));
            }

            for (int i = 0 ; i < UpcomingMapItem.INTERNAL_KEYS.length ; i++) {
                String key = UpcomingMapItem.INTERNAL_KEYS[i];
                if (event.has(key))
                    item.addInfo(key, event.getString(key));
            }
        } catch (JSONException jsox) {
            return null; //Need a valid lat-lon pair.
        }

        return item;
    }

    public UpcomingMapItem(String _desc, double _lat, double _lon) {
        super(_desc, _lat, _lon);
        info = new Hashtable();
    }

    public String getShortDescription(int length) {
        String tmp = (String)info.get("name");
        if (tmp != null) {
            if (tmp.length() > length)
                return tmp.substring(0, length - 4) + " ...";
            else
                return tmp;
        }
        return super.getShortDescription(length);
    }

    public String getDescription() {
        String buffer = new String("");

        for (int i = 0 ; i < UpcomingMapItem.UPCOMING_DISPLAYABLES.length ; i++) {
            String tmp = (String)info.get(UpcomingMapItem.UPCOMING_DISPLAYABLES[i]);
            if ((tmp != null) && (tmp.length() > 0))
                buffer = buffer.concat(UpcomingMapItem.DISPLAY_KEY[i]).concat(": ").concat(tmp).concat("\n");
        }
        return buffer;
    }

    public void addInfo(String key, String value) {
        info.put(key, value);
    }

    public void resolveAddr(LocationData observer) {
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();
        String url = UpcomingMapItem.YAHOO_GEO_CODER_URL + GeoCrawlerKey.YAHOO_REST_MAP_KEY;

        //Get the street.
        String tmp = (String)info.get("venue_address");
        if ((tmp == null) || (tmp.length() == 0))
            return; //We do not have to resolve where the street is not clear.
        url = url.concat("&street=").concat(encoder.encode(tmp));

        //Get the city.
        tmp = (String)info.get("venue_city");
        if ((tmp == null) || (tmp.length() == 0))
            return; //Street without a parent city is ambiguous.
        url = url.concat("&city=").concat(encoder.encode(tmp));


        //Get the state.
        tmp = (String)info.get("venue_state_name");
        if ((tmp != null) && (tmp.length() > 0))
            url = url.concat("&state=").concat(encoder.encode(tmp));

        //Get the zip.
        tmp = (String)info.get("venue_zip");
        if ((tmp != null) && (tmp.length() > 0))
            url = url.concat("&zip=").concat(encoder.encode(tmp));

        //Now make the query and get back the data...
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(url);
        } catch (IOException iox) {
            return;
        }
        if (response == null)
            return;

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
        double bestLat = getLat();
        double bestLon = getLon();
        double distance = Double.NaN;
        int start = response.indexOf("<Latitude>", 0);
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

            //Check if these are parsable!
            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                double tmpDistance = observer.getDistance(lat, lon);
                if ((distance == Double.NaN) || (tmpDistance < distance)) {
                    bestLat = lat;
                    bestLon = lon;
                    distance = tmpDistance;
                }
            } catch (Exception e) {
                break;
            }
            start = response.indexOf("<Latitude>", end + 12);
        }

        this.updateLocation(bestLat, bestLon);
    }
}
