/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Hashtable;
import java.util.Enumeration;

/**
 *
 * @author anandi
 */
public class GeoCrawlerKey {
    //Fire Eagle
    public static final String FIRE_EAGLE_TOKEN = "Your Fire Eagle Consumer Key here.";
    public static final String FIRE_EAGLE_SECRET = "Your Fire Eagle Consumer Secret here.";
    public static final String FIRE_EAGLE_AUTH_URL = "Your Fire Eagle mobile auth URL here.";
    public static final String YAHOO_REST_MAP_KEY = "Your Yahoo Maps! app key here.";

    //Open Cell ID
    public static final String OPEN_CELL_ID_KEY = "Your open cell id app key here";

    public static final String VALUE_TYPE_HIDDEN = "hidden";
    public static final String VALUE_TYPE_UNKNOWN = "unknown";
    public static final String VALUE_TYPE_BOOLEAN = "boolean";
    public static final String VALUE_TYPE_NUMERIC = "number";

    //These are not really keys, but identifiers for the persistent database.
    private static Hashtable configValueTypes;
    
    public static final String FIRE_EAGLE_ACCESS_TOKEN = "FE.AccessToken";
    public static final String FIRE_EAGLE_REQUEST_TOKEN = "FE.RequestToken";
    public static final String FIRE_EAGLE_ACCESS_SECRET = "FE.AccessTokenSecret";
    public static final String FIRE_EAGLE_REQUEST_SECRET = "FE.RequestTokenSecret";

    public static final String GPS_ENABLED = "GPS.enabled";
    public static final String GPS_ERROR = "GPS.error"; //In meters.
    public static final String GPS_TIMEOUT = "GPS.timeout"; //In seconds.
    public static final String CELL_ENABLED = "CELL.enabled";
    public static final String UPDATE_INTERVAL = "LM.minutes";
    public static final String UPDATE_AUTO = "LM.run";

    private static void initialize() {
        if (configValueTypes == null) {
            configValueTypes = new Hashtable();
            //Put each key and it's corresponding value type here.
            configValueTypes.put(GeoCrawlerKey.FIRE_EAGLE_ACCESS_TOKEN, GeoCrawlerKey.VALUE_TYPE_HIDDEN);
            configValueTypes.put(GeoCrawlerKey.FIRE_EAGLE_ACCESS_SECRET, GeoCrawlerKey.VALUE_TYPE_HIDDEN);
            configValueTypes.put(GeoCrawlerKey.FIRE_EAGLE_REQUEST_TOKEN, GeoCrawlerKey.VALUE_TYPE_HIDDEN);
            configValueTypes.put(GeoCrawlerKey.FIRE_EAGLE_REQUEST_SECRET, GeoCrawlerKey.VALUE_TYPE_HIDDEN);

            configValueTypes.put(GeoCrawlerKey.GPS_ENABLED, GeoCrawlerKey.VALUE_TYPE_BOOLEAN);
            configValueTypes.put(GeoCrawlerKey.GPS_ERROR, GeoCrawlerKey.VALUE_TYPE_NUMERIC);
            configValueTypes.put(GeoCrawlerKey.GPS_TIMEOUT, GeoCrawlerKey.VALUE_TYPE_NUMERIC);
            configValueTypes.put(GeoCrawlerKey.CELL_ENABLED, GeoCrawlerKey.VALUE_TYPE_BOOLEAN);
            configValueTypes.put(GeoCrawlerKey.UPDATE_INTERVAL, GeoCrawlerKey.VALUE_TYPE_NUMERIC);
            configValueTypes.put(GeoCrawlerKey.UPDATE_AUTO, GeoCrawlerKey.VALUE_TYPE_BOOLEAN);
        }
    }

    public static String getConfigType(String s) {
        GeoCrawlerKey.initialize();
        String t = (String)configValueTypes.get(s);
        if (t == null)
            return GeoCrawlerKey.VALUE_TYPE_UNKNOWN;

        return t;
    }

    public static Enumeration getConfigKeys() {
        GeoCrawlerKey.initialize();
        return configValueTypes.keys();
    }

    public static int getKeyCount() {
        GeoCrawlerKey.initialize();
        return configValueTypes.size();
    }
}
