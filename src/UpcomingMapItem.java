/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Hashtable;
import java.util.Vector;
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
        Hashtable geocoderParams = new Hashtable();
        //Get the street.
        String tmp = (String)info.get("venue_address");
        if ((tmp == null) || (tmp.length() == 0))
            return; //We do not have to resolve where the street is not clear.
        geocoderParams.put("street", tmp);

        //Get the city.
        tmp = (String)info.get("venue_city");
        if ((tmp == null) || (tmp.length() == 0))
            return; //Street without a parent city is ambiguous.
        geocoderParams.put("city", tmp);


        //Get the state.
        tmp = (String)info.get("venue_state_name");
        if ((tmp != null) && (tmp.length() > 0))
            geocoderParams.put("state", tmp);

        //Get the zip.
        tmp = (String)info.get("venue_zip");
        if ((tmp != null) && (tmp.length() > 0))
            geocoderParams.put("zip", tmp);

        Vector locations = GeoCoder.addressToLocation(geocoderParams);
        if (locations == null)
            return;

        double bestLat = getLat();
        double bestLon = getLon();
        double distance = 0;
        for (int i = 0 ; i < locations.size() ; i++) {
            LocationData location = (LocationData)locations.elementAt(i);
            double tmpDistance = observer.getDistance(location);
            if ((i == 0) || (tmpDistance < distance)) {
                bestLat = location.getLatitude();
                bestLon = location.getLongitude();
                distance = tmpDistance;
            }
        }

        this.updateLocation(bestLat, bestLon);
    }
}
