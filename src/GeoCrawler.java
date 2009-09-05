/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.microedition.midlet.*;
import javax.microedition.lcdui.Display;
import javax.microedition.rms.RecordStoreException;
import java.util.Hashtable;

/**
 * @author anandi
 */
public class GeoCrawler extends MIDlet implements LocationConsumer {
    public static final String VERSION = "0.1.0";

    public static final int STATE_EXIT = 0;
    public static final int STATE_BEGIN = 1;
    public static final int STATE_MAP = 2;
    public static final int STATE_FIREEAGLE = 3;
    public static final int STATE_MANUAL = 4;
    public static final int STATE_CONFIG = 5;
    public static final int STATE_ERROR = 6;
    public static final int STATE_SEARCH = 7;

    public static final int DETECTION_IN_PROGRESS = 0;
    public static final int DETECTION_SUCCEEDED = 1;
    public static final int DETECTION_FAILED = 2;

    LocationMaster locationCollector;
    private LocationData currentLocation; //Normally should be null, until the mapDisplay is ready.
                          //Pretty hacky and bad programming!!

    private int state;
    private int detection_state;

    private Display display;
    private DisplayModule currentDisplay;

    private MainDisplay mainDisplay;
    private MapDisplay mapDisplay;
    private ManualDisplay manualDisplay;
    private FireEagleDisplay fireEagleDisplay;
    private ConfigDisplay configDisplay;
    private ErrorDisplay errorDisplay;
    private SearchInputDisplay searchInputDisplay;

    private SearchResults searchResults; //Actually an engine that fetches search results.

    private PersistentConfig configStore;
    private FireEagle fireEagleInstance;

    public GeoCrawler() {
        display = Display.getDisplay(this);
        currentDisplay = null;
        currentLocation = null;
        try {
            configStore = new PersistentConfig("GeoCrawlerStore");
        } catch (RecordStoreException re) {
            configStore = null;
        }

        if (configStore != null)
            fireEagleInstance =
                new FireEagle(GeoCrawlerKey.FIRE_EAGLE_TOKEN,
                              GeoCrawlerKey.FIRE_EAGLE_SECRET, configStore);
        else
            fireEagleInstance = null;

        locationCollector = new LocationMaster(this);

        mainDisplay = null;
        mapDisplay = null;
        fireEagleDisplay = null;
        manualDisplay = null;
        configDisplay = null;
        errorDisplay = null;
        searchInputDisplay = null;

        searchResults = null;

        state = STATE_BEGIN;
        detection_state = DETECTION_IN_PROGRESS;
    }

    public PersistentConfig getConfigStore() {
        return configStore;
    }

    public FireEagle getFireEagle() {
        return fireEagleInstance;
    }

    public Display getDisplay() {
        return display;
    }

    public MapDisplay getMapDisplay() {
        return mapDisplay;
    }

    public void handleNextState(int state) {
        int currentState = this.state;
        if (state == STATE_BEGIN) {
            this.state = state;
            if (mainDisplay == null)
                mainDisplay = new MainDisplay(this);
            currentDisplay = mainDisplay;
        } else if (state == STATE_MAP) {
            this.state = state;
            if (mapDisplay == null) {
                mapDisplay = new MapDisplay(this);
                mapDisplay.registerMapItemSource(new UpcomingEvents());
                searchResults = new SearchResults();
                mapDisplay.registerMapItemSource(searchResults);
                if (currentLocation != null)
                    mapDisplay.notifyLocationUpdate();
            }
            currentDisplay = mapDisplay;
        } else if (state == STATE_FIREEAGLE) {
            if (fireEagleDisplay == null)
                fireEagleDisplay = new FireEagleDisplay(this);
            this.state = state;
            currentDisplay = fireEagleDisplay;
        } else if (state == STATE_MANUAL) {
            if (manualDisplay == null)
                manualDisplay = new ManualDisplay(this);
            this.state = state;
            currentDisplay = manualDisplay;
        } else if (state == STATE_CONFIG) {
            if (configDisplay == null)
                configDisplay = new ConfigDisplay(this);
            this.state = state;
            currentDisplay = configDisplay;
        } else if (state == STATE_ERROR) {
            if (errorDisplay == null)
                errorDisplay = new ErrorDisplay(this);
            this.state = state;
            currentDisplay = errorDisplay;
        } else if (state == STATE_SEARCH) {
            if (searchInputDisplay == null)
                searchInputDisplay = new SearchInputDisplay(this, searchResults);
            this.state = state;
            currentDisplay = searchInputDisplay;
        } else if (state == STATE_EXIT) {
            if ((locationCollector != null) && locationCollector.isRunning())
                locationCollector.stop();
            this.destroyApp(false);
            this.notifyDestroyed();
        }

        if (state != STATE_EXIT)
            currentDisplay.display(currentState);
    }

    public void startApp() {
        detection_state = DETECTION_IN_PROGRESS;
        locationCollector.start();
        handleNextState(STATE_BEGIN);
    }

    public int getDetectionState() {
        return detection_state;
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void locationCallback(LocationData location) {
        boolean autoSwitch = false;

        if ((currentLocation == null) && (state == STATE_BEGIN)
            && (fireEagleInstance != null)
            && (fireEagleInstance.getState() == FireEagle.STATE_AUTHORIZED))
            autoSwitch = true;
        currentLocation = location;
        detection_state = DETECTION_SUCCEEDED;

        //Update Fire Eagle if we are authorized...
        if (!location.updatedFireEagle && (fireEagleInstance != null)
            && (fireEagleInstance.getState() == FireEagle.STATE_AUTHORIZED)) {
            String lat = Double.toString(location.getLatitude());
            String lon = Double.toString(location.getLongitude());

            Hashtable params = new Hashtable(2);
            params.put("lat", lat);
            params.put("lon", lon);
            fireEagleInstance.updateLocation(params);

            //See if we can get the current address string...
            String[] locations = fireEagleInstance.lookupLocation(params);
            if ((locations != null) && (locations.length == 1)) {
                currentLocation.setAddress(locations[0]);
            }
        }

        if (mapDisplay != null) {
            mapDisplay.notifyLocationUpdate();
            if (state == STATE_MAP)
                mapDisplay.display(state); //Refresh
        }

        if (autoSwitch)
            handleNextState(STATE_MAP);
        else if (state == STATE_BEGIN)
            handleNextState(STATE_BEGIN);
    }

    public void detectFailed() {
        detection_state = DETECTION_FAILED;
        handleNextState(STATE_MAP); //Let us go to the map anyway.
    }

    public LocationMaster getCollector() {
        return locationCollector;
    }

    public void showError(String error, int nextState) {
        if (error == null)
            return;
        setError(error);
        state = nextState; //Spoof the return state for error display.
        handleNextState(STATE_ERROR);
    }

    public void showInfo(String error, int nextState) {
        if (error == null)
            return;
        setInfo(error);
        state = nextState; //Spoof the return state for error display.
        handleNextState(STATE_ERROR);
    }

    //Call to this method simply logs an error... does not show it.
    public void setError(String error) {
        if (error == null)
            return;
        if (errorDisplay == null) {
            errorDisplay = new ErrorDisplay(this);
        }
        errorDisplay.setMessage(error, ErrorDisplay.MSG_ERROR);
    }

    //Call to this method simply logs an info, which makes no sense to be
    //public.
    private void setInfo(String info) {
        if (info == null)
            return;
        if (errorDisplay == null) {
            errorDisplay = new ErrorDisplay(this);
        }
        errorDisplay.setMessage(info, ErrorDisplay.MSG_INFO);
    }

    public LocationData getCurrentLocation() {
        return currentLocation;
    }
}
