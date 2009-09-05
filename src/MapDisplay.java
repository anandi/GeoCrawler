
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class MapDisplay extends DisplayModule implements ConfigListener {
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
    public static final int COMMAND_SWITCH_MODE = 7;

    //The following controls the state of the map display
    public static final int MODE_SIMPLE = 0; //Just show the map and overlays.
    public static final int MODE_DISPLAY_CURRENT = 1; //Display the current location string.
    public static final int MODE_ROAMING = 2; //Transfer focus to the map items.

    public static final int MAX_MAP_ITEMS = 10; //Completely arbitrary.

    private MapCanvas canvas;
    private int width;
    private int height;
    private String widthString;
    private String heightString;
    private Image mapImage;
    private String mapURL; //This is the actual URL to get the image.
    private String mapRequestURL; //This is the silly one level redirection to get
                                    // the XML containing the image URL!

    private Command manualCommand; //Go to manual input screen.

    private int currentZoom; /* Please be aware that this is not a default zoom
                              * level offered by the map image service. This is
                              * the radius (well, the actual radius is
                              * 2^currentZoom) in kilometers. I need this hack
                              * not just because Yahoo offers this, but because
                              * I need to plot other things on this map later.*/

    private double shift_lat;
    private double shift_lon;

    private int displayMode;

    private Hashtable mapItemSources;
    private MapItemUpdater itemUpdaterThread;

    public MapDisplay(GeoCrawler app) {
        super(app);

        manualCommand = new Command("Manual location...", Command.SCREEN, 1);

        shift_lat = 0.0;
        shift_lon = 0.0;

        canvas = new MapCanvas(this);
        canvas.addCommand(manualCommand);
        canvas.addCommand(this.getBackCommand());
        canvas.setCommandListener(this);

        height = canvas.getHeight();
        width = canvas.getWidth();
        heightString = Integer.toString(height);
        widthString = Integer.toString(width);

        mapURL = null;
        mapImage = null;

        currentZoom = DEFAULT_ZOOM;

        mapItemSources = new Hashtable();
        itemUpdaterThread = null;
        displayMode = MODE_DISPLAY_CURRENT;
    }

    protected void setDisplayMode(int mode) {
        boolean needsRepaint = false;
        switch(mode) {
            case MODE_SIMPLE:
                displayMode = mode;
                if (canvas != null) {
                    canvas.clearDisplayText();
                    needsRepaint = true;
                }
                break;
            case MODE_DISPLAY_CURRENT:
                displayMode = mode;
                if (setCurrentAddressInDisplay())
                    needsRepaint = true;
                break;
            case MODE_ROAMING:
                if ((canvas != null) && canvas.hasMapItems()) {
                    displayMode = mode;
                    canvas.clearMapItemHighlight();
                    setNextMapItemHighlight(true);
                    needsRepaint = true;
                }
                break;
            default:
                break;
        }

        if (needsRepaint && (app.getDisplay().getCurrent() == canvas))
            canvas.repaint(); //We have already checked that canvas is not null.
    }

    private boolean isCenteredOnCurrentLocation() {
        if ((shift_lat == 0.0) && (shift_lon == 0.0))
            return true;
        return false;
    }

    private boolean setCurrentAddressInDisplay() {
        String address = app.getCurrentLocation().getAddress();
        if (isCenteredOnCurrentLocation() && (address != null)
            && (canvas != null)) {
            canvas.setDisplayText(address);
            return true; //Success
        }

        return false; //Failure.
    }

    public void display(int prevState) {
        if (prevState != GeoCrawler.STATE_MAP)
            previousState = prevState;

        LocationData currentLoc = app.getCurrentLocation();
        if (currentLoc == null) {
            //Map not yet ready to be drawn.
            app.showError("Currently no location can be identified. If GPS is turned on, please wait for some time or check your configuration.", GeoCrawler.STATE_BEGIN);
            return;
        }

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

        //Before we actually draw on the canvas, we need to set the string to be
        //displayed if the mode warrants it.
        if (displayMode != MODE_ROAMING)
            canvas.clearDisplayText();
        if (displayMode == MODE_DISPLAY_CURRENT)
            setCurrentAddressInDisplay();

        //Yes. I know that this is inefficient! Sigh!
        updateMapItemsInCanvas();

        if (prevState != GeoCrawler.STATE_MAP)
            app.getDisplay().setCurrent(canvas);
        else
            canvas.repaint();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == getBackCommand())
            app.handleNextState(GeoCrawler.STATE_BEGIN);
        else if (c == manualCommand)
            app.handleNextState(GeoCrawler.STATE_MANUAL);
        else {
            String label = c.getLabel();
            Enumeration e = this.mapItemSources.elements();
            boolean found = false;
            while (e.hasMoreElements()) {
                MapItemSource s = (MapItemSource)e.nextElement();
                String name = s.getServiceName();
                if (label.equals(name)) {
                    found = true;
                    if (s.getCustomDisplayState() == MapItemSource.CUSTOM_STATE_NONE) {
                        s.setRefreshMode(MapItemSource.REFRESH_REQUESTED);
                        this.refreshMapItems();
                    } else
                        app.handleNextState(s.getCustomDisplayState());
                }
            }
            if (!found)
                app.setError("Unknown command received in map context.");
        }
    }

    protected void refreshMapItems() {
        if (itemUpdaterThread != null) {
            //A background thread is running to update map items. Stop it!
            itemUpdaterThread.stop();
            itemUpdaterThread = null;
        }

        //Kick off the background updater...
        itemUpdaterThread = new MapItemUpdater(mapItemSources, this,
                                               app.getCurrentLocation());
        itemUpdaterThread.start();
    }

    public void notifyLocationUpdate() {
        shift_lat = 0.0;
        shift_lon = 0.0;
        currentZoom = MapDisplay.DEFAULT_ZOOM;
        displayMode = MODE_DISPLAY_CURRENT;
        refreshMapItems();
    }

    private int getDisplayRadiusInKm() {
        return (1 << currentZoom);
    }

    private String imageURL() {
        //Using Yahoo REST MAP service!
        String url = MapDisplay.YAHOO_REST_MAP_URL;
        url = url.concat(GeoCrawlerKey.YAHOO_REST_MAP_KEY);
        url = url.concat("&latitude=").concat(Double.toString(getDisplayLat()));
        url = url.concat("&longitude=").concat(Double.toString(getDisplayLon()));
        int radiusInKm = getDisplayRadiusInKm();
//        System.err.println("Radius in KM: "+Integer.toString(radiusInKm));
        double radiusInMiles = ((double)radiusInKm) * 0.621371192;
//        System.err.println("Radius in miles: "+Double.toString(radiusInMiles));
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

    protected boolean setNextMapItemHighlight(boolean forward) {
        if ((displayMode != MapDisplay.MODE_ROAMING) || (canvas == null)
            || !canvas.hasMapItems())
            return false;
        canvas.highlightNextMapItem(forward);
        MapItem item = canvas.getHighlightedMapItem();
        if (item != null)
            canvas.setDisplayText(item.getShortDescription(canvas.getDescCharCount()));
        return true;
    }

    private boolean handleRoamingModeMapCommand(int command) {
        switch(command) {
            case MapDisplay.COMMAND_EAST:
            case MapDisplay.COMMAND_SOUTH:
                this.setNextMapItemHighlight(true);
                canvas.repaint();
                return true; //Indicate that the command is handled.
            case MapDisplay.COMMAND_WEST:
            case MapDisplay.COMMAND_NORTH:
                this.setNextMapItemHighlight(false);
                canvas.repaint();
                return true;
            case MapDisplay.COMMAND_SET_LOCATION:
                //We want more information on the currently highlighted item.
                MapItem item = canvas.getHighlightedMapItem();
                if (item != null)
                    app.showInfo(item.getDescription(), GeoCrawler.STATE_MAP);
                return true;
            default:
                break;
        }
        return false; //Command not handled.
    }

    private void handleDefaultMapCommand(int command) {
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
                //The FIRE key can be used for lots of things...
                if (!isCenteredOnCurrentLocation()) {
                    //We want to update our location.
                    LocationData loc = new LocationData(getDisplayLat(), getDisplayLon(),
                                                        500); /* FIXME */
                    app.getCollector().setLocation(loc);
                    app.handleNextState(GeoCrawler.STATE_MAP);
                } else if (displayMode == MODE_SIMPLE) {
                    //We are not displaying the current location in the header.
                    setDisplayMode(MODE_DISPLAY_CURRENT);
                } else if (displayMode == MODE_DISPLAY_CURRENT) {
                    //We are clicking on the current location despite showing
                    //the current address...
                    String currentAddress = app.getCurrentLocation().getAddress();
                    if (currentAddress != null) {
                        app.showInfo("You are currently at: "+currentAddress,
                                      GeoCrawler.STATE_MAP);
                    }
                }
                break;
            case MapDisplay.COMMAND_SWITCH_MODE:
                switch (displayMode) {
                    case MapDisplay.MODE_SIMPLE:
                        if (isCenteredOnCurrentLocation())
                            //We are already centered on current location.
                            //Switch to ROAM mode.
                            setDisplayMode(MapDisplay.MODE_ROAMING);
                        else {
                            shift_lat = 0.0;
                            shift_lon = 0.0;
                            app.handleNextState(GeoCrawler.STATE_MAP);
                        }
                        break;
                    case MapDisplay.MODE_DISPLAY_CURRENT:
                        setDisplayMode(MapDisplay.MODE_SIMPLE);
                        break;
                    case MapDisplay.MODE_ROAMING:
                        setDisplayMode(MapDisplay.MODE_DISPLAY_CURRENT);
                        break;
                    default:
                        break;
                }
                break;
            default:
        }
    }

    public void mapCommand(int command) {
        if (displayMode == MapDisplay.MODE_ROAMING) {
            if (handleRoamingModeMapCommand(command))
                return;
        }

        this.handleDefaultMapCommand(command);
    }

    private double getShift() {
        int divisor = 1 << (MapDisplay.MAX_ZOOM - currentZoom);
        double shift = MapDisplay.MAX_DEGREE_SHIFT / ((double)divisor);
        return shift;
    }

    private double getDisplayLat() {
        double lat = app.getCurrentLocation().getLatitude() + shift_lat;
        if (lat > 90)
            lat = 90;
        else if (lat < -90)
            lat = -90;
        return lat;
    }

    private double getDisplayLon() {
        double lon = app.getCurrentLocation().getLongitude() + shift_lon;
        if (lon > 180)
            lon = lon - 360;
        else if (lon < -180)
            lon = lon + 360;
        return lon;
    }

    public void registerMapItemSource(MapItemSource source) {
        if (source == null)
            return;
        String name = source.getServiceName();
        String key = source.getConfigKey();
        if (key.length() > 0) {
            String display = "Automatically fetch "+name+" aa needed";
            int mode = source.getDefaultRunMode();
            boolean b = (mode == MapItemSource.REFRESH_AUTO);

            ConfigItem c = new ConfigItem(key, display, true, b, this);
            if (!app.getConfigStore().registerConfigItem(c)) {
                app.setError("Attempt to regster duplicate key: "+key+" for map item source: "+name);
                return; //Maybe duplicate. Don't register.
            }

            try {
                b = c.getBoolValue();
            } catch (Exception e) {
                app.setError("Attempt to get configured run mode for map item source: "+name+" failed.");
                return; //Something screwed up. Don't register.
            }

            source.setRefreshMode((b) ? MapItemSource.REFRESH_AUTO : MapItemSource.REFRESH_MANUAL);
        } else
            key = name; //We really don't need the key unless config has to call us back.
        mapItemSources.put(key, source);
        Command command = new Command(name, Command.SCREEN, 1);
        canvas.addCommand(command);
    }

    private void updateMapItemRefreshMode(String key, boolean value) {
        MapItemSource source = (MapItemSource)mapItemSources.get(key);
        if (source != null) {
            source.setRefreshMode((value) ? MapItemSource.REFRESH_AUTO : MapItemSource.REFRESH_MANUAL);
            refreshMapItems();
        }
    }

    private synchronized void updateMapItemsInCanvas() {
        if ((canvas == null) || (mapItemSources == null))
            return;

        Enumeration sources = mapItemSources.elements();
        int totalItems = 0;
        while (sources.hasMoreElements()) {
            MapItemSource source = (MapItemSource)sources.nextElement();
            totalItems += source.getItemCount();
        }

        if (totalItems > MAX_MAP_ITEMS)
            totalItems = MAX_MAP_ITEMS;

        if (totalItems > 0) {
            sources = mapItemSources.elements();
            int index = 0;
            Vector availableItems = new Vector();
            while (sources.hasMoreElements() && (index < totalItems)) {
                MapItemSource source = (MapItemSource)sources.nextElement();
                int count = source.getItemCount();
                for (int i = 0 ; (i < count) && (index < totalItems) ; i++) {
                    MapItem item = source.getItem(i);
                    if (item == null) //Could be race condition, or even o/w
                        continue;
                    item.x = getX(item.getLocation());
                    item.y = getY(item.getLocation());
                    availableItems.addElement(item);
                    index++;
                }
            }
            if (index == 0)
                canvas.setMapItems(null);
            else {
                MapItem[] items = new MapItem[index];
                for (int i = 0 ; i < index ; i++)
                    items[i] = (MapItem)availableItems.elementAt(i);
                canvas.setMapItems(items);
            }
        } else {
            canvas.setMapItems(null);
        }
    }
    
    //Get the X coordinate of the draw, assuming 0,0 is top-left
    protected int getX(LocationData loc) {
        LocationData curr = new LocationData(getDisplayLat(), getDisplayLon(), 0);
        double dist = curr.getXDistance(loc);
        //Get the number of pixels (+ve or -ve) from the center.
        int x = (int)(((dist * width)/ (2 * getDisplayRadiusInKm())) + 0.5);
        
        return width/2 + x;
    }

    //Get the Y coordinate of the draw, assuming 0,0 is top-left
    protected int getY(LocationData loc) {
        LocationData curr = new LocationData(getDisplayLat(), getDisplayLon(), 0);
        double dist = curr.getYDistance(loc);
        //Get the number of pixels (+ve or -ve) from the center.
        int y = (int)(((dist * height)/ (2 * getDisplayRadiusInKm())) + 0.5);

        return height/2 - y;
    }

    public synchronized void mapItemUpdaterCallback(boolean more,
                                                    MapItemUpdater thread) {
        LocationData loc = thread.getLocation();
        if (loc != app.getCurrentLocation()) {
            //Thread is obsolete. Wonder why it did not die. If this is same as
            //the current updater thread, then set current thread to null and
            //tell the updater thread to buzz off...
            thread.stop();
            if ((itemUpdaterThread != null)
                && (itemUpdaterThread.getLocation() == loc)) {
                //We don't really have a method to check for equality of the
                //locations, but this is more correct...
                this.itemUpdaterThread = null;
            }
        }

        updateMapItemsInCanvas();
        if ((canvas != null) && (app.getDisplay().getCurrent() == canvas))
            canvas.repaint(); //We have already checked that canvas is not null.

        if (!more) {
            thread.stop();
            itemUpdaterThread = null;
        }
    }

    public void notifyChanged(ConfigItem item) {
        String key = item.getKey();
        boolean b = false;
        try {
            b = item.getBoolValue();
        } catch (Exception ex) {
            return;
        }
        this.updateMapItemRefreshMode(key, b);
    }
}
