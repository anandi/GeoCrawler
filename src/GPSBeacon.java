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

    public GPSBeacon() {
        errorInMeter = 500; //Default error is 500 meters
        timeout = 60;
    }

    public Object getProperty(String property) {
        if (property.equals("timeoutInSecond")) {
            return new Integer(timeout);
        }
        return super.getProperty(property);
    }

    public boolean setProperty(String property, Object value) {
        if (property.equals("timeoutInSecond")) {
            timeout = ((Integer)value).intValue();
            return true;
        }
        return super.setProperty(property, value);
    }

    public boolean initialize() {
        Criteria cr= new Criteria();
        cr.setHorizontalAccuracy((int)(errorInMeter + 0.5));
        try {
            lp = LocationProvider.getInstance(cr);
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
