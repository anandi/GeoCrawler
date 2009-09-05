/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Date;
import java.io.IOException;

/**
 *
 * @author anandi
 */
public abstract class MapItemSource {
    protected MapItem[] items;
    protected Date lastRun;
    protected LocationData lastLoc;

    public static final int REFRESH_AUTO = 0;
    public static final int REFRESH_MANUAL = 1;
    public static final int REFRESH_REQUESTED = 2;

    public static final int CUSTOM_STATE_NONE = 0;
    protected int refreshMode;
    private int defaultRefreshMode;

    private String key;
    private String name;

    MapItemSource(String _name, String _key, int _defaultRefreshMode) {
        init(_key, _name, _defaultRefreshMode);
    }

    MapItemSource(String _name) {
        init("", _name, REFRESH_MANUAL);
    }

    private void init(String _key, String _name, int _defaultRefreshMode) {
        items = null;
        lastRun = null;
        lastLoc = null;
        refreshMode = _defaultRefreshMode;
        defaultRefreshMode = _defaultRefreshMode;

        key = _key;
        name = _name;
    }

    //Override this function so that when the module is invoked, it can provide
    //a handle to the main application to display whatever state it needs to.
    public int getCustomDisplayState() {
        return MapItemSource.CUSTOM_STATE_NONE; //0 is not a valid state.
    }

    //Override this method to allow for periodic refresh, even if the current
    //location has not changed.
    public boolean needsRefresh(LocationData loc) {
        if (refreshMode == MapItemSource.REFRESH_REQUESTED)
            return true;
        if (refreshMode != MapItemSource.REFRESH_AUTO)
            return false;
        return (items == null) ? true : false;
    }

    protected void updateRunHistory(LocationData loc) {
        lastRun = new Date();
        lastLoc = loc;
    }

    //Execute the actual update.
    public abstract boolean runUpdate(LocationData loc);

    public String getConfigKey() {
        return key;
    }

    public String getServiceName() {
        return name;
    }

    public int getDefaultRunMode() {
        return defaultRefreshMode;
    }

    public void setRefreshMode(int mode) {
        switch(mode) {
        case REFRESH_AUTO:
        case REFRESH_MANUAL:
        case REFRESH_REQUESTED:
            refreshMode = mode;
        default:
            break;
        }
    }

    public int getItemCount() {
        if (items == null)
            return 0;
        return items.length;
    }
    
    public MapItem getItem(int index) {
        if (items == null)
            return null;
        if (index >= items.length)
            return null;
        return items[index];
    }

    public void invalidateItems() {
        //This will be called when the current location changes and the items
        //become obsolete.
        items = null;
    }

    protected String getHTTP(String url) {
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(url);
        } catch (IOException iox) {
            System.err.println("HTTP query failed. URL: "+url);
            return null;
        }

        return response;
    }

    //We shall add more methods here as we proceed.
}
