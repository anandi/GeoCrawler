
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
    public static final int MSG_ERROR = 0;
    public static final int MSG_INFO = 1;

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

    public void setMessage(String msg, int type) {
        if ((msg != null) && ((type == MSG_ERROR) || (type == MSG_INFO))) {
            message.setText(msg);

            if (type == MSG_INFO)
                form.setTitle("Info");
            else //Can only be info
                form.setTitle("Error");
        }
    }

    public void display(int prevState) {
        previousState = prevState;
        if (message.getText().length() == 0) {
            message.setText(NO_ERROR); //This suffers from race. Please put in
                                       //a lock.
            form.setTitle("Error");
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
