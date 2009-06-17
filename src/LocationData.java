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
    // const Double kEarthRadiusMiles = 3956.0;
    public static final double EARTH_RADIUS_IN_KM = 6376.5;
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

    //Instead of throwing exception, this returns a Double.NaN on error.
    //Code is taken from
    //http://www.codeproject.com/KB/cs/distancebetweenlocations.aspx
    public double getDistance(LocationData loc) {
        /*
            The Haversine formula according to Dr. Math.
            http://mathforum.org/library/drmath/view/51879.html

            dlon = lon2 - lon1
            dlat = lat2 - lat1
            a = (sin(dlat/2))^2 + cos(lat1) * cos(lat2) * (sin(dlon/2))^2
            c = 2 * atan2(sqrt(a), sqrt(1-a))
            d = R * c

            Where
                * dlon is the change in longitude
                * dlat is the change in latitude
                * c is the great circle distance in Radians.
                * R is the radius of a spherical Earth.
                * The locations of the two points in
                    spherical coordinates (longitude and
                    latitude) are lon1,lat1 and lon2, lat2.
        */
        double dDistance = Double.NaN;
        double dLat1InRad = Math.toRadians(lat);
        double dLong1InRad = Math.toRadians(lon);
        double dLat2InRad = Math.toRadians(loc.lat);
        double dLong2InRad = Math.toRadians(loc.lon);

        double dLongitude = dLong2InRad - dLong1InRad;
        double dLatitude = dLat2InRad - dLat1InRad;

        // Intermediate result a.
        double sin_dLatitude = Math.sin(dLatitude / 2.0);
        double sin_dLongitude = Math.sin(dLongitude / 2.0);
        double a = (sin_dLatitude * sin_dLatitude) +
                   Math.cos(dLat1InRad) * Math.cos(dLat2InRad) *
                   (sin_dLongitude * sin_dLongitude);

        // Intermediate result c (great circle distance in Radians).
        double c = 2.0 * LocationData.aTan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        // Distance.
        dDistance = LocationData.EARTH_RADIUS_IN_KM * c;

        return dDistance;
    }

    //I wouldn't exactly vouch for this... but, I found it at:
    //http://www.gamedev.net/community/forums/topic.asp?topic_id=441464 and it
    //refers to http://dspguru.com/comp.dsp/tricks/alg/fxdatan2.htm
    public static double aTan2(double y, double x) {
	double coeff_1 = Math.PI / 4.0;
	double coeff_2 = 3d * coeff_1;
	double abs_y = Math.abs(y);
	double angle;
	if (x >= 0d) {
		double r = (x - abs_y) / (x + abs_y);
		angle = coeff_1 - coeff_1 * r;
	} else {
		double r = (x + abs_y) / (abs_y - x);
		angle = coeff_2 - coeff_1 * r;
	}
	return y < 0d ? -angle : angle;
    }


}
