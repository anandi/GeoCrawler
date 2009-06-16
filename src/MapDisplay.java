
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
//import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
//import javax.microedition.lcdui.ImageItem;
import java.io.IOException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class MapDisplay extends DisplayModule {
    private static final String YAHOO_REST_MAP_URL = "http://local.yahooapis.com/MapsService/V1/mapImage?appid=";
    private static final int DEFAULT_ZOOM = 3;
    private static final int MAX_ZOOM = 10;
    private static final double MAX_DEGREE_SHIFT = 8.0; /* Atmost 8 degree shift
                                                  * of lat or lon at max zoom */

    //The following commands are received on the map canvas.
    public static final int COMMAND_ZOOM_OUT = 0;
    public static final int COMMAND_ZOOM_IN = 1;
    public static final int COMMAND_EAST = 2;
    public static final int COMMAND_WEST = 3;
    public static final int COMMAND_NORTH = 4;
    public static final int COMMAND_SOUTH = 5;
    public static final int COMMAND_SET_LOCATION = 6;

    private MapCanvas canvas;
    private LocationData currentLoc;
    private int width;
    private int height;
    private String widthString;
    private String heightString;
    private Image mapImage;
    private String mapURL; //This is the actual URL to get the image.
    private String mapRequestURL; //This is the silly one level redirection to get
                                    // the XML containing the image URL!

    private Command manualCommand; //Go to manual input screen.
    private Command configCommand; //Go to configuration screen.
    private Command resetCommand; //Go back to current location.

    private int currentZoom; /* Please be aware that this is not a default zoom
                              * level offered by the map image service. This is
                              * the radius (well, the actual radius is
                              * 2^currentZoom) in kilometers. I need this hack
                              * not just because Yahoo offers this, but because
                              * I need to plot other things on this map later.*/

    private double shift_lat;
    private double shift_lon;

    public MapDisplay(GeoCrawler app) {
        super(app);

        manualCommand = new Command("Manual location...", Command.SCREEN, 1);
        configCommand = new Command("Config", Command.SCREEN, 1);
        resetCommand = new Command("Current location", Command.SCREEN, 1);

        shift_lat = 0.0;
        shift_lon = 0.0;

        canvas = new MapCanvas(this);
        canvas.addCommand(this.getExitCommand());
        canvas.addCommand(manualCommand);
        canvas.addCommand(configCommand);
        canvas.addCommand(this.getHomeCommand());
        canvas.setCommandListener(this);

        //Till we use Canvas and do getWidth and getHeight!
//        height = form.getHeight();
//        width = form.getWidth();
        height = canvas.getHeight();
        width = canvas.getWidth();
        heightString = Integer.toString(height);
        widthString = Integer.toString(width);

        mapURL = null;
        mapImage = null;

        currentZoom = DEFAULT_ZOOM;

        currentLoc = null;
    }

    public void display(int prevState) {
        if (prevState != GeoCrawler.STATE_MAP)
            previousState = prevState;

        if (currentLoc == null) {
            //Map not yet ready to be drawn.
            app.showError("Currently no location can be identified. If GPS is turned on, please wait for some time or check your configuration.", GeoCrawler.STATE_BEGIN);
            return;
        }

        if ((shift_lat != 0) || (shift_lon != 0))
            canvas.addCommand(resetCommand);
        else
            canvas.removeCommand(resetCommand);

        String url = imageURL();
        if (url == null) {
            if (mapImage == null)
                //We can't get an URL and there is no map to display.
                app.showError("Could not communicate with Yahoo! maps service. Is your network connection enabled?", GeoCrawler.STATE_BEGIN);
            else {
                url = mapURL; //Don't shift the map.
                app.setError("Failed to retrieve image URL for ("+Double.toString(this.getDisplayLat())+","+Double.toString(this.getDisplayLon())+")");
            }
            return;
        }
        
        if ((mapURL == null) || !url.equals(mapURL)) {
            Image img = null;
            try {
                img = HTTPUtil.loadImage(url);
            } catch (IOException ioe) {
                img = null; //Make sure that the image is null.
            }

            if (img != null) {
                mapImage = img;
                mapURL = url;
            } else
                app.setError("Failed to load map image for ("+Double.toString(this.getDisplayLat())+","+Double.toString(this.getDisplayLon())+")");

            if (mapImage == null) {
                app.showError("Failed to load map image for ("+Double.toString(this.getDisplayLat())+","+Double.toString(this.getDisplayLon())+")", GeoCrawler.STATE_BEGIN);
                return;
            }
        }
        canvas.setImage(mapImage);

        if (prevState != GeoCrawler.STATE_MAP)
            app.getDisplay().setCurrent(canvas);
        else
            canvas.repaint();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == getExitCommand())
            app.handleNextState(GeoCrawler.STATE_EXIT);
        else if (c == getHomeCommand())
            app.handleNextState(GeoCrawler.STATE_BEGIN);
        else if (c == manualCommand)
            app.handleNextState(GeoCrawler.STATE_MANUAL);
        else if (c == configCommand)
            app.handleNextState(GeoCrawler.STATE_CONFIG);
        else if (c == resetCommand) {
            shift_lat = 0.0;
            shift_lon = 0.0;
            currentZoom = MapDisplay.DEFAULT_ZOOM;
            app.handleNextState(GeoCrawler.STATE_MAP);
        } else
            System.out.println("Unknown command received in map form.");
    }

    public void setLocation(LocationData loc) {
        currentLoc = loc;
        shift_lat = 0.0;
        shift_lon = 0.0;
        currentZoom = MapDisplay.DEFAULT_ZOOM;
    }

    /*Should really be using the YDN APIs here. There are some Y!Go code to
      handle map bounding boxes...
     *  http://svn.corp.yahoo.com/view/yahoo/platform/ygeo/ajaxapi/branches/pretrunk_build/Maps/Mercator/Mercator.js?revision=5257&view=co
rmsguhan: in the file i gave you, please look at Mercator.prototype.ll_to_tile
rmsguhan: this accepts the center lat lon, or Geo point , which is the instance of the GeoPoint class
rmsguhan: var getTileInfo = function(gp,z) {
        var mo = (z && z!=zoomLevel)? (new Mercator(z)) : MP;
        var txy = mo.ll_to_tile(gp);
        var tll = mo.xy_to_ll(txy.tx, txy.ty, 0, 0);
        var pointpxy = mo.ll_to_pxy(gp.Lat, gp.Lon);
        var tilepxy = mo.ll_to_pxy(tll.Lat, tll.Lon);
        return {xy: txy, ll: tll, cp: {x: pointpxy.x-tilepxy.x, y: tilepxy.y-pointpxy.y}};
    };
rmsguhan: this code might give the tile info
rmsguhan: all this is in the Map API
rmsguhan: http://vault.yahoo.com/viewcvs/yahoo/clife/devices/j2me/src/com/yahoo/go/skin/YGoMapControl.java?revision=1.116&view=markup
     */
    private String imageURL() {
        //Using Yahoo REST MAP service!
        String url = MapDisplay.YAHOO_REST_MAP_URL;
        url = url.concat(GeoCrawlerKey.YAHOO_REST_MAP_KEY);
        url = url.concat("&latitude=").concat(Double.toString(getDisplayLat()));
        url = url.concat("&longitude=").concat(Double.toString(getDisplayLon()));
        int radiusInKm = (1 << currentZoom);
        System.err.println("Radius in KM: "+Integer.toString(radiusInKm));
        double radiusInMiles = ((double)radiusInKm) * 0.621371192;
        System.err.println("Radius in miles: "+Double.toString(radiusInMiles));
        url = url.concat("&radius=").concat(Double.toString(radiusInMiles));
        url = url.concat("&image_width=").concat(widthString);
        url = url.concat("&image_height=").concat(heightString);
        url = url.concat("&image_type=png");

        if ((mapRequestURL != null) && (mapURL != null)
            && url.equals(mapRequestURL))
            return mapURL; //No need to get fresh.

        //Get the XML!
//        System.err.println("Trying to fetch: "+url);
        String responseXML = null;
        try {
            responseXML = HTTPUtil.httpGetRequest(url);
        } catch (IOException ioe) {
            System.err.println("HTTP GET Failed.");
            return null;
        }

        if (responseXML == null)
            return null;

//        System.err.println("HTTP GET Response: "+responseXML);
        //Do a naive parse for this!
        int beginPos = responseXML.indexOf("<Result");
        if (beginPos == -1) {
            System.err.println("Invalid beginning of response");
            return null;
        }
        beginPos = responseXML.indexOf('>', beginPos);
        if (beginPos == -1) {
            System.err.println("Couldnot find beginning of the URL");
            return null;
        }
        beginPos++;
        int endPos = responseXML.indexOf('<', beginPos);
        if (endPos == -1) {
            System.err.println("Could not find end of the URL");
            return null;
        }
        String imageURL = responseXML.substring(beginPos, endPos - 1);
//        System.err.println("Image URL: "+imageURL);
        if (imageURL.indexOf("http:") != 0) {
            System.err.println("Image URL does not begin with http");
            return null;
        }
        mapRequestURL = url;
        return imageURL;
    }

    public void mapCommand(int command) {
        double shift;
        double temp;

        switch(command) {
            case MapDisplay.COMMAND_ZOOM_IN:
                if (currentZoom > 0) {
                    currentZoom--;
                    app.handleNextState(GeoCrawler.STATE_MAP);
                }
                break;
            case MapDisplay.COMMAND_ZOOM_OUT:
                if (currentZoom < MapDisplay.MAX_ZOOM) {
                    currentZoom++;
                    app.handleNextState(GeoCrawler.STATE_MAP);
                }
                break;
            case MapDisplay.COMMAND_EAST:
                shift = getShift();
                temp = shift_lon + shift;
                if (temp > 180)
                    temp = temp - 360;
                else if (temp < -180)
                    temp = temp + 360;
                shift_lon = temp;
                app.handleNextState(GeoCrawler.STATE_MAP);
                break;
            case MapDisplay.COMMAND_WEST:
                shift = getShift();
                temp = shift_lon - shift;
                if (temp > 180)
                    temp = temp - 360;
                else if (temp < -180)
                    temp = temp + 360;
                shift_lon = temp;
                app.handleNextState(GeoCrawler.STATE_MAP);
                break;
            case MapDisplay.COMMAND_NORTH:
                shift = getShift();
                temp = shift_lat + shift;
                if (temp > 90)
                    temp = 90;
                else if (temp < -90)
                    temp = -90;
                shift_lat = temp;
                app.handleNextState(GeoCrawler.STATE_MAP);
                break;
            case MapDisplay.COMMAND_SOUTH:
                shift = getShift();
                temp = shift_lat - shift;
                if (temp > 90)
                    temp = 90;
                else if (temp < -90)
                    temp = -90;
                shift_lat = temp;
                app.handleNextState(GeoCrawler.STATE_MAP);
                break;
            case MapDisplay.COMMAND_SET_LOCATION:
                LocationData loc = new LocationData(getDisplayLat(), getDisplayLon(),
                                                    500); /* FIXME */
                app.getCollector().setLocation(loc);
                app.handleNextState(GeoCrawler.STATE_MAP);
                break;
            default:
        }
    }

    private double getShift() {
        int divisor = 1 << (MapDisplay.MAX_ZOOM - currentZoom);
        double shift = MapDisplay.MAX_DEGREE_SHIFT / ((double)divisor);
        return shift;
    }

    private double getDisplayLat() {
        double lat = currentLoc.getLatitude() + shift_lat;
        if (lat > 90)
            lat = 90;
        else if (lat < -90)
            lat = -90;
        return lat;
    }

    private double getDisplayLon() {
        double lon = currentLoc.getLongitude() + shift_lon;
        if (lon > 180)
            lon = lon - 360;
        else if (lon < -180)
            lon = lon + 360;
        return lon;
    }
}
