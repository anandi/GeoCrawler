/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import net.oauth.j2me.Consumer;
import net.oauth.j2me.token.RequestToken;
import net.oauth.j2me.token.AccessToken;
import net.oauth.j2me.OAuthServiceProviderException;
import net.oauth.j2me.BadTokenStateException;
import java.util.Hashtable;
import java.io.IOException;
import org.json.me.JSONObject;
import org.json.me.JSONException;
import org.json.me.JSONArray;

/**
 *
 * @author anandi
 */
public class FireEagle {
    public static final int STATE_NOTOKEN = 0;
    public static final int STATE_REQUEST_TOKEN = 1;
    public static final int STATE_AUTHORIZED = 2;

    private PersistentConfig configStore;

    private String consumerToken;
    private String consumerTokenSecret;

    public static final String OAUTH_HOST="https://fireeagle.yahooapis.com";
    public static final String REQUEST_TOKEN_URL="/oauth/request_token";
    public static final String ACCESS_TOKEN_URL="/oauth/access_token";
    public static final String UPDATE_API_URL="/api/0.1/update.json";
    public static final String QUERY_API_URL="/api/0.1/user";
    public static final String LOOKUP_API_URL="/api/0.1/lookup.json";

    private String token;
    private String secret;

    private int state;

    public FireEagle(String consumerKey, String consumerSecret,
                     PersistentConfig pc) {
        this.configStore = pc;
        consumerToken = consumerKey;
        consumerTokenSecret = consumerSecret;
        state = STATE_NOTOKEN;

        //First check whether we already have a access token.
        token = configStore.getConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN);
        if (token != null) {
            secret = configStore.getConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_SECRET);
            state = STATE_AUTHORIZED;
        } else {
            if ((GeoCrawlerKey.GEO_CRAWLER_DEVEL_MODE)
                && (GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN_STR != null)
                && (GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN_SECRET_STR != null)) {
                token = GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN_STR;
                secret = GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN_SECRET_STR;
                configStore.setConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN, token);
                configStore.setConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_SECRET, secret);
                state = STATE_AUTHORIZED;
            }
        }

        if (token == null) {
            //Check if we have a saved request token.
            token = configStore.getConfigString(GeoCrawlerKey.FIRE_EAGLE_REQUEST_TOKEN);
            if (token != null) {
                secret = configStore.getConfigString(GeoCrawlerKey.FIRE_EAGLE_REQUEST_SECRET);
                state = STATE_REQUEST_TOKEN;
            }
        }
    }

    public int getState() {
        return state;
    }

    public String getToken() {
        return token;
    }

    private Consumer getOauthConsumer() {
        Consumer oauthConsumer = new Consumer(consumerToken,
                                              consumerTokenSecret);
        oauthConsumer.setSignatureMethod("HMAC-SHA1");
        return oauthConsumer;
    }

    public boolean fetchRequestToken() {
        if (state != STATE_NOTOKEN)
            return false; //Don't bother getting anything.

        RequestToken rToken = null;
        try {
            Consumer oConsumer = getOauthConsumer();
            String url = OAUTH_HOST+REQUEST_TOKEN_URL;
            rToken = oConsumer.getRequestToken(url, "oob");
        } catch (OAuthServiceProviderException e) {
            return false;
        }

        if (rToken == null)
            return false;

        token = rToken.getToken();
        secret = rToken.getSecret();
        if (!(configStore.setConfigString(GeoCrawlerKey.FIRE_EAGLE_REQUEST_TOKEN, token)
             && configStore.setConfigString(GeoCrawlerKey.FIRE_EAGLE_REQUEST_SECRET, secret)))
            return false;

        state = STATE_REQUEST_TOKEN;
        return true;
    }

    private void deleteRequestToken(int state) {
        configStore.deleteConfigString(GeoCrawlerKey.FIRE_EAGLE_REQUEST_TOKEN);
        configStore.deleteConfigString(GeoCrawlerKey.FIRE_EAGLE_REQUEST_SECRET);
        this.state = state;
    }

    public boolean exchangeRequestToken(String verifier) {
        if (state != STATE_REQUEST_TOKEN)
            return false;

        AccessToken aToken = null;
        try {
            RequestToken rToken = new RequestToken(token, secret);
            String url = OAUTH_HOST+ACCESS_TOKEN_URL;
            Consumer oConsumer = getOauthConsumer();
            aToken = oConsumer.getAccessToken(url, rToken, verifier);
        } catch (OAuthServiceProviderException e) {
            //Remove the request token.
            int rc = e.getHTTPResponseCode();
            if ((rc >= 400) && (rc <= 403))
                //We got a oauth related injury. Normally, after all debugging
                //and testing, if we still get it, it must be related to bad
                //token state. Better to roll back.
                deleteRequestToken(STATE_NOTOKEN);
            return false;
        } catch (BadTokenStateException be) {
            deleteRequestToken(STATE_NOTOKEN);
            return false;
        }

        if (aToken == null) //Bad parsing error perhaps.
            return false; //Do not delete the request token.

        token = aToken.getToken();
        secret = aToken.getSecret();
        if (!(configStore.setConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN, token)
             && configStore.setConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_SECRET, secret)))
            return false;
        deleteRequestToken(STATE_AUTHORIZED);
        return true;
    }
    
    private void deleteAccessToken() {
        configStore.deleteConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN);
        configStore.deleteConfigString(GeoCrawlerKey.FIRE_EAGLE_ACCESS_SECRET);
        this.state = STATE_NOTOKEN;
    }

    public void resetTokens() {
        if (state == STATE_REQUEST_TOKEN)
            deleteRequestToken(STATE_NOTOKEN);
        else if (state == STATE_AUTHORIZED)
            deleteAccessToken();
    }

    //The two following method are strictly temporary, till I get the JSON code
    //from https://meapplicationdevelopers.dev.java.net/source/browse/meapplicationdevelopers/demobox/mobileajax/lib/json/
    //working.
    protected String naiveParseErrorResponse(String someXML) {
        int s=someXML.indexOf("msg=");
        if (s<0) {
            return "unknown";
        }
        s=s+5; // move to end of msg=" tag
        int e=someXML.indexOf("\"", s);
        if (e<0) {
            return "unknown";
        }
        return someXML.substring(s, e);
    }

    protected String naiveParseQueryResponse(String someXML) {
        int s=someXML.indexOf("<name>");
        if (s<0) {
            return "unknown";
        }
        s=s+6; // move to end of <name> tag
        int e=someXML.indexOf("</name>", s);
        if (e<0) {
            return "unknown";
        }
        return someXML.substring(s, e);
    }

    public boolean updateLocation(Hashtable params) {
        if (state != STATE_AUTHORIZED)
            return false;
        AccessToken aToken = new AccessToken(token, secret);
        Consumer oConsumer = getOauthConsumer();
        String response = "";
        try {
            response = oConsumer.accessProtectedResource(OAUTH_HOST+UPDATE_API_URL,
                                                        aToken, params, "POST");
        } catch (OAuthServiceProviderException ospe) {
            String feError = this.naiveParseErrorResponse(ospe.getHTTPResponse());
            System.err.println("FireEagle::updateLocation - Caught exception: "
                               + ospe.getHTTPResponse() + ": " + feError);
            return false;
        } catch (IOException ioe) {
            System.err.println("FireEagle::updateLocation - Caught exception: "
                               + ioe.getMessage());
            return false;
        }

        //Call succeeded.
        try {
            JSONObject parsedResponse = new JSONObject(response);
            String status = parsedResponse.getString("stat");
            if (!status.equals("ok"))
                return false;
//            System.err.println("Successfully updated Fire Eagle");
        } catch (JSONException jx) {
            System.err.println("Failed to parse JSON response: "+jx.getMessage());
            return false; //Failed to parse JSON.
        }
        return true;
    }
    
    /*
Lookup with a lat-lon: Should always be unambiguous!
{
 "count": 1,
 "stat": "ok",
 "locations": [
  {
   "name": "Cheluvadipalya Lane, Bangalore, Karnataka, India",
   "woeid": 55924889,
   "place_id": "7lymAgCcBJX6_TKGDw"
  }
 ],
 "start": 0,
 "total": 1,
 "query": "lat=12.967&lon=77.567"
}

Lookup with a query string!
{
 "count": 22,
 "stat": "ok",
 "locations": [
  {
   "name": "London, England",
   "woeid": 44418,
   "place_id": ".2P4je.dBZgMyQ"
  },
  {
   "name": "London, Ontario",
   "woeid": 4063,
   "place_id": "NRJjNLydAZo9"
  },
  ...
  {
   "name": "London, France",
   "woeid": 20215476,
   "place_id": "QXJP3KqbAZ6NZA.pqA"
  }
 ],
 "start": 0,
 "total": 22,
 "query": "q=London"
}
     */
    public String[] lookupLocation(Hashtable params) {
        if (state != STATE_AUTHORIZED)
            return null;
        AccessToken aToken = new AccessToken(token, secret);
        Consumer oConsumer = getOauthConsumer();
        String response = "";
        try {
            response = oConsumer.accessProtectedResource(OAUTH_HOST+LOOKUP_API_URL,
                                                        aToken, params, "POST");
        } catch (OAuthServiceProviderException ospe) {
            String feError = this.naiveParseErrorResponse(ospe.getHTTPResponse());
            System.err.println("FireEagle::updateLocation - Caught exception: "
                               + ospe.getHTTPResponse() + ": " + feError);
            return null;
        } catch (IOException ioe) {
            System.err.println("FireEagle::updateLocation - Caught exception: "
                               + ioe.getMessage());
            return null;
        }

        //Call succeeded.
        String[] locations = null;
        try {
            JSONObject parsedResponse = new JSONObject(response);
            String status = parsedResponse.getString("stat");
            if (!status.equals("ok"))
                return null;
            int count = parsedResponse.getInt("count");
            System.err.println("Lookup query successful: Got "+Integer.toString(count)+" entries");
            if (count == 0)
                return null;
            locations = new String[count];
            JSONArray locArray = parsedResponse.getJSONArray("locations");
            for (int i = 0 ; i < count ; i++) {
                locations[i] = locArray.getJSONObject(i).getString("name");
                System.err.println("Got location: "+locations[i]);
            }
        } catch (JSONException jx) {
            System.err.println("Failed to parse JSON response: "+jx.getMessage());
            return null; //Failed to parse JSON.
        }
        return locations;
    }

    //The purpose of this class is not to query Fire Eagle for the current
    //location. Think about it. You have a mobile in your hand and a cell
    //connection. You need to draw a map of the things around you. How would
    //Fire Eagle know where you are if your mobile does not tell that to Fire
    //Eagle? In which case, your mobile already knows where it is... and you
    //are at hand to correct manually any error. There is absolutely no
    //reason to get your location from Fire Eagle.
    //Um... you do have a cell network connection. Don't you?
}
