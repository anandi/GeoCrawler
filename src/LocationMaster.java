/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Date;
/**
 *
 * @author anandi
 */
public class LocationMaster extends Thread implements ConfigListener {
    private static final String GPS_ENABLED = "GPS.enabled";
    private static final String GPS_ERROR = "GPS.error"; //In meters.
    private static final String GPS_TIMEOUT = "GPS.timeout"; //In seconds.
    private static final String CELL_ENABLED = "CELL.enabled";
    private static final String UPDATE_INTERVAL = "LM.minutes";
    private static final String UPDATE_AUTO = "LM.run";
    private static final String FIREEAGLE_QUERY = "SVC.fe.query";
    private static final String IP_QUERY = "SVC.ip.query";

    private int lock; //Protects location.
    private LocationData location;

    private boolean autoRun;
    private boolean initialized;
    private long interval; //Pause interval in milliseconds.
    private boolean running;

    private LocationConsumer consumer;
    private FireEagle fireEagle;
    private boolean queryFireEagle;
    private double gpsErrorInMeters;
    private int gpsTimeoutInSeconds;
    private Date lastRun;

    LocationBeacon gps;
    LocationBeacon cell;
    LocationBeacon ip;

    public LocationMaster(GeoCrawler app) {
        lock = 0;
        location = null;
        consumer = app;
        lastRun = null;

        initialized = false; //Indicate that we are not ready to run yet.
        PersistentConfig pc = app.getConfigStore();
        fireEagle = app.getFireEagle();

        //By default, do not query Fire Eagle for location.
        queryFireEagle = false;
        ConfigItem feQueryItem = new ConfigItem(FIREEAGLE_QUERY,
                                                "Query Fire Eagle for your location (if authorized) when no location can be found.",
                                                true, queryFireEagle, this);
        pc.registerConfigItem(feQueryItem);
        try {
            queryFireEagle = feQueryItem.getBoolValue();
        } catch (Exception e) {}

        ip = null;
        boolean useIP = false;
        ConfigItem ipItem = new ConfigItem(IP_QUERY,
                                           "Enable IP based location detection",
                                           true, useIP, this);
        pc.registerConfigItem(ipItem);
        try {
            useIP = ipItem.getBoolValue();
        } catch (Exception e) {}

        if (useIP)
            ip = LocationBeacon.instanceOf("IPBeacon");

        gps = null;
        boolean useGPS = true;
        ConfigItem gpsItem = new ConfigItem(GPS_ENABLED,
                                            "Enable GPS signal detection",
                                            true, useGPS, this);
        pc.registerConfigItem(gpsItem);
        try {
            useGPS = gpsItem.getBoolValue();
        } catch (Exception e) {} //Will not occur.

        gpsErrorInMeters = 500;
        pc.registerConfigItem(new ConfigItem(GPS_ERROR,
                                             "Allowed error radius for GPS detection in meters (min 10)",
                                             true, gpsErrorInMeters, this));
        gpsTimeoutInSeconds = 60;
        pc.registerConfigItem(new ConfigItem(GPS_TIMEOUT,
                                             "Maximum seconds for a GPS detection timeout",
                                             true, gpsTimeoutInSeconds, this));

        if (useGPS && (gps == null)) {
            gps = LocationBeacon.instanceOf("GPSBeacon");
            if (gps != null) {
                gps.setProperty("errorInMeter", new Double(gpsErrorInMeters));
                gps.setProperty("timeoutInSecond", new Integer(gpsTimeoutInSeconds));
            }
        }

        cell = null;
        boolean useCell = true;
        ConfigItem cellItem = new ConfigItem(CELL_ENABLED,
                                             "Enable cell tower location detection",
                                             true, useCell, this);
        pc.registerConfigItem(cellItem);
        try {
            useCell = cellItem.getBoolValue();
        } catch (Exception e) {}

        if (useCell && (cell == null))
            cell = LocationBeacon.instanceOf("CellIDBeacon");

        int updateIntervalInMinutes = 5;
        setRunIntervalInMinutes(updateIntervalInMinutes);
        pc.registerConfigItem(new ConfigItem(UPDATE_INTERVAL,
                                             "Interval in minutes between location detection",
                                             true, updateIntervalInMinutes, this));

        running = false;
        boolean autoUpdate = true;
        ConfigItem autoUpdateItem = new ConfigItem(UPDATE_AUTO,
                                             "Enable automatic location update",
                                                   true, autoUpdate, this);
        pc.registerConfigItem(autoUpdateItem);
        try {
            autoUpdate = autoUpdateItem.getBoolValue();
        } catch (Exception e) {}
        if (autoUpdate)
            setAutoRunMode(true); //Careful! This kicks off the run!!

        initialized = true;
    }

    public Date getLastRun() {
        return lastRun;
    }

    private void feQueryEnable(boolean val) {
        queryFireEagle = val;
    }

    private void GPSEnable(boolean val) {
        if (val && (gps == null)) {
            gps = LocationBeacon.instanceOf("GPSBeacon");
            if (gps == null) {
                gps.setProperty("errorInMeter", new Double(gpsErrorInMeters));
                gps.setProperty("timeoutInSecond", new Integer(gpsTimeoutInSeconds));
            }
        } else if (!(val || (gps == null)))
            gps = null; //Has race condition!
    }

    public boolean GPSEnabled() {
        return (gps == null) ? false : true;
    }

    private void CellEnable(boolean val) {
        if (val && (cell == null))
            cell = LocationBeacon.instanceOf("CellIDBeacon");
        else if (!(val || (cell == null)))
            cell = null; //Has race condition!
    }

    private void IPEnable(boolean val) {
        if (val && (ip == null))
            ip = LocationBeacon.instanceOf("IPBeacon");
        else if (!(val || (ip == null)))
            ip = null; //Has race condition!
    }

    public boolean CellEnabled() {
        return (cell == null) ? false : true;
    }

    private void setGPSErrorInMeters(double error) {
        if (error <= 10)
            return; //Doesn't take any effect! We need to propagate this back.
        if (gps != null)
            gps.setProperty("errorInMeter", new Double(error));
    }

    public double getGPSErrorInMeters() {
        return gpsErrorInMeters;
    }

    private void setGPSTimeoutInSeconds(int seconds) {
        if (seconds <= 0)
            return;
        if (gps != null)
            gps.setProperty("timeoutInSecond", new Integer(seconds));
    }

    public int getGPSTimeoutInSeconds() {
        return gpsTimeoutInSeconds;
    }

    private void setRunIntervalInMinutes(int mins) {
        if (mins < 1)
            return;
        interval = mins * 60 * 1000; //Same value in milliseconds.
    }

    public int getRunIntervalInMinutes() {
        return (int)(interval / (60 * 1000));
    }

    private boolean setAutoRunMode(boolean val) {
        autoRun = val;
        if (autoRun && initialized)
            this.run(); //Start the run.
        return true;
    }

    public boolean getAutoRunMode() {
        return autoRun;
    }

    private synchronized int updateLock(boolean increment) {
        if (increment)
            lock++;
        else
            lock--;
        return lock;
    }

    private boolean getLock() {
        int iterations = 0;
        while (updateLock(true) != 1) {
            updateLock(false); //This is a spinlock.
            iterations++;
            if (iterations > 5)
                return false;
            try {
                Thread.sleep(100); //100 millisecond sleep time between tries.
            } catch (InterruptedException ie) {}
        }

        return true;
    }

    private void releaseLock() {
        updateLock(false);
    }

    public boolean setLocation(double lat, double lon, double err) {
        return this.setLocation(new LocationData(lat, lon, err));
    }

    protected boolean setLocation(LocationData loc) {
        if (!getLock())
            return false;
        location = loc;
        releaseLock();
        consumer.locationCallback(location);
        return true;
    }

    public LocationData getLocation() {
        if (!getLock())
            return null;
        LocationData tmpLocation = location;
        releaseLock();
        return tmpLocation;
    }
    
    public boolean isRunning() {
        return running;
    }

    //The reason this is not part of the LocationBeacon class is because we want
    //to have a clear separation between the beacon (which is mostly external
    //format, and our own custom internal format.
    protected LocationData getLocationFromBeacon(LocationBeacon beacon) {
        double error = ((Double)beacon.getProperty("errorInMeter")).doubleValue();
        return new LocationData(beacon.getLatitude(), beacon.getLongitude(), error);
    }

    protected LocationData getUpdate() {
        LocationData loc = null;
        boolean gpsUpdate = false;

        //First try the GPS.
        if ((gps != null) && gps.update()) {
            loc = getLocationFromBeacon(gps);
            gpsUpdate = true;
            loc.source = "GPS";
        }

        if ((cell != null) && cell.update()) {
            if (loc == null) {
                loc = getLocationFromBeacon(cell);
                loc.source = "Cell ID";
            } else if (gpsUpdate)
                cell.annotateWithLatLon(loc.getLatitude(), loc.getLongitude());
        }

        if (ip != null && ip.update()) {
            if (loc == null) {
                loc = getLocationFromBeacon(ip);
                loc.source = "IP Address";
            } else if (gpsUpdate)
                ip.annotateWithLatLon(loc.getLatitude(), loc.getLongitude());
        }

        if (loc != null)
            return loc;

        //If we are here, then we could not find an update through the
        //beacons. Let us see if we can get something from Fire Eagle...
        if (queryFireEagle && (fireEagle != null)
            && (fireEagle.getState() == FireEagle.STATE_AUTHORIZED)) {
            loc = fireEagle.getLocation();
            loc.source = "Fire Eagle";
        }

        return loc;
    }

    //http://www.codeproject.com/KB/cs/distancebetweenlocations.aspx has some
    //code to determine the distance between two lat-lon points. Right now, I
    //am not going to get into implementations from that or others.
    //For the time being I am going to assume that 0.00135 degree difference
    //makes for 100 meters distance! (God help me for blasphemy!)
    private boolean checkUpdate(LocationData loc) {
        //Run some checks to see if the location has moved far away to
        //deserve an update!
        if (location == null)
            return true; //Update anyway!

        if (location.getDistance(loc) < loc.getErrorInMeters())
            //Currently set location is within the error radius of the latest
            //reading. Don't update.
            return false;

        //This would normally qualify for an update, except that it cannot
        //have happened too soon. If that is the case, then it means that
        //there was a manual update in between.
        long delayInMilli = loc.getTimeStamp().getTime()
                                            - location.getTimeStamp().getTime();
        if (delayInMilli < this.interval)
            return false;
        return true;
    }

    public void stop() {
        running = false;
    }

    public void run() {
        if (isRunning())
            return; //Already running.

        running = true;

        //Initialize all the beacons...
        if (gps != null)
            gps.initialize();
        if (cell != null)
            cell.initialize();
        if (ip != null)
            ip.initialize();

        //Atleast, try to get the Fire Eagle location if possible...
        if (queryFireEagle && (fireEagle != null)
            && (fireEagle.getState() == FireEagle.STATE_AUTHORIZED)) {
            LocationData loc = fireEagle.getLocation();
            if (loc != null) {
                loc.source = "Fire Eagle";
                setLocation(loc);
            }
        }

        boolean firstTime = true;

        while (autoRun && running) {
            LocationData loc = getUpdate();
            if (loc != null) {
                if (firstTime || checkUpdate(loc)) {
                    setLocation(loc);
                    firstTime = false;
                }
            }
            lastRun = new Date();

            if (location == null)
                consumer.detectFailed(); //Send a message saying we have no location.

            try {
                Thread.sleep(interval);
            } catch (InterruptedException ie) {} //Do nothing!. It is OK to run
                                                 //unscheduled once in a while.
        }

        if (location == null)
            consumer.detectFailed(); //Send a message saying we have no location.
    }

    public void notifyChanged(ConfigItem item) {
        try {
        if (item.getKey().equals(GPS_ENABLED))
            this.GPSEnable(item.getBoolValue());
        else if (item.getKey().equals(GPS_ERROR))
            this.setGPSErrorInMeters(item.getDoubleValue());
        else if (item.getKey().equals(GPS_TIMEOUT))
            this.setGPSTimeoutInSeconds(item.getIntValue());
        else if (item.getKey().equals(CELL_ENABLED))
            this.CellEnable(item.getBoolValue());
        else if (item.getKey().equals(UPDATE_INTERVAL))
            this.setRunIntervalInMinutes(item.getIntValue());
        else if (item.getKey().equals(UPDATE_AUTO))
            this.setAutoRunMode(item.getBoolValue());
        else if (item.getKey().equals(FIREEAGLE_QUERY))
            this.feQueryEnable(item.getBoolValue());
        else if (item.getKey().equals(IP_QUERY))
            this.IPEnable(item.getBoolValue());
        } catch (Exception e) {} //Shouldn't be thrown.
    }
}
