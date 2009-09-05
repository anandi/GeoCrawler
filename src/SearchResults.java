/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import org.json.me.JSONObject;
import org.json.me.JSONException;
import org.json.me.JSONArray;
import net.oauth.j2me.OAuthParameterEncoder;

/**
 *
 * @author anandi
 */
public class SearchResults extends MapItemSource {
    private static final String NAME = "Search";
    public static final String LOCALS_QUERY_URL = "http://local.yahooapis.com/LocalSearchService/V3/localSearch?appid=";
    public static final String LOCALS_FIXED_PARAMS = "output=json";

    private String query;
    private int results;
    private int start;

    public SearchResults() {
        super(SearchResults.NAME);
        query = null;
        results = -1;
        start = -1;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setResults(int results) {
        this.results = results;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getCustomDisplayState() {
        return GeoCrawler.STATE_SEARCH;
    }

    private String localSearchQueryURL(LocationData loc) {
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();

        //The map API key should work for the query.
        String url = LOCALS_QUERY_URL + GeoCrawlerKey.YAHOO_REST_MAP_KEY;
        url = url.concat("&"+LOCALS_FIXED_PARAMS);
        url = url.concat("&latitude="+Double.toString(loc.getLatitude()));
        url = url.concat("&longitude="+Double.toString(loc.getLongitude()));
        url = url.concat("&query="+encoder.encode(this.query));
        url = url.concat("&results="+Integer.toString(results));
        url = url.concat("&start="+Integer.toString(start));

        return url;
    }

/* Locals Response looks like:
{
  "ResultSet":{
    "totalResultsAvailable":"728",
    "totalResultsReturned":"2",
    "firstResultPosition":"1",
    "ResultSetMapUrl":"http:\/\/maps.yahoo.com\/broadband\/?q1=Mars%2C+PA+16046&tt=discounts&tp=1",
    "Result":[
      ...
    ]
  }
 } */

    private boolean processLocalSearchResponse(String response) {
        try {
            JSONObject parsedResponse = new JSONObject(response);
            JSONObject resultSet = parsedResponse.getJSONObject("ResultSet");
            if (resultSet == null) {
                System.err.println("Unexpected response from locals query: "+response);
                return false;
            }
            JSONArray resultArray = resultSet.getJSONArray("Result");
            int resultCount = resultArray.length();
            if (resultCount == 0) {
                System.err.println("Found 0 Search results. events");
                items = new MapItem[0];
                return true;
            }

            //Parse successful. Now make the stuff into MapItems.
            items = new MapItem[resultCount];
            for (int i = 0 ; i < resultCount ; i++) {
                JSONObject result = resultArray.getJSONObject(i);
                LocalSearchMapItem lsmi = LocalSearchMapItem.fromJSON(result);
//                if (lsmi != null)
//                    lsmi.resolveAddr(loc); //We don't always trust the lat-lon.
                items[i] = lsmi;
            }
        } catch (JSONException jsox) {
            System.err.println("Failed to parse Locals response");
            System.err.println("Exception: "+jsox.getMessage());
            return false;
        }

        return true;
    }

    public boolean runUpdate(LocationData loc) {
        if (refreshMode == MapItemSource.REFRESH_REQUESTED)
            refreshMode = MapItemSource.REFRESH_MANUAL;

        if ((query == null) || (results < 0) || (start < 0))
            return false;

        String url = this.localSearchQueryURL(loc);

        //Now make the query and get back the data...
        String response = this.getHTTP(url);
        if (response == null)
            return false;

        return processLocalSearchResponse(response);
    }
}
