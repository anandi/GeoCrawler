/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class MapItem {
    private String description;
    private double latitude;
    private double longitude;

    //Note: x and y depend on the current center.
    public int x;
    public int y;

    MapItem(String _desc, double _lat, double _lon) {
        description = _desc;
        latitude = _lat;
        longitude = _lon;
    }
    
    public String getDescription() {
        return description;
    }

    public String getShortDescription(int length) {
        if (description.length() <= length)
            return description;
        return description.substring(0, length - 4).concat(" ...");
    }

    public LocationData getLocation() {
        return new LocationData(latitude, longitude, 0);
    }

    protected void updateLocation(double lat, double lon) {
        latitude = lat;
        longitude = lon;
    }

    public double getLat() {
        return latitude;
    }

    public double getLon() {
        return longitude;
    }
}
