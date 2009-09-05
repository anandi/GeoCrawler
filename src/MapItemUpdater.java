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

        while (running) {
            //Keep running till there are no updates left.
            if (!runOnce())
                break;
        }

        running = false;
    }

    private boolean runOnce() {
        //In the first pass, invalidate all items that need refresh. This will
        //ensure that each call to the owner callback will only have fresh data.
        Enumeration sources = mapItemSources.elements();
        boolean hasUpdates = false;
        while (sources.hasMoreElements()) {
            MapItemSource source = (MapItemSource)sources.nextElement();
            if (source.needsRefresh(currentLoc)) {
                source.invalidateItems();
                hasUpdates = true;
            }
        }
        if (!hasUpdates)
            return false;

        sources = mapItemSources.elements();
        while (sources.hasMoreElements() && running) {
            MapItemSource source = (MapItemSource)sources.nextElement();
            if (source.needsRefresh(currentLoc))
                source.runUpdate(currentLoc);
            //Once a map source completes, update it on canvas if possible.
            owner.mapItemUpdaterCallback(sources.hasMoreElements(), this);
        }

        return true;
    }

    public LocationData getLocation() {
        return currentLoc;
    }

    public void stop() {
        running = false;
    }
}
