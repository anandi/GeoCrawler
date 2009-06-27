/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.microedition.location.Coordinates;
import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationProvider;

/**
 *
 * @author anandi
 */
public class GPSBeacon extends LocationBeacon {
    private LocationProvider lp;
    private int timeout;

    public GPSBeacon(double errorInMeter, int timeoutInSeconds) {
        if (errorInMeter > 0)
            this.errorInMeter = errorInMeter;
        else
            this.errorInMeter = 500; //Default error is 500 meters
        if (timeoutInSeconds > 0)
            timeout = timeoutInSeconds;
        else
            timeout = 60;

        Criteria cr= new Criteria();
        cr.setHorizontalAccuracy((int)(this.errorInMeter + 0.5));
        try {
            lp= LocationProvider.getInstance(cr);
        } catch (LocationException ex) {} //Do nothing!
    }

    public void setErrorInMeters(double error) {
        if (error > 0)
            this.errorInMeter = error;
    }

    public void setTimeoutInSeconds(int timeout) {
        if (timeout > 0)
            this.timeout = timeout;
    }

    public int getTimeoutInSeconds() {
        return timeout;
    }

    public boolean initialize() {
        Criteria cr= new Criteria();
        cr.setHorizontalAccuracy((int)(errorInMeter + 0.5));
        try {
            lp= LocationProvider.getInstance(cr);
        } catch (LocationException ex) {
            return false;
        }
        return true;
    }

    public boolean update() {
        Location l;
        Coordinates c;
        double lat = 0;
        double lon = 0;

        if (GeoCrawlerKey.GEO_CRAWLER_DEVEL_MODE) {
            setLatLon(GeoCrawlerKey.GPS_DEFAULT_LAT, GeoCrawlerKey.GPS_DEFAULT_LON);
            return true;
        }

        if (lp == null)
            return false;

        try {
            l = lp.getLocation(timeout);
            c = l.getQualifiedCoordinates();
        } catch (InterruptedException ie) {
            return false;
        } catch (LocationException le) {
            return false;
        }

        setLatLon(c.getLatitude(), c.getLongitude());
        return true;
    }
}
