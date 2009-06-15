/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Date;

/**
 *
 * @author anandi
 */
public class LocationData {
    private double lat;
    private double lon;
    private double errInMeters;
    Date timeStamp;

    LocationData(double lat, double lon, double error) {
        this.lat = lat;
        this.lon = lon;
        this.errInMeters = error;
        timeStamp = new Date();
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lon;
    }

    public double getErrorInMeters() {
        return errInMeters;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long epoch) {
        timeStamp.setTime(epoch);
    }

    public void setTimeStamp(Date date) {
        timeStamp = date;
    }
}
