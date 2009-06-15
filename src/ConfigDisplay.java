
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Item;
//import javax.microedition.lcdui.StringItem;
import java.util.Enumeration;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class ConfigDisplay extends DisplayModule {
    private Form form;
    private String[] keyArray;
    private Command doneCommand;

    private static String CHOICE_YES = "Yes";
    private static String CHOICE_NO = "No";

    public ConfigDisplay(GeoCrawler app) {
        super(app);

        form = new Form("GeoCrawler Config");

        Enumeration keys = GeoCrawlerKey.getConfigKeys();
        keyArray = new String[GeoCrawlerKey.getKeyCount()];
        int length = 0;
        while (keys.hasMoreElements()) {
            keyArray[length] = (String)keys.nextElement();
            length++;
        }

        //Somehow, my IDE is not happy with java.util.Arrays.sort! I have to
        //do my own sort. OK then, bubble sort is good enough!
        sort(keyArray);

        for (int i = 0 ; i < keyArray.length ; i++) {
            String type = GeoCrawlerKey.getConfigType(keyArray[i]);
            if (type.equals(GeoCrawlerKey.VALUE_TYPE_BOOLEAN)) {
                //Handle boolean with Yes / No checkbox!
                ChoiceGroup choices = new ChoiceGroup(keyArray[i], Choice.EXCLUSIVE);
                int yesIdx = choices.append(CHOICE_YES, null);
                int noIdx = choices.append(CHOICE_NO, null);
                if (app.getConfigStore().getConfigString(keyArray[i]).equals("true"))
                    choices.setSelectedIndex(yesIdx, true);
                else
                    choices.setSelectedIndex(noIdx, true);
                form.append(choices);
            } else if (type.equals(GeoCrawlerKey.VALUE_TYPE_NUMERIC)) {
                //Handle numeric with textfield!
                TextField field = new TextField(keyArray[i], null, 60, TextField.DECIMAL);
                form.append(field);
                field.setString(app.getConfigStore().getConfigString(keyArray[i]));
            }
        }

        form.addCommand(getExitCommand());
        doneCommand = new Command("Save", Command.SCREEN, 1);
        form.addCommand(doneCommand);

        form.setCommandListener(this);
    }

    private void sort(String[] arr) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 1 ; i < arr.length ; i++) {
                if (arr[i - 1].compareTo(arr[i]) > 0) {
                    String tmp = arr[i];
                    arr[i] = arr[i - 1];
                    arr[i - 1] = tmp;
                    changed = true;
                }
            }
        }
    }

    public void display(int prevState) {
        previousState = prevState;
        app.getDisplay().setCurrent(form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == getExitCommand())
            app.handleNextState(GeoCrawler.STATE_EXIT);
        else if (c == getHomeCommand())
            app.handleNextState(GeoCrawler.STATE_BEGIN);
        else if (c == doneCommand) {
            for (int i = 0 ; i < form.size() ; i++) {
                Item item = form.get(i);
                String key = item.getLabel();
                String type = GeoCrawlerKey.getConfigType(key);
                if (type.equals(GeoCrawlerKey.VALUE_TYPE_BOOLEAN)) {
                    //Handle yes/no choice.
                    ChoiceGroup choices = (ChoiceGroup)item;
                    String choice = choices.getString(choices.getSelectedIndex());
                    String currentVal = app.getConfigStore().getConfigString(key);
                    if (choice.equals(ConfigDisplay.CHOICE_YES) && currentVal.equals("false"))
                        app.handleConfigChange(key, "true");
                    if (choice.equals(ConfigDisplay.CHOICE_NO) && currentVal.equals("true"))
                        app.handleConfigChange(key, "false");
                } else if (type.equals(GeoCrawlerKey.VALUE_TYPE_NUMERIC)) {
                    //Handle numbers.
                    TextField field = (TextField)item;
                    String val = field.getString();
                    String currentVal = app.getConfigStore().getConfigString(key);
                    if (!val.equals(currentVal)) {
                        app.handleConfigChange(key, val);
                    }
                }
            }
            app.handleNextState(previousState); //Go back to previous display.
        } else {
            System.out.println("Unknown command received in config form.");
        }
    }
}
