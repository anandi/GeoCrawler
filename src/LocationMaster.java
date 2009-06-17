/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class LocationMaster extends Thread {
    private int lock; //Protects location.
    private LocationData location;

    private PersistentConfig configStore;
    private boolean autoRun;
    private boolean initialized;
    private long interval; //Pause interval in milliseconds.
    private boolean running;

    LocationConsumer consumer;
    private double gpsErrorInMeters;
    private int gpsTimeoutInSeconds;
    GPSBeacon gps;
    CellIDBeacon cell;

    public LocationMaster(PersistentConfig pc, LocationConsumer lc) {
        lock = 0;
        location = null;
        consumer = lc;
        configStore = pc;

        initialized = false; //Indicate that we are not ready to run yet.

        String s;
        s = configStore.getConfigString(GeoCrawlerKey.GPS_ENABLED);
        boolean useGPS = true;
        if (s == null)
            configStore.setConfigString(GeoCrawlerKey.GPS_ENABLED, "true");
        else
            useGPS = (s.equals("true")) ? true : false;

        gpsErrorInMeters = 500;
        gpsTimeoutInSeconds = 60;
        s = configStore.getConfigString(GeoCrawlerKey.GPS_ERROR);
        if (s == null)
            setGPSErrorInMeters(gpsErrorInMeters);
        else
            gpsErrorInMeters = Double.parseDouble(s);
        s = configStore.getConfigString(GeoCrawlerKey.GPS_TIMEOUT);
        if (s == null)
            this.setGPSTimeoutInSeconds(gpsTimeoutInSeconds);
        else
            gpsTimeoutInSeconds = Integer.parseInt(s);

        if (useGPS)
            gps = new GPSBeacon(gpsErrorInMeters, gpsTimeoutInSeconds);
        else
            gps = null;

        s = configStore.getConfigString(GeoCrawlerKey.CELL_ENABLED);
        boolean useCell = true;
        if (s == null)
            configStore.setConfigString(GeoCrawlerKey.CELL_ENABLED, "true");
        else
            useCell = (s.equals("true")) ? true : false;

        if (useCell)
            cell = new CellIDBeacon();
        else
            cell = null;

        s = configStore.getConfigString(GeoCrawlerKey.UPDATE_INTERVAL);
        if (s == null)
            setRunIntervalInMinutes(5);
        else
            interval = Integer.parseInt(s) * 60 * 1000;

        s = configStore.getConfigString(GeoCrawlerKey.UPDATE_AUTO);
        running = false;
        if (s == null)
            this.setAutoRunMode(true); //Careful! This kicks off the run!!
        else
            autoRun = (s.equals("true")) ? true : false;

        initialized = true;
    }

    public boolean GPSEnable(boolean val) {
        if (!configStore.setConfigString(GeoCrawlerKey.GPS_ENABLED, (val) ? "true" : "false"))
            return false;
        if (val && (gps == null))
            gps = new GPSBeacon(gpsErrorInMeters, gpsTimeoutInSeconds);
        else if (!(val || (gps == null)))
            gps = null; //Has race condition!
        return true;
    }

    public boolean GPSEnabled() {
        return (gps == null) ? false : true;
    }

    public boolean CellEnable(boolean val) {
        if (!configStore.setConfigString(GeoCrawlerKey.CELL_ENABLED, (val) ? "true" : "false"))
            return false;
        if (val && (cell == null))
            cell = new CellIDBeacon();
        else if (!(val || (cell == null)))
            cell = null; //Has race condition!
        return true;
    }

    public boolean CellEnabled() {
        return (cell == null) ? false : true;
    }

    public boolean setGPSErrorInMeters(double error) {
        if (error <= 10)
            return false;
        if (!configStore.setConfigString(GeoCrawlerKey.GPS_ERROR, Double.toString(error)))
            return false;
        if (gps != null)
            gps.setErrorInMeters(error);
        return true;
    }

    public double getGPSErrorInMeters() {
        return gpsErrorInMeters;
    }

    public boolean setGPSTimeoutInSeconds(int seconds) {
        if (seconds <= 0)
            return false;
        if (!configStore.setConfigString(GeoCrawlerKey.GPS_TIMEOUT, Integer.toString(seconds)))
            return false;
        if (gps != null)
            gps.setTimeoutInSeconds(seconds);
        return true;
    }

    public int getGPSTimeoutInSeconds() {
        return gpsTimeoutInSeconds;
    }

    public boolean setRunIntervalInMinutes(int mins) {
        if (mins < 1)
            return false;
        if (!configStore.setConfigString(GeoCrawlerKey.UPDATE_INTERVAL, Integer.toString(mins)))
            return false;
        interval = mins * 60 * 1000; //Same value in milliseconds.
        return true;
    }

    public int getRunIntervalInMinutes() {
        return (int)(interval / (60 * 1000));
    }

    public boolean setAutoRunMode(boolean val) {
        if (!configStore.setConfigString(GeoCrawlerKey.UPDATE_AUTO, (val) ? "true" : "false"))
            return false;
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
        System.err.println("LocationMaster thread running...");
        while (autoRun && running) {
            LocationData loc = this.getUpdate();
            if ((loc != null) && checkUpdate(loc))
                setLocation(loc);

            try {
                Thread.sleep(interval);
            } catch (InterruptedException ie) {} //Do nothing!. It is OK to run
                                                 //unscheduled once in a while.
        }
        System.err.println("LocationMaster thread stopped...");
    }
}
