/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Date;

/**
 *
 * @author anandi
 */
public abstract class MapItemSource {
    protected MapItem[] items;
    protected Date lastRun;

    MapItemSource() {
        items = null;
        lastRun = null;
    }

    //Override this method to allow for periodic refresh, even if the current
    //location has not changed.
    public boolean needsRefresh() {
        return (items == null) ? true : false;
    }

    //Execute the actual update.
    public abstract boolean runUpdate(LocationData loc);

    public int getItemCount() {
        if (items == null)
            return 0;
        return items.length;
    }
    
    public MapItem getItem(int index) {
        if (items == null)
            return null;
        if (index >= items.length)
            return null;
        return items[index];
    }

    public void invalidateItems() {
        //This will be called when the current location changes and the items
        //become obsolete.
        items = null;
    }

    //We shall add more methods here as we proceed.
}
