
import java.util.Date;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import java.io.IOException;
import java.util.Enumeration;
import org.json.me.JSONObject;
import org.json.me.JSONException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class MainDisplay extends DisplayModule {
    private static final String UPDATE_URL = "http://github.com/anandi/GeoCrawler/raw/master/version.json";

    private Form form;
    private StringItem string;
    private Command authCommand; //Go to Fire Eagle.
    private Command nextCommand; //Go to main app.
    private Command configCommand; //Go to configuration screen.
    private Command errorCommand; //Go to the last error screen.
    private Command manualCommand; //Go to the manual location screen.
    private Command detailsCommand; //Show the update details.
    private Command checkUpdateCommand; //Check for updates.

    public MainDisplay(GeoCrawler app) {
        super(app);

        string = new StringItem("Welcome to GeoCrawler", "");
        form = new Form(null, new Item[] {string});
        nextCommand = new Command("My Location...", Command.SCREEN, 1);
        detailsCommand = new Command("Show details", Command.SCREEN, 2);
        manualCommand = new Command("Manual location...", Command.SCREEN, 3);
        configCommand = new Command("Config", Command.SCREEN, 4);
        errorCommand = new Command("Last error...", Command.SCREEN, 5);
        authCommand = new Command("Fire Eagle...", Command.SCREEN, 6);
        checkUpdateCommand = new Command("Check Update", Command.SCREEN, 7);
        form.addCommand(this.getExitCommand());
        form.addCommand(errorCommand); //Always keep this handy.
        form.addCommand(manualCommand); //The manual display needs to be accessible from the main.
        form.addCommand(checkUpdateCommand);
        form.setCommandListener(this);
    }

    public void display(int prevState) { //prevState is ignored!
        boolean onlyExitCommand = false;

        if (app.getConfigStore() == null) {
            string.setText("Cannot access storage. Choose \"Exit\" to terminate");
            onlyExitCommand = true;
        } else if (app.getFireEagle().getState() == FireEagle.STATE_NOTOKEN) {
            String intro = new String();
            intro = intro.concat("\n\nThis application works best with Fire Eagle.");
            intro = intro.concat(" To authorize Fire Eagle, choose \"Fire Eagle...\" from menu.");
            intro = intro.concat("\n\n");
            intro = intro.concat("If you use cell tower based location detection, ");
            intro = intro.concat("this application may try to use opencellid (www.opencellid.org). ");
            intro = intro.concat("It also tries to update the opencellid database with anonymous information");
            intro = intro.concat(" in order to enhance the database for everyone.");
            intro = intro.concat("\n\nIf you are not comfortable with this, please turn off cell tower based location detection");
            string.setText(intro);
        } else if (app.getFireEagle().getState() == FireEagle.STATE_REQUEST_TOKEN) {
            string.setText("You have not finished Fire Eagle authorization. To proceed, choose \"Fire Eagle...\" from menu");
        } else if (app.getFireEagle().getState() == FireEagle.STATE_AUTHORIZED) {
            String text = "You are currently successfully authorized with Fire Eagle";
            LocationData loc = app.getCurrentLocation();
            if (loc == null) {
                if (app.getDetectionState() == GeoCrawler.DETECTION_IN_PROGRESS) {
                    int timeout = app.getCollector().getGPSTimeoutInSeconds();
                    text = text.concat("\n Trying for "+Integer.toString(timeout)+" seconds to get your location");
                } else {
                    text = text.concat("\n Currently unable to detect your location");
                }
            } else {
                text = text.concat("\n You were last detected at: "+loc.getAddress());
            }
            string.setText(text);
        } else {
            app.handleNextState(GeoCrawler.STATE_MAP);
            return;
        }

        if (!onlyExitCommand) {
            form.addCommand(authCommand);
            form.addCommand(nextCommand);
            form.addCommand(configCommand);
            form.addCommand(detailsCommand);
        } else {
            form.removeCommand(authCommand);
            form.removeCommand(nextCommand);
            form.removeCommand(configCommand);
            form.removeCommand(detailsCommand);
        }

        app.getDisplay().setCurrent(form);
    }

    private void checkUpdate() {
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(UPDATE_URL);
        } catch (IOException iox) {
            app.showError("Failed to retrieve the update information", GeoCrawler.STATE_BEGIN);
            return;
        }

        try {
            JSONObject parsedResponse = new JSONObject(response);
            if (parsedResponse.has("Version")) {
                String version = parsedResponse.getString("Version");
                if (version.equals(GeoCrawler.VERSION))
                    app.showInfo("You are currently using the latest version.", GeoCrawler.STATE_BEGIN);
                else {
                    Enumeration e = parsedResponse.keys();
                    String message = "A new version of the application is available";
                    while (e.hasMoreElements()) {
                        String k = (String)e.nextElement();
                        String v = parsedResponse.getString(k);
                        message = message.concat("\n"+k+": "+v);
                    }
                    app.showInfo(message, GeoCrawler.STATE_BEGIN);
                }
            }
        } catch (JSONException jsox) {
            app.showError("Failed to retrieve the update information", GeoCrawler.STATE_BEGIN);
            return;
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == getExitCommand())
            app.handleNextState(GeoCrawler.STATE_EXIT);
        else if (c == authCommand) {
            app.handleNextState(GeoCrawler.STATE_FIREEAGLE);
        } else if (c == nextCommand) {
            app.handleNextState(GeoCrawler.STATE_MAP);
        } else if (c == configCommand) {
            app.handleNextState(GeoCrawler.STATE_CONFIG);
        } else if (c == errorCommand) {
            app.handleNextState(GeoCrawler.STATE_ERROR);
        } else if (c == manualCommand) {
            app.handleNextState(GeoCrawler.STATE_MANUAL);
        } else if (c == detailsCommand) {
            String text = "Location Details";
            boolean autoRun = app.getCollector().getAutoRunMode();
            if (!autoRun)
                text = text.concat("\n You have switched off automatic location detection.");

            LocationData loc = app.getCurrentLocation();
            if (loc != null) {
                Date lastUpdate = loc.getTimeStamp();
                text = text.concat("\n You were last located at: "+lastUpdate.toString());
                if (loc.source != null)
                    text = text.concat("\n Your displayed location has been obtained from: "+loc.source);
            }

            Date lastRun = app.getCollector().getLastRun();
            if (lastRun != null)
                text = text.concat("\n Last attempt to detect your location was at: "+lastRun.toString());
            else if (!autoRun)
                text = text.concat("\n No attempt has been made to detect your location.");
            else
                text = text.concat("\n The auto updater is currently trying to detect your location");
            app.showInfo(text, GeoCrawler.STATE_BEGIN);
        } else if (c == checkUpdateCommand) {
            checkUpdate();
        } else {
            System.out.println("Unknown command received in main form.");
        }
    }

}
