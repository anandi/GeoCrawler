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

    public LocationBeacon() {
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lon;
    }

    public double getErrorInMeter() {
        return errorInMeter;
    }

    protected void setLatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public boolean update() {
        return false;
    }
}
