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

    public static final int STATE_EXIT = 0;
    public static final int STATE_BEGIN = 1;
    public static final int STATE_MAP = 2;
    public static final int STATE_FIREEAGLE = 3;
    public static final int STATE_MANUAL = 4;
    public static final int STATE_CONFIG = 5;
    public static final int STATE_ERROR = 6;

    LocationMaster locationCollector;
    LocationData tempLoc; //Normally should be null, until the mapDisplay is ready.
                          //Pretty hacky and bad programming!!

    private int state;

    private Display display;
    private DisplayModule currentDisplay;

    private MainDisplay mainDisplay;
    private MapDisplay mapDisplay;
    private ManualDisplay manualDisplay;
    private FireEagleDisplay fireEagleDisplay;
    private ConfigDisplay configDisplay;
    private ErrorDisplay errorDisplay;

    private PersistentConfig configStore;
    private FireEagle fireEagleInstance;

    public GeoCrawler() {
        display = Display.getDisplay(this);
        currentDisplay = null;
        tempLoc = null;
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

        locationCollector = new LocationMaster(configStore, this);

        initVars();
        state = STATE_BEGIN;
    }
    
    private void initVars() {
        mainDisplay = null;
        mapDisplay = null;
        fireEagleDisplay = null;
        manualDisplay = null;
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
                if (tempLoc != null) {
                    mapDisplay.setLocation(tempLoc);
                    tempLoc = null;
                }
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
        locationCollector.start();
        handleNextState(STATE_BEGIN);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void locationCallback(LocationData location) {
        if (mapDisplay == null) {
            tempLoc = location; //It will be given to mapDisplay when initialized!
            return;
        }

        //Update Fire Eagle if we are authorized...
        if ((fireEagleInstance != null)
            && (fireEagleInstance.getState() == FireEagle.STATE_AUTHORIZED)) {
            Hashtable params = new Hashtable(2);
            params.put("lat", Double.toString(location.getLatitude()));
            params.put("lon", Double.toString(location.getLongitude()));
            fireEagleInstance.updateLocation(params);
        }

        mapDisplay.setLocation(location);
        if (state == STATE_MAP)
            mapDisplay.display(state); //Refresh
    }

    public LocationMaster getCollector() {
        return locationCollector;
    }

    public boolean handleConfigChange(String key, String newVal) {
        boolean boolVal = false;
        double dVal = 0;

        String type = GeoCrawlerKey.getConfigType(key);
        if (type.equals(GeoCrawlerKey.VALUE_TYPE_BOOLEAN)) {
            if (newVal.equals("true"))
                boolVal = true;
            else if (newVal.equals("false"))
                boolVal = false;
            else
                return false;
        } else if (type.equals(GeoCrawlerKey.VALUE_TYPE_NUMERIC)) {
            dVal = Double.parseDouble(newVal);
        } else
            return false;

        if (key.equals(GeoCrawlerKey.CELL_ENABLED)) {
            return locationCollector.CellEnable(boolVal);
        } else if (key.equals(GeoCrawlerKey.GPS_ENABLED)) {
            return locationCollector.GPSEnable(boolVal);
        } else if (key.equals(GeoCrawlerKey.GPS_ERROR)) {
            return locationCollector.setGPSErrorInMeters(dVal);
        } else if (key.equals(GeoCrawlerKey.GPS_TIMEOUT)) {
            return locationCollector.setGPSTimeoutInSeconds((int)(dVal + 0.5));
        } else if (key.equals(GeoCrawlerKey.UPDATE_AUTO)) {
            return locationCollector.setAutoRunMode(boolVal);
        } else if (key.equals(GeoCrawlerKey.UPDATE_INTERVAL)) {
            return locationCollector.setRunIntervalInMinutes((int)(dVal + 0.5));
        } else
            return false;
    }

    public void showError(String error, int nextState) {
        if (error == null)
            return;
        setError(error);
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
        errorDisplay.setError(error);
    }
}
