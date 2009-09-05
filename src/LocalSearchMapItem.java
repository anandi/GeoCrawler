/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Hashtable;
import org.json.me.JSONObject;
import org.json.me.JSONException;
//import org.json.me.JSONArray;

/**
 *
 * @author anandi
 */
public class LocalSearchMapItem extends MapItem {
    private Hashtable info;

/* A result looks like:
      {
        "id":"11918680",
        "Title":"Purvis Brothers Incorporated",
        "Address":"321 Mars Valencia Rd",
        "City":"Mars",
        "State":"PA",
        "Phone":"(724) 625-1566",
        "Latitude":"40.688029",
        "Longitude":"-80.005455",
        "Rating":{
          "AverageRating":"NaN",
          "TotalRatings":"0",
          "TotalReviews":"0",
          "LastReviewDate":"",
          "LastReviewIntro":""
        },
        "Distance":"0.74",
        "Url":"http:\/\/local.yahoo.com\/info-11918680-purvis-brothers-incorporated-mars",
        "ClickUrl":"http:\/\/local.yahoo.com\/info-11918680-purvis-brothers-incorporated-mars",
        "MapUrl":"http:\/\/maps.yahoo.com\/maps_result?q1=321+Mars+Valencia+Rd+Mars+PA&gid1=11918680",
        "BusinessUrl":"http:\/\/purvisbrothers.com\/",
        "BusinessClickUrl":"http:\/\/purvisbrothers.com\/",
        "Categories":{
          "Category":[
            {
              "id":"96931024",
              "content":"Metal Industries"
            },
            ...
          ]
        }
      },
*/
    private static String[] DISPLAYABLES = {
        "Title", "Address", "Phone", "BusinessUrl"
    };
    private static String[] INTERNAL_KEYS = {
        "City", "State"
    };

    public LocalSearchMapItem(String _desc, double _lat, double _lon) {
        super(_desc, _lat, _lon);
        info = new Hashtable();
    }

    public static LocalSearchMapItem fromJSON(JSONObject result) {
        if (result == null)
            return null;

        LocalSearchMapItem item = null;
        try {
            double lat = Double.parseDouble(result.getString("Latitude"));
            double lon = Double.parseDouble(result.getString("Longitude"));
            item = new LocalSearchMapItem(null, lat, lon);

            for (int i = 0 ; i < LocalSearchMapItem.DISPLAYABLES.length ; i++) {
                String key = LocalSearchMapItem.DISPLAYABLES[i];
                if (result.has(key))
                    item.addInfo(key, result.getString(key));
            }

            for (int i = 0 ; i < LocalSearchMapItem.INTERNAL_KEYS.length ; i++) {
                String key = LocalSearchMapItem.INTERNAL_KEYS[i];
                if (result.has(key))
                    item.addInfo(key, result.getString(key));
            }
        } catch (JSONException jsox) {
            return null; //Need a valid lat-lon pair.
        }

        return item;
    }

    public void addInfo(String key, String value) {
        info.put(key, value);
    }

    public String getShortDescription(int length) {
        String tmp = (String)info.get("Title");
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

        for (int i = 0 ; i < LocalSearchMapItem.DISPLAYABLES.length ; i++) {
            String tmp = (String)info.get(LocalSearchMapItem.DISPLAYABLES[i]);
            if ((tmp != null) && (tmp.length() > 0))
                buffer = buffer.concat(LocalSearchMapItem.DISPLAYABLES[i]).concat(": ").concat(tmp).concat("\n");
        }
        return buffer;
    }
}
