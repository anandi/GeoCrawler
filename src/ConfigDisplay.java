
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Item;
//import javax.microedition.lcdui.StringItem;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

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
    private Command doneCommand;
    private Hashtable displayToConfigMap;

    private static String CHOICE_YES = "Yes";
    private static String CHOICE_NO = "No";

    public ConfigDisplay(GeoCrawler app) {
        super(app);

        form = new Form("GeoCrawler Config");
        form.addCommand(getBackCommand());
        doneCommand = new Command("Save", Command.SCREEN, 1);
        form.addCommand(doneCommand);

        form.setCommandListener(this);
        displayToConfigMap = new Hashtable();
    }

    private String[] getConfigKeys() {
        Vector keyVector = app.getConfigStore().getVisibleKeys();
        String[] keyArr = new String[keyVector.size()];
        Enumeration keys = keyVector.elements();
        int index = 0;
        while (keys.hasMoreElements()) {
            keyArr[index] = (String)(keys.nextElement());
            index++;
        }
        return keyArr;
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
        displayToConfigMap.clear();
        form.deleteAll(); //Refresh.

        String[] keyArray = this.getConfigKeys();
        //Somehow, my IDE is not happy with java.util.Arrays.sort! I have to
        //do my own sort. OK then, bubble sort is good enough!
        sort(keyArray);
        for (int i = 0 ; i < keyArray.length ; i++) {
            ConfigItem configItem = app.getConfigStore().getVisibleConfigItem(keyArray[i]);
            int type = configItem.getType();
            int id = -1;
            if (type == ConfigItem.TYPE_BOOLEAN) {
                //Handle boolean with Yes / No checkbox!
                ChoiceGroup choices = new ChoiceGroup(configItem.getDisplayString(), Choice.EXCLUSIVE);
                int yesIdx = choices.append(CHOICE_YES, null);
                int noIdx = choices.append(CHOICE_NO, null);
                try {
                    if (configItem.getBoolValue())
                        choices.setSelectedIndex(yesIdx, true);
                    else
                        choices.setSelectedIndex(noIdx, true);
                } catch (Exception e) {} //No exception will be thrown!
                id = form.append(choices);
            } else if ((type == ConfigItem.TYPE_INTEGER) || (type == ConfigItem.TYPE_FLOAT)) {
                //Handle numeric with textfield!
                TextField field = new TextField(configItem.getDisplayString(), null, 60, TextField.DECIMAL);
                id = form.append(field);
                field.setString(configItem.getValueAsString());
            }
            if (id != -1)
                displayToConfigMap.put(new Integer(id), configItem);
            //We should be throwing exception o/w!
        }

        app.getDisplay().setCurrent(form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == getBackCommand())
            handleBackCommand();
        else if (c == getHomeCommand())
            app.handleNextState(GeoCrawler.STATE_BEGIN);
        else if (c == doneCommand) {
            for (int i = 0 ; i < form.size() ; i++) {
                Item item = form.get(i);
                ConfigItem configItem = (ConfigItem)(displayToConfigMap.get(new Integer(i)));
                if (configItem == null)
                    continue;
                String key = configItem.getKey();
                int type = configItem.getType();
                if (type == ConfigItem.TYPE_BOOLEAN) {
                    //Handle yes/no choice.
                    ChoiceGroup choices = (ChoiceGroup)item;
                    String choice = choices.getString(choices.getSelectedIndex());
                    boolean currValue = false;
                    try {
                        currValue = configItem.getBoolValue();
                        if (choice.equals(ConfigDisplay.CHOICE_YES) && !currValue) {
                            configItem.setValue(true);
                            configItem.saved = false;
                        }
                        if (choice.equals(ConfigDisplay.CHOICE_NO) && currValue) {
                            configItem.setValue(false);
                            configItem.saved = false;
                        }
                    } catch (Exception e) {} //Won't be thrown.
                } else if (type == ConfigItem.TYPE_INTEGER) {
                    //Handle numbers.
                    TextField field = (TextField)item;
                    String val = field.getString();
                    int currVal = 0;
                    int newVal = currVal;
                    try {
                        currVal = configItem.getIntValue();
                        newVal = Integer.parseInt(val);
                        if (currVal != newVal) {
                            configItem.setValue(newVal);
                            configItem.saved = false;
                        }
                    } catch (Exception e) {} //Assume not changed.
                } else if (type == ConfigItem.TYPE_FLOAT) {
                    //Handle numbers.
                    TextField field = (TextField)item;
                    String val = field.getString();
                    double currVal = 0;
                    double newVal = currVal;
                    try {
                        currVal = configItem.getDoubleValue();
                        newVal = Double.parseDouble(val);
                        if (currVal != newVal) {
                            configItem.setValue(newVal);
                            configItem.saved = false;
                        }
                    } catch (Exception e) {} //Assume not changed.
                }
            }
            app.getConfigStore().updateConfigItems();
            app.handleNextState(previousState); //Go back to previous display.
        } else {
            System.out.println("Unknown command received in config form.");
        }
    }
}
