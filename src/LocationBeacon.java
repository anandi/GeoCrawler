/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class LocationBeacon {
    private double lat;
    private double lon;
    protected double errorInMeter;

    public static LocationBeacon instanceOf(String str) {
        try {
            if (str.equals("GPSBeacon")) {
                //Check location package before trying to instantiate a beacon.
                Class l = Class.forName("javax.microedition.location.Criteria");
            }
            Class c = Class.forName(str);
            return (LocationBeacon)(c.newInstance());
        } catch (IllegalAccessException ex) {
            System.err.println("Class "+str+" could not be instantiated as a beacon");
        } catch (InstantiationException ex) {
            System.err.println("Class "+str+" could not be instantiated as a beacon");
        } catch (ClassNotFoundException ex) {
            System.err.println("Class "+str+" could not be instantiated as a beacon");
        }

        return null;
    }

    public LocationBeacon() {
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lon;
    }

    protected void setLatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public boolean update() {
        return false;
    }

    public boolean initialize() {
        return true; //By default, nothing to do.
    }

    //For any beacon other than GPS, it is possible to annotate the beacon with
    //some lat-lon pair if possible to get them. This is an optional method,
    //not related to the core running of the application.
    public void annotateWithLatLon(double lat, double lon) {
        //Do nothing by default.
    }

    //For any beacon other than GPS, the beacon needs to be translated to a
    //workable lat-lon pair. This pair may not always be a precise location of
    //the user's location. Instead, it is a centroid of a known area.
    //Note that this is provided in case the location is not a direct update
    //from the device (like a manual input)
    public boolean resolve() {
        return true;
    }

    public boolean setProperty(String property, Object value) {
        if (property.equals("errorInMeter")) {
            errorInMeter = ((Double)value).doubleValue();
            return true;
        }
        return false;
    }

    public Object getProperty(String property) {
        if (property.equals("errorInMeter")) {
            return new Double(errorInMeter);
        }
        return null;
    }
}
