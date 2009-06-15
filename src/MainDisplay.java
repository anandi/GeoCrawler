
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Item;

/**
 *
 * @author anandi
 */
public class MainDisplay extends DisplayModule {
    private Form form;
    private StringItem string;
    private Command authCommand; //Go to Fire Eagle.
    private Command nextCommand; //Go to main app.
    private Command configCommand; //Go to configuration screen.


    public MainDisplay(GeoCrawler app) {
        super(app);

        string = new StringItem("Welcome to GeoCrawler", "");
        form = new Form(null, new Item[] {string});
        authCommand = new Command("Fire Eagle...", Command.SCREEN, 1);
        nextCommand = new Command("My Location...", Command.SCREEN, 1);
        configCommand = new Command("Config", Command.SCREEN, 1);
        form.addCommand(this.getExitCommand());
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
            string.setText("Welcome back.");
        } else {
            app.handleNextState(GeoCrawler.STATE_MAP);
            return;
        }

        if (!onlyExitCommand) {
            form.addCommand(authCommand);
            form.addCommand(nextCommand);
            form.addCommand(configCommand);
        } else {
            form.removeCommand(authCommand);
            form.removeCommand(nextCommand);
            form.removeCommand(configCommand);
        }

        app.getDisplay().setCurrent(form);
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
        } else {
            System.out.println("Unknown command received in main form.");
        }
    }

}
