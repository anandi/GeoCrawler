
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.io.ConnectionNotFoundException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class FireEagleDisplay extends DisplayModule {
    private Form form;
    private StringItem code;
    private StringItem info;
    private TextField verifier;
    private Command doneCommand;
    private Command resetCommand;

    public FireEagleDisplay(GeoCrawler app) {
        super(app);
        
        doneCommand = new Command("Done", Command.SCREEN, 1);
        resetCommand = new Command("Reset authorization", Command.SCREEN, 1);
        info = new StringItem("", "");
        verifier = new TextField("Verifier:", null, 120, TextField.ANY);
        code = new StringItem("Request code:", "");

        form = new Form("Fire Eagle Authorization");
        form.addCommand(getExitCommand());
        form.addCommand(getHomeCommand());
        form.setCommandListener(this);
    }

    public void display(int prevState) { //prevState is ignored.
        boolean addResetCommand = true;
        boolean addDoneCommand = true;

        form.deleteAll();
        form.removeCommand(doneCommand);
        form.removeCommand(resetCommand);

        FireEagle fireEagle = app.getFireEagle();
        if (fireEagle.getState() == FireEagle.STATE_AUTHORIZED) {
            info.setText("You are currently authorized. You can reset the authorization by choosing \"Reset ...\" at any time.");
            form.append(info);
            addDoneCommand = false;
        } else if (fireEagle.getState() == FireEagle.STATE_REQUEST_TOKEN) {
            verifier.setString(""); //Always initialize.
            info.setText("Please enter your verification string and choose \"Done\" to complete authorization. You can reset the authorization and start again if you have problem");
            form.append(info);
            form.append(verifier);
        } else if (fireEagle.getState() == FireEagle.STATE_NOTOKEN) {
            String token = null;
            if (fireEagle.fetchRequestToken()) {
                token = fireEagle.getToken();
                //Note. This changes the state of the fireEagle to STATE_REQUEST_TOKEN!
            }
            if (token != null) {
                code.setText(token);
                form.append(code);
                String data = new String("Fire Eagle allows you to share your location with others. To find out more, visit http://fireeagle.yahoo.net/.");
                data = data.concat("If your WAP browser does not automatically open, visit ");
                data = data.concat(GeoCrawlerKey.FIRE_EAGLE_AUTH_URL);
                data = data.concat(" and enter the above code. Press \"Done\" after the authorization.");
                info.setText(data);
                form.append(info);
                try {
                    app.platformRequest(GeoCrawlerKey.FIRE_EAGLE_AUTH_URL);
                } catch (ConnectionNotFoundException cnfe) {
                    System.err.println("Caught an exception when trying to launch browser");
                }
            } else {
                info.setText("Failed to initiate authorization. Your network connection may be suspect. Choose \"Main menu\" to go back.");
                form.append(info);
                addDoneCommand = false;
            }
        }

        if (addDoneCommand)
            form.addCommand(doneCommand);
        if (addResetCommand)
            form.addCommand(resetCommand);

        app.getDisplay().setCurrent(form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == getExitCommand()) {
            app.handleNextState(GeoCrawler.STATE_EXIT);
        } else if (c == getHomeCommand()) {
            app.handleNextState(GeoCrawler.STATE_BEGIN);
        } else if (c == doneCommand) {
            FireEagle fireEagle = app.getFireEagle();
            if (fireEagle.getState() == FireEagle.STATE_REQUEST_TOKEN) {
                if (verifier.size() > 0) {
                    //The verifier was displayed and updated.
                    if (fireEagle.exchangeRequestToken(verifier.getString())) {
                        verifier.setString(""); //Always reset the field.
                        //fireEagle has changed state to STATE_AUTHORIZED.
                        app.handleNextState(GeoCrawler.STATE_FIREEAGLE);
                    } else {
                        info.setText("There was a problem in authorization. Please try again.");
                    }
                } else {
                    //Redo the form with the verifier input field.
                    app.handleNextState(GeoCrawler.STATE_FIREEAGLE);
                }
            }
        } else if (c == resetCommand) {
            app.getFireEagle().resetTokens();
            app.handleNextState(GeoCrawler.STATE_FIREEAGLE);
        } else {
            System.out.println("Unknown command received in Fire Eagle form.");
        }
    }
}
