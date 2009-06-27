/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Date;
import akme.mobile.util.MathUtil;

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
        return getDistance(loc.lat, loc.lon);
    }

    public double getDistance(double other_lat, double other_lon) {
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
        double dLat2InRad = Math.toRadians(other_lat);
        double dLong2InRad = Math.toRadians(other_lon);

        double dLongitude = dLong2InRad - dLong1InRad;
        double dLatitude = dLat2InRad - dLat1InRad;

        // Intermediate result a.
        double sin_dLatitude = Math.sin(dLatitude / 2.0);
        double sin_dLongitude = Math.sin(dLongitude / 2.0);
        double a = (sin_dLatitude * sin_dLatitude) +
                   Math.cos(dLat1InRad) * Math.cos(dLat2InRad) *
                   (sin_dLongitude * sin_dLongitude);

        // Intermediate result c (great circle distance in Radians).
        double c = 2.0 * MathUtil.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        // Distance.
        dDistance = LocationData.EARTH_RADIUS_IN_KM * c;

        return dDistance;
    }

    public double getXDistance(LocationData other) {
        if (other.lon == this.lon)
            return 0;
        LocationData other1 = new LocationData(this.lat, other.lon, 0);
        double distance = getDistance(other1);
        //Now, get the sign!
        if (this.lon > other.lon)
            return -1 * distance; //X projection is negative.

        return distance;
    }

    public double getYDistance(LocationData other) {
        if (other.lat == this.lat)
            return 0;
        LocationData other1 = new LocationData(other.lat, this.lon, 0);
        double distance = getDistance(other1);
        //Now, get the sign!
        if (this.lat > other.lat)
            return -1 * distance; //X projection is negative.

        return distance;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof LocationData))
            return false;
        LocationData other = (LocationData)obj;
        if ((this.lat == other.lat) && (this.lon == other.lon))
            return true;
        return false;
    }

    /* This function needs the distance in KM and the angle in degrees with
     * NORTH as 0-degree, increasing anti-clockwise. So, EAST is -90-degree
     * or 270 degree.
     * Algorithm source: http://williams.best.vwh.net/avform.htm#LL
     */

    public LocationData getLatLonAt(double distanceInKm, double angleInDegree) {
        double greatCircleDistanceInRadian = distanceInKm/EARTH_RADIUS_IN_KM;
        double angleInRadian = Math.toRadians(angleInDegree);
        double latInRadian = Math.toRadians(lat);
        double lonInRadian = Math.toRadians(lon);

        double sinOfDistance = Math.sin(greatCircleDistanceInRadian); //reused

        double newLatInRadian = MathUtil.asin((Math.sin(latInRadian) * Math.cos(greatCircleDistanceInRadian))
                                           + (Math.cos(latInRadian) * sinOfDistance * Math.cos(angleInRadian)));
        double newLonInRadian = lonInRadian;
        double cosOfLat = Math.cos(newLatInRadian); //reused
        if (cosOfLat != 0) {
            //Not a pole
            newLonInRadian = mod(lonInRadian
                                      - MathUtil.asin(Math.sin(angleInRadian) * sinOfDistance / cosOfLat)
                                      + Math.PI, 2 * Math.PI) - Math.PI;
        }

        return new LocationData(Math.toDegrees(newLatInRadian),
                                                  Math.toDegrees(newLonInRadian),
                                                  this.getErrorInMeters());
    }

    //Hopefully, this is correct implementation of a non-integral mod!
    public static double mod(double x, double y) {
        if ((y == 0) || (x == Double.NaN) || (y == Double.NaN))
            return Double.NaN;
        double q = Math.floor(x / y);
        return x - (q * y);
    }
}
