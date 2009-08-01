/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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

    private int lock; //Protects location.
    private LocationData location;

    private boolean autoRun;
    private boolean initialized;
    private long interval; //Pause interval in milliseconds.
    private boolean running;

    private LocationConsumer consumer;
    private FireEagle fireEagle;
    private double gpsErrorInMeters;
    private int gpsTimeoutInSeconds;
    GPSBeacon gps;
    CellIDBeacon cell;

    public LocationMaster(GeoCrawler app) {
        lock = 0;
        location = null;
        consumer = app;

        initialized = false; //Indicate that we are not ready to run yet.

        gps = null;
        boolean useGPS = true;
        ConfigItem gpsItem = new ConfigItem(GPS_ENABLED,
                                            "Enable GPS signal detection",
                                            true, useGPS, this);
        PersistentConfig pc = app.getConfigStore();
        pc.registerConfigItem(gpsItem);
        fireEagle = app.getFireEagle();
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

        if (useGPS && (gps == null))
            gps = new GPSBeacon(gpsErrorInMeters, gpsTimeoutInSeconds);

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
            cell = new CellIDBeacon();

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

    private void GPSEnable(boolean val) {
        if (val && (gps == null))
            gps = new GPSBeacon(gpsErrorInMeters, gpsTimeoutInSeconds);
        else if (!(val || (gps == null)))
            gps = null; //Has race condition!
    }

    public boolean GPSEnabled() {
        return (gps == null) ? false : true;
    }

    private void CellEnable(boolean val) {
        if (val && (cell == null))
            cell = new CellIDBeacon();
        else if (!(val || (cell == null)))
            cell = null; //Has race condition!
    }

    public boolean CellEnabled() {
        return (cell == null) ? false : true;
    }

    private void setGPSErrorInMeters(double error) {
        if (error <= 10)
            return; //Doesn't take any effect! We need to propagate this back.
        if (gps != null)
            gps.setErrorInMeters(error);
    }

    public double getGPSErrorInMeters() {
        return gpsErrorInMeters;
    }

    private void setGPSTimeoutInSeconds(int seconds) {
        if (seconds <= 0)
            return;
        if (gps != null)
            gps.setTimeoutInSeconds(seconds);
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

    protected LocationData getUpdate() {
        //First try the GPS.
        if (gps != null) {
            if (gps.update()) {
                LocationData loc = new LocationData(gps.getLatitude(),
                                    gps.getLongitude(), gps.getErrorInMeter());
                //Give back to where we get from ...
                if (CellEnabled() && (cell != null)) {
                    if (cell.update()) {
                        //We have a valid cell tower identity.
                        cell.updateService(loc.getLatitude(), loc.getLongitude());
                    }
                }
                return loc;
            }
        }

        if (cell != null) {
            if (cell.update()) {
                //We also need to resolve!
                if (cell.resolve()) {
                    LocationData loc = new LocationData(cell.getLatitude(),
                                    cell.getLongitude(), cell.getErrorInMeter());
                    return loc;
                }
            }
        }

        //If we are here, then we could not find an update through the
        //beacons. Let us see if we can get something from Fire Eagle...
        if ((fireEagle != null) && (fireEagle.getState() == FireEagle.STATE_AUTHORIZED)) {
            LocationData loc = fireEagle.getLocation();
            return loc;
        }

        return null;
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

        //Atleast, try to get the Fire Eagle location if possible...
        if ((fireEagle != null) && (fireEagle.getState() == FireEagle.STATE_AUTHORIZED)) {
            LocationData loc = fireEagle.getLocation();
            if (loc != null)
                setLocation(loc);
        }

        while (autoRun && running) {
            LocationData loc = getUpdate();
            if ((loc != null) && checkUpdate(loc))
                setLocation(loc);

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
        } catch (Exception e) {} //Shouldn't be thrown.
    }
}
