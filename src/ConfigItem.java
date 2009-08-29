/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */

//Yeah! Don't lecture me on how horribly this class design sucks! I know. I am
//just not interested in cleaning it up right now!
public class ConfigItem {
    public static final int TYPE_BOOLEAN = 0;
    public static final int TYPE_INTEGER = 1;
    public static final int TYPE_FLOAT   = 2;
    public static final int TYPE_STRING  = 3;
    //Add more types as needed.

    private String key; //Internal key for storing the config item.
    private String display; //String to be displayed.
    private boolean visibility; //Whether end users should see this config item.
    private int type; //Refer to the types above.
    private Object value;
    private ConfigListener owner;

    public boolean saved; //Whether the current instance value is persisted.

    public String getKey() {
        return key;
    }

    //We shall need to i18n it some day!
    public String getDisplayString() {
        return display;
    }

    public boolean isVisible() {
        return visibility;
    }

    public int getType() {
        return type;
    }

    public ConfigItem(String key, String display, boolean visibility,
                      boolean val, ConfigListener owner) {
        this.key = key;
        this.display = display;
        this.visibility = visibility;
        this.type = ConfigItem.TYPE_BOOLEAN;
        this.value = new Boolean(val);
        this.owner = owner;
        saved = false;
    }

    public ConfigItem(String key, String display, boolean visibility,
                      int val, ConfigListener owner) {
        this.key = key;
        this.display = display;
        this.visibility = visibility;
        this.type = ConfigItem.TYPE_INTEGER;
        this.value = new Integer(val);
    }

    public ConfigItem(String key, String display, boolean visibility,
                      double val, ConfigListener owner) {
        this.key = key;
        this.display = display;
        this.visibility = visibility;
        this.type = ConfigItem.TYPE_FLOAT;
        this.value = new Double(val);
    }

    public ConfigItem(String key, String display, boolean visibility,
                      String val, ConfigListener owner) {
        this.key = key;
        this.display = display;
        this.visibility = visibility;
        this.type = ConfigItem.TYPE_STRING;
        this.value = val;
    }

    public boolean getBoolValue() throws Exception {
        if (type != ConfigItem.TYPE_BOOLEAN)
            throw new Exception("ConfigItem: getBoolValue called for non-boolean");
        return ((Boolean)value).booleanValue();
    }

    public void setValue(boolean val) throws Exception {
        if (type != ConfigItem.TYPE_BOOLEAN)
            throw new Exception("ConfigItem: setValue called for non-boolean");
        boolean curr_val = ((Boolean)value).booleanValue();
        if (curr_val != val) {
            saved = false;
            value = new Boolean(val);
            if (owner != null)
                owner.notifyChanged(this);
        }
    }

    public int getIntValue() throws Exception {
        if (type != ConfigItem.TYPE_INTEGER)
            throw new Exception("ConfigItem: getIntValue called for non-integer");
        return ((Integer)value).intValue();
    }

    public void setValue(int val) throws Exception {
        if (type != ConfigItem.TYPE_INTEGER)
            throw new Exception("ConfigItem: setValue called for non-integer");
        int curr_val = ((Integer)value).intValue();
        if (curr_val != val) {
            saved = false;
            value = new Integer(val);
            if (owner != null)
                owner.notifyChanged(this);
        }
    }

    public double getDoubleValue() throws Exception {
        if (type != ConfigItem.TYPE_FLOAT)
            throw new Exception("ConfigItem: getDoubleValue called for non-float");
        return ((Double)value).doubleValue();
    }

    public void setValue(double val) throws Exception {
        if (type != ConfigItem.TYPE_FLOAT)
            throw new Exception("ConfigItem: setValue called for non-float");
        double curr_val = ((Double)value).doubleValue();
        if (curr_val != val) {
            saved = false;
            value = new Double(val);
            if (owner != null)
                owner.notifyChanged(this);
        }
    }

    public void setValueFromString(String val) throws Exception {
        if (this.type == ConfigItem.TYPE_BOOLEAN) {
            if (val.equals("true"))
                setValue(true);
            else if (val.equals("false"))
                setValue(false);
            else
                throw new Exception("ConfigItem: setValueFromString called for non-boolean");
        } else if (this.type == ConfigItem.TYPE_INTEGER) {
            int i;
            try {
                i = Integer.parseInt(val);
            } catch (NumberFormatException nfe) {
                throw new Exception("ConfigItem: setValueFromString called for non-integer");
            }
            try {
                setValue(i);
            } catch (Exception e) {} //No exception will be thrown!
        } else if (this.type == ConfigItem.TYPE_FLOAT) {
            double d;
            try {
                d = Double.parseDouble(val);
            } catch (NumberFormatException nfe) {
                throw new Exception("ConfigItem: setValueFromString called for non-float");
            }
            try {
                setValue(d);
            } catch (Exception e) {} //No exception will be thrown!
        } else if (this.type == ConfigItem.TYPE_STRING) {
            value = val;
            if (owner != null)
                owner.notifyChanged(this);
        }
    }

    public String getValueAsString() {
        if (type == ConfigItem.TYPE_BOOLEAN) {
            //The reason we do not rely on the native toString is because,
            //the reverse method Boolean.parseBoolean does not seem to be
            //available for deserialization. So, let us make sure that we
            //give out what we accept back.
            return (((Boolean)value).booleanValue()) ? "true" : "false";
        }
        return value.toString();
    }
}
