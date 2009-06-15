
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class ManualDisplay extends DisplayModule {
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

    public ManualDisplay(GeoCrawler app) {
        super(app);
        latitude = new TextField("Latitude:", null, 60, TextField.DECIMAL);
        longitude = new TextField("Longitude:", null, 60, TextField.DECIMAL);
        cellid = new TextField("Cell-ID:", null, 60, TextField.DECIMAL);
        lac = new TextField("LAC:", null, 60, TextField.DECIMAL);
        mcc = new TextField("MCC:", null, 60, TextField.DECIMAL);
        mnc = new TextField("MNC:", null, 60, TextField.DECIMAL);

        doneCommand = new Command("Update", Command.SCREEN, 1);
        form = new Form("Developer page");
        form.append(latitude);
        form.append(longitude);
        form.append(cellid);
        form.append(lac);
        form.append(mcc);
        form.append(mnc);
        form.addCommand(this.getExitCommand());
        form.addCommand(doneCommand);
        form.addCommand(this.getHomeCommand());
        form.setCommandListener(this);
    }

    public void display(int prevState) { //prevState is ignored.
        //Set the GPS location fields.
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
            CellIDBeacon cell = new CellIDBeacon();
            if (cell.update()) {
                cellidVal = cell.getCellIdString();
                lacVal = cell.getLACString();
                mccVal = cell.getMCCString();
                mncVal = cell.getMNCString();
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

    public void commandAction(Command c, Displayable d) {
        if (c == getExitCommand())
            app.handleNextState(GeoCrawler.STATE_EXIT);
        else if (c == getHomeCommand())
            app.handleNextState(GeoCrawler.STATE_BEGIN);
        else if (c == this.doneCommand) {
            //First preference to lat-lon changes.
            if (!(latitude.getString().equals(latVal) && longitude.getString().equals(lonVal))) {
                LocationData loc
                    = new LocationData(Double.parseDouble(latitude.getString()),
                                      Double.parseDouble(longitude.getString()),
                                      500.00);
                app.getCollector().setLocation(loc);
            } else if (!(cellidVal.equals(cellid.getString())
                         && lacVal.equals(lac.getString())
                         && mccVal.equals(mcc.getString())
                         && mncVal.equals(mnc.getString()))) {
                CellIDBeacon cell = new CellIDBeacon();
                cell.setCellIdString(cellid.getString());
                cell.setLACString(lac.getString());
                cell.setMCCString(mcc.getString());
                cell.setMNCString(mnc.getString());
                if (cell.resolve()) {
                    LocationData loc = new LocationData(cell.getLatitude(),
                                                        cell.getLongitude(),
                                                        cell.getErrorInMeter());
                    app.getCollector().setLocation(loc);
                }
            }
            app.handleNextState(GeoCrawler.STATE_MAP); //Go back to map!
        } else
            System.out.println("Unknown command received in devel form.");
    }
}
