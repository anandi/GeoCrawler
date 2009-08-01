/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Hashtable;
import java.util.Enumeration;

/**
 *
 * @author anandi
 */
public class MapItemUpdater extends Thread {
    private Hashtable mapItemSources;
    MapDisplay owner; //Needed to callback.
    LocationData currentLoc;
    private volatile boolean running;

    public MapItemUpdater(Hashtable sources, MapDisplay owner, LocationData loc) {
        mapItemSources = sources;
        this.owner = owner;
        currentLoc = loc;
        running = false;
    }

    public void run() {
        running = true;
        Enumeration sources = mapItemSources.elements();
        while (sources.hasMoreElements()) {
            MapItemSource source = (MapItemSource)sources.nextElement();
            source.invalidateItems();
        }

        sources = mapItemSources.elements();
        while (sources.hasMoreElements() && running) {
            MapItemSource source = (MapItemSource)sources.nextElement();
            source.runUpdate(currentLoc);
            //Once a map source completes, update it on canvas if possible.
            owner.mapItemUpdaterCallback(sources.hasMoreElements(), this);
        }
    }

    public LocationData getLocation() {
        return currentLoc;
    }

    public void stop() {
        running = false;
    }
}
