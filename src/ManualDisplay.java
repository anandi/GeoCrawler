
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.StringItem;
import java.util.Hashtable;
import java.util.Vector;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class ManualDisplay extends DisplayModule {
    public static final int STATE_INIT = 0; //No memory or continuation state.
    public static final int STATE_DISAMBIGUATE = 1; //Disambiguation needed.
    public static final int STATE_CONFIRM = 2; //Confirmation needed.

    private static final int MAX_DISAMBIGUATION_CHOICES = 10;

    private TextField address;
    private int addressIndex;
    private ChoiceGroup address_choices;
    private StringItem address_notice;
    int additionalIndex;
    private String oldAddress;

    private TextField latitude;
    private TextField longitude;
    private String latVal;
    private String lonVal;

    private TextField cellid;
    private TextField lac;
    private TextField mnc;
    private TextField mcc;
    private String cellidVal;
    private String lacVal;
    private String mccVal;
    private String mncVal;

    private Form form;
    private Command doneCommand;

    private int state;

    public ManualDisplay(GeoCrawler app) {
        super(app);
        state = ManualDisplay.STATE_INIT;

        address = new TextField("Address: ", null, 100, TextField.ANY);
        address_choices = null;
        address_notice = null;
        oldAddress = null;
        additionalIndex = -1;
        int input_flags = (GeoCrawlerKey.GEO_CRAWLER_DEVEL_MODE) ? TextField.DECIMAL : TextField.UNEDITABLE;

        latitude = new TextField("Latitude:", null, 60, input_flags);
        longitude = new TextField("Longitude:", null, 60, input_flags);

        cellid = new TextField("Cell-ID:", null, 60, input_flags);
        lac = new TextField("LAC:", null, 60, input_flags);
        mcc = new TextField("MCC:", null, 60, input_flags);
        mnc = new TextField("MNC:", null, 60, input_flags);

        form = new Form("Manual Update");
        addressIndex = form.append(address);
        form.append(latitude);
        form.append(longitude);
        form.append(cellid);
        form.append(lac);
        form.append(mcc);
        form.append(mnc);

        doneCommand = new Command("Update", Command.SCREEN, 1);
        form.addCommand(getBackCommand());
        form.addCommand(doneCommand);

        form.setCommandListener(this);
    }

    //This is landing up as something I did not initially intend.
    private void changeState(int newState) {
        if (newState == state)
            return;

        switch (newState) {
            case ManualDisplay.STATE_INIT:
                this.address_choices = null;
                this.address_notice = null;
                if (additionalIndex != -1)
                    form.delete(additionalIndex);
                additionalIndex = -1;
                break;
            case ManualDisplay.STATE_DISAMBIGUATE:
            case ManualDisplay.STATE_CONFIRM:
            default:
                break;
        }

        state = newState;
    }

    public void display(int prevState) {
        //Set the GPS location fields.
        if ((prevState != GeoCrawler.STATE_MANUAL)
            && (prevState != GeoCrawler.STATE_ERROR))
            this.previousState = prevState;

        //Set the address if available...
        if (state == ManualDisplay.STATE_INIT) {
            oldAddress = app.getCurrentLocation().getAddress();
            if (oldAddress == null)
                oldAddress = "Undefined";
            address.setString(oldAddress);
        }

        LocationData loc = app.getCollector().getLocation();
        if (loc != null) {
            latVal = Double.toString(loc.getLatitude());
            lonVal = Double.toString(loc.getLongitude());
        } else {
            latVal = "";
            lonVal = "";
        }
        latitude.setString(latVal);
        longitude.setString(lonVal);

        //Cell ID is enabled. Set the cell ID fields if we can get them.
        if (app.getCollector().CellEnabled()) {
            LocationBeacon cell = LocationBeacon.instanceOf("CellIDBeacon");
            if (cell.update()) {
                cellidVal = (String)cell.getProperty("cellid");
                lacVal = (String)cell.getProperty("lac");
                mccVal = (String)cell.getProperty("mcc");
                mncVal = (String)cell.getProperty("mnc");
            } else {
                cellidVal = "";
                lacVal = "";
                mccVal = "";
                mncVal = "";
            }
            cellid.setString(cellidVal);
            lac.setString(lacVal);
            mcc.setString(mccVal);
            mnc.setString(mncVal);
        }
        app.getDisplay().setCurrent(form);
    }

    //A small note on this aggressively stupid behavior...
    //A careful browsing of the code will reveal that all location updates sent
    //to the app will be updated to Fire Eagle by default. However, we understand
    //that all of these updates are going to be beacon based (GPS, Cell ID, etc)
    //However, this is one instance where it is a string based location, one
    //that needs geocoding. In this case, it is better that we give Fire Eagle
    //the string and let it geocode. We do use a (possibly) different geocoder
    //to convert the string to a lat-lon, but that may not match the Fire Eagle
    //geo-coding. This is why we force a Fire Eagle update with a string, before
    //it is lost.
    private boolean updateFireEagle(String address) {
        //We do not have to move to the STATE_CONFIRM, since the location is selected from list.
        Hashtable params = new Hashtable(1);
        params.put("q", address);
        if (app.getFireEagle().updateLocation(params)) {
            //OK. We succeeded in updating Fire Eagle. Now to get our location from it...
            //Since Fire Eagle is probably going to take some time to update the location,
            //best is not to do anything. The background updater, if it is on, will pull
            //the data down.
            changeState(ManualDisplay.STATE_INIT);
            //Instead of trying to get the location from Fire Eagle, try to get
            //it from the geocoding service if possible.
            params.clear();
            params.put("location", address);
            Vector locations = GeoCoder.addressToLocation(params);
            if (locations != null) {
                //Resolved to an accurate location... Always take the first one
                LocationData location = (LocationData)locations.elementAt(0);
                location.setAddress(address);
                location.updatedFireEagle = true; //Do not update again.
                location.source = "Manual";
                app.getCollector().setLocation(location);
            } else
                app.setError("Temporary failure to update display due to geocoding problem.");
            app.handleNextState(previousState);
        } else {
            changeState(ManualDisplay.STATE_INIT);
            app.showError("Failed to update Fire Eagle with location.", GeoCrawler.STATE_MANUAL);
            return false;
        }
        return true;
    }

    public void handleAddressChanges() {
        if (state == ManualDisplay.STATE_INIT) {
            //User is asking for it!
            String newAddr = address.getString();
            Hashtable params = new Hashtable(1);
            params.put("q", newAddr);
            FireEagle fe = app.getFireEagle();
            if (fe.getState() != FireEagle.STATE_AUTHORIZED) {
                address.setString(oldAddress);
                app.showError("You have not authorized Fire Eagle to do address lookup", GeoCrawler.STATE_MANUAL);
                return;
            }
            String[] locations = fe.lookupLocation(params);
            if (locations == null) {
                address.setString(oldAddress);
                app.showError("Fire Eagle could not resolve this address", GeoCrawler.STATE_MANUAL);
                return;
            }
            if (locations.length == 0) {
                //Address did not resolve.
                app.showError("Fire Eagle did not understand this address.", GeoCrawler.STATE_MANUAL);
            } else if (locations.length == 1) {
                //Address is unambiguous. Move to the confirm screen...
                address_notice = new StringItem("", "Press \"Done\" to update your location to: "+locations[0]);
                form.insert(addressIndex, address_notice);
                additionalIndex = addressIndex;
                state = ManualDisplay.STATE_CONFIRM;
            } else {
                //Multiple locations found. Need to disambiguate.
                //Hah! Try 'San Francisco... got 230 possible matches. Have to
                //restrict!
                int choices = (locations.length > ManualDisplay.MAX_DISAMBIGUATION_CHOICES) ? ManualDisplay.MAX_DISAMBIGUATION_CHOICES : locations.length;
                address_choices = new ChoiceGroup("Please select one:", Choice.EXCLUSIVE);
                for (int i = 0 ; i < choices ; i++)
                    address_choices.append(locations[i], null);
                form.insert(addressIndex, address_choices);
                additionalIndex = addressIndex;
                state = ManualDisplay.STATE_DISAMBIGUATE;
            }
            return;
        } else if (state == ManualDisplay.STATE_DISAMBIGUATE) {
            //Get the choice...
            int idx = address_choices.getSelectedIndex();
            if (idx == -1) {
                app.showError("You must select from the list or start from beginning", GeoCrawler.STATE_MANUAL);
                return;
            } else
                updateFireEagle(address_choices.getString(idx));
        } else if (state == ManualDisplay.STATE_CONFIRM) {
            //Get the choice...
            updateFireEagle(address.getString());
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == getBackCommand()) {
            changeState(ManualDisplay.STATE_INIT);
            this.handleBackCommand();
        } else if (c == this.doneCommand) {
            //First preference to String addresses...
            if (!oldAddress.equals(address.getString()) || (state != ManualDisplay.STATE_INIT)) {
                //String based updates are complicated. Handle them separately.
                handleAddressChanges();
                return;
            }
            changeState(ManualDisplay.STATE_INIT); //Make sure to reset!
            //Next preference to lat-lon changes.
            String latStr = latitude.getString();
            String lonStr = longitude.getString();
            if (!(latStr.equals(latVal) && lonStr.equals(lonVal))
                && (latStr.length() > 0) && (lonStr.length() > 0)) {
                LocationData loc
                    = new LocationData(Double.parseDouble(latitude.getString()),
                                      Double.parseDouble(longitude.getString()),
                                      500.00);
                loc.source = "Manual";
                app.getCollector().setLocation(loc);
            } else if (!(cellidVal.equals(cellid.getString())
                         && lacVal.equals(lac.getString())
                         && mccVal.equals(mcc.getString())
                         && mncVal.equals(mnc.getString()))) {
                LocationBeacon cell = LocationBeacon.instanceOf("CellIDBeacon");
                cell.setProperty("cellid", cellid.getString());
                cell.setProperty("lac", lac.getString());
                cell.setProperty("mcc", mcc.getString());
                cell.setProperty("mnc", mnc.getString());
                if (cell.resolve()) {
                    double error = ((Double)cell.getProperty("errorInMeter")).doubleValue();
                    LocationData loc = new LocationData(cell.getLatitude(),
                                                        cell.getLongitude(),
                                                        error);
                    loc.source = "Manual";
                    app.getCollector().setLocation(loc);
                }
            }
            app.handleNextState(previousState); //Go back to wherever we came from.
        } else
            System.out.println("Unknown command received in manual form.");
    }
}
