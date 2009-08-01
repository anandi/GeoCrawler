/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Command;

/**
 *
 * @author anandi
 */
public abstract class DisplayModule extends Object implements CommandListener {

    protected GeoCrawler app;
    private static Command exitCommand;
    private static Command homeCommand;
    private static Command backCommand;

    protected int previousState; //Helps to preserve context.

    public DisplayModule(GeoCrawler app) {
        this.app = app;
        if (exitCommand == null)
            exitCommand = new Command("Exit", Command.EXIT, 1);
        if (homeCommand == null)
            homeCommand = new Command("Main menu", Command.SCREEN, 1);
        if (backCommand == null)
            backCommand = new Command("Back", Command.BACK, 1);
        previousState = GeoCrawler.STATE_BEGIN; //Default previous state.
    }

    protected Command getExitCommand() {
        return exitCommand;
    }

    protected Command getHomeCommand() {
        return homeCommand;
    }

    protected Command getBackCommand() {
        return backCommand;
    }

    protected void handleBackCommand() {
        app.handleNextState(previousState);
    }

    public abstract void display(int prevState);
}
