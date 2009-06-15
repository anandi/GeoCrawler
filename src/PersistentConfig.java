/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import java.util.Hashtable;

/**
 *
 * @author anandi
 */
public class PersistentConfig {
    RecordStore rs;
    Hashtable recordIdMap;
    Hashtable valueMap;

    public PersistentConfig(String dbName) throws RecordStoreException {
        rs = RecordStore.openRecordStore(dbName, true);
        valueMap = new Hashtable();
        recordIdMap = new Hashtable();

        RecordEnumeration re = rs.enumerateRecords(null, null, false);
        while (re.hasNextElement()) {
            int rID = re.nextRecordId();
            String r = new String(rs.getRecord(rID));
            int pos = r.indexOf('=');
            if (pos >= 0) {
                String name = r.substring(0, pos);
                String value = r.substring(pos + 1);
                valueMap.put(name, value);
                Integer rid = new Integer(rID);
                recordIdMap.put(name, rid);
            }
        }
    }

    public String getConfigString(String key) {
        String s = null;

        try {
            s = (String)valueMap.get(key);
        } catch (NullPointerException npe) {
            return null;
        }

        return s;
    }

    public boolean setConfigString(String key, String value) {
        String record = key+"="+value;
        Integer rid = null;
        try {
            rid = (Integer)recordIdMap.get(key);
        } catch (NullPointerException npe) {}

        if (rid == null) {
            try {
                int newId = rs.addRecord(record.getBytes(), 0, record.length());
                valueMap.put(key, value);
                recordIdMap.put(key, new Integer(newId));
            } catch (RecordStoreException re) {
                return false;
            }
        } else {
            //The key exists.
            try {
                rs.setRecord(rid.intValue(), record.getBytes(), 0, record.length());
                valueMap.put(key, value);
            } catch (RecordStoreException re) {
                return false;
            }
        }
        return true;
    }

    public boolean deleteConfigString(String key) {
        Integer rid = null;
        try {
            rid = (Integer)recordIdMap.get(key);
        } catch (NullPointerException npe) {}

        if (rid == null)
            return true; //Nothing to delete.
        try {
            rs.deleteRecord(rid.intValue());
        } catch (RecordStoreException re) {
            return false;
        }

        return true;
    }

    protected void finalize() {
        try {
            rs.closeRecordStore();
        } catch (RecordStoreException re) {
        }

        rs = null;
    }
}
