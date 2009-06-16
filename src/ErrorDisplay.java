
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class ErrorDisplay extends DisplayModule {
    private Form form;
    private StringItem message;
    private Command backCommand;
    private static String NO_ERROR = "There are no application errors to display";

    public ErrorDisplay(GeoCrawler app) {
        super(app);
        form = new Form("Application Error");
        message = new StringItem("", "");
        form.append(message);
        backCommand = new Command("OK", Command.SCREEN, 1);
        form.addCommand(backCommand);
        form.addCommand(this.getExitCommand());
        form.setCommandListener(this);
    }

    public void setError(String error) {
        if (error != null)
            message.setText(error);
    }

    public void display(int prevState) {
        previousState = prevState;
        if (message.getText().length() == 0) {
            message.setText(NO_ERROR); //This suffers from race. Please put in
                                       //a lock.
        }
        app.getDisplay().setCurrent(form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.getExitCommand())
            app.handleNextState(GeoCrawler.STATE_EXIT);
        else if (c == this.backCommand) {
            if (message.getText().equals(NO_ERROR)) {
                message.setText("");
            }
            app.handleNextState(previousState);
        }
    }
}
