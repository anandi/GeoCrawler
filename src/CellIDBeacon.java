/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;

/**
 *
 * @author anandi
 */
public class CellIDBeacon extends LocationBeacon {
    private String mnc;
    private String mcc;
    private String lac;
    private String cellid;

    //For the time being, I am keeping the opencellid API key here...
    private static String OPEN_CELL_ID_GET_URL = "http://www.opencellid.org/cell/get?";
    private static String OPEN_CELL_ID_SET_URL = "http://www.opencellid.org/measure/add?";

    public CellIDBeacon() {
        errorInMeter = 2000; //Should actually be meaningfully set by resolve!
    }

    /*
     * Code copied from http://www.easywms.com/easywms/?q=en/node/3589
     * Mashed up with coding style from
     * http://sunpo.spaces.live.com/blog/cns!E63009838F5BB82C!455.entry
     */
    /**
     * get the cell id in the phone
     *
     * @return
     */
    protected boolean getCellId() {
        String out = null;
        String[] keys = new String[] {
            "Cell-ID",
            "CellID",
            "phone.cid",
            "com.nokia.mid.cellid",
            "com.sonyericsson.net.cellid",
            "com.samsung.cellid",
            "com.siemens.cellid",
            "cid"
        };
        try {
            for (int i = 0 ; i < keys.length ; i++) {
                out = System.getProperty(keys[i]);
                if ((out != null) && !out.equals("null") && !out.equals(""))
                    break;
            }
        } catch (Exception e) {
            return false;
        }

//        if ((out == null) || out.equals("null") || out.equals("")) {
            //net.rim.device.api.system.GPRSInfo ? Class.forName?? Later!
            //#= out = GPRSInfo.getCellInfo().getCellId();
            if ((out == null) || out.equals("null") || out.equals(""))
                return false;
//        }
        cellid = out;
        return true;
    }

    public String getCellIdString() {
        return cellid;
    }

    public boolean setCellIdString(String value) {
        if ((value == null) || (value.equals("")))
            return false;
        cellid = value; //Should check for format etc.
        return true;
    }

    /**
     * get the lac sring from phone
     */
    protected boolean getLAC(){
        String out = null;
        String[] keys = new String[] {
            "phone.lac",
            "com.sonyericsson.net.lac",
            "com.nokia.mid.lac",
            "LocAreaCode",
            "com.sonyericsson.net.lac",
            "com.samsung.lac",
            "com.siemens.lac",
            "lac"
        };

        try {
            for (int i = 0 ; i < keys.length ; i++) {
                out = System.getProperty(keys[i]);
                if ((out != null) && !out.equals("null") && !out.equals(""))
                    break;
            }
        } catch (Exception e) {
            return false;
        }

//        if ((out == null) || out.equals("null") || out.equals("")) {
            //net.rim.device.api.system.GPRSInfo ? Class.forName?? Later!
            //out = GPRSInfo.getCellInfo().getLAC();
            if ((out == null) || out.equals("null") || out.equals(""))
                return false;
//        }
        lac = out;
        return true;
    }

    public String getLACString() {
        return lac;
    }

    public boolean setLACString(String value) {
        if ((value == null) || (value.equals("")))
            return false;
        lac = value; //Should check for format etc.
        return true;
    }

    /**
     * Example IMSI (O2 UK): 234103530089555
     * String mcc = imsi.substring(0,3); // 234 (UK)
     * String mnc = imsi.substring(3,5); // 10 (O2)
     * @return
     */
    protected String getIMSI() {
        String out = null;
        String[] keys = new String[] {
            "IMSI",
            "phone.imsi",
            "com.nokia.mid.mobinfo.IMSI",
            "imsi"
        };

        try {
            for (int i = 0 ; i < keys.length ; i++) {
                out = System.getProperty(keys[i]);
                if ((out != null) && !out.equals("null") && !out.equals(""))
                    return out;
            }
        } catch (Exception e) {
            out = "";
            return out;
        }

//        if ((out == null) || out.equals("null") || out.equals("")) {
            //net.rim.device.api.system.GPRSInfo ? Class.forName?? Later!
            //out = GPRSInfo.getCellInfo().getBSIC();
            //return out;
//        }

        if ((out == null) || out.equals("null") || out.equals(""))
            out = "";
        return out;
    }

    /**
     *
     * For moto, Example IMSI (O2 UK): 234103530089555
     * String mcc = imsi.substring(0,3); // 234 (UK)
     * @return
     */
    protected boolean getMCC(){
        String out = null;
        String[] keys = new String[] {
            "phone.mcc",
            "com.sonyericsson.net.mcc",
            "com.nokia.mid.countrycode",
            "mcc"
        };

        try {
            for (int i = 0 ; i < keys.length ; i++) {
                out = System.getProperty(keys[i]);
                if ((out != null) && !out.equals("null") && !out.equals(""))
                    break;
            }
        } catch (Exception e) {
            return false;
        }
//        if ((out == null) || out.equals("null") || out.equals("")) {
            //net.rim.device.api.system.GPRSInfo ? Class.forName?? Later!
            //out = GPRSInfo.getCellInfo().getMCC();
//        }
        if ((out == null) || out.equals("null") || out.equals(""))
            out = getIMSI().equals("")?"": getIMSI().substring(0,3);
        if ((out == null) || out.equals("null") || out.equals(""))
            return false;
        mcc = out;
        return true;
    }

    public String getMCCString() {
        return mcc;
    }

    public boolean setMCCString(String value) {
        if ((value == null) || (value.equals("")))
            return false;
        mcc = value; //Should check for format etc.
        return true;
    }

    /**
     * For moto, Example IMSI (O2 UK): 234103530089555
     * String mnc = imsi.substring(3,5); // 10 (O2)
     * @return
     */
    protected boolean getMNC() {
        String out = null;
        String[] keys = new String[] {
            "phone.mnc",
            "com.sonyericsson.net.mnc",
            "com.nokia.mid.networkid",
            "mnc"
        };

        try {
            for (int i = 0 ; i < keys.length ; i++) {
                out = System.getProperty(keys[i]);
                if ((out != null) && !out.equals("null") && !out.equals(""))
                    break;
            }
        } catch (Exception e) {
            return false;
        }
//        if ((out == null) || out.equals("null") || out.equals("")) {
            //net.rim.device.api.system.GPRSInfo ? Class.forName?? Later!
            //out = GPRSInfo.getCellInfo().getMNC();
//        }
        if ((out == null) || out.equals("null") || out.equals(""))
            out = getIMSI().equals("")?"": getIMSI().substring(3,5);
        if ((out == null) || out.equals("null") || out.equals(""))
            return false;
        mnc = out;
        return true;
    }

    public String getMNCString() {
        return mnc;
    }

    public boolean setMNCString(String value) {
        if ((value == null) || (value.equals("")))
            return false;
        mnc = value; //Should check for format etc.
        return true;
    }

    public boolean initialze() {
        //Nothing to initialize.
        return true;
    }

    public boolean update() {
        if (getCellId() && getLAC() && getMCC() && getMNC())
            return true;
        return false;
    }

    /* TBD: Resolve through Fire Eagle. Isn't there any open APIs!!?? */
    public boolean resolve() {
        if ((mnc == null) || (mnc.length() == 0))
            return false;
        if ((mcc == null) || (mcc.length() == 0))
            return false;
        if ((lac == null) || (lac.length() == 0))
            return false;
        if ((cellid == null) || (cellid.length() == 0))
            return false;
        return resolveWithOpenCellID();
    }

    private boolean resolveWithOpenCellID() {
        //Assume that the fields have already been checked.
        String request = new String(OPEN_CELL_ID_GET_URL);
        request = request.concat("key="+GeoCrawlerKey.OPEN_CELL_ID_KEY);
        request = request.concat("&mnc="+mnc);
        request = request.concat("&mcc="+mcc);
        request = request.concat("&lac="+lac);
        request = request.concat("&cellid="+cellid);

        System.err.println("Making a open cell id request to resolve cell: "+request);
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(request);
        } catch (IOException ioe) {
            System.err.println("Error in getting response");
            return false;
        }

        //Got an XML response...
        System.err.println("Got response: "+response);

        //Do a naive parse of OpenCellID response...
        if (response.indexOf("<rsp stat=\"ok\"") == -1) {
            System.err.println("Error response from open cell id");
            return false;
        }

        String valStr = getValueFromOpenCellIDGETResponse(response, "lat");
        if (valStr == null) {
            System.err.println("Could not find latitude in response");
            return false;
        }
        System.err.println("Got latitude: " + valStr);
        double latVal = Double.parseDouble(valStr);

        valStr = getValueFromOpenCellIDGETResponse(response, "lon");
        if (valStr == null) {
            System.err.println("Could not find longitude in response");
            return false;
        }
        System.err.println("Got longitude: " + valStr);
        double lonVal = Double.parseDouble(valStr);

        //Getting the error radius is optional!
        valStr = getValueFromOpenCellIDGETResponse(response, "range");
        if (valStr != null)
            errorInMeter = Double.parseDouble(valStr);

        this.setLatLon(latVal, lonVal);
        return true;
    }

    private String getValueFromOpenCellIDGETResponse(String response,
                                                                   String key) {
        int valBegin = response.indexOf(key+"=");
        if (valBegin == -1)
            return null;
        valBegin += key.length() + 2; //Add the string '<key>="'
        int valEnd = response.indexOf("\"", valBegin);
        if (valEnd == -1)
            return null;
        return response.substring(valBegin, valEnd);
    }

    public void updateService(double lat, double lon) {
        if ((mnc == null) || (mnc.length() == 0))
            return;
        if ((mcc == null) || (mcc.length() == 0))
            return;
        if ((lac == null) || (lac.length() == 0))
            return;
        if ((cellid == null) || (cellid.length() == 0))
            return;
        this.updateOpenCellID(lat, lon);
    }

    private void updateOpenCellID(double lat, double lon) {
        //Assume that the fields have already been checked.
        String request = new String(OPEN_CELL_ID_SET_URL);
        request = request.concat("key="+GeoCrawlerKey.OPEN_CELL_ID_KEY);
        request = request.concat("&mnc="+mnc);
        request = request.concat("&mcc="+mcc);
        request = request.concat("&lac="+lac);
        request = request.concat("&cellid="+cellid);
        request = request.concat("&lat="+Double.toString(lat));
        request = request.concat("&lon="+Double.toString(lon));

        System.err.println("Making a open cell id request to add cell info: "+request);
        String response = null;
        try {
            response = HTTPUtil.httpGetRequest(request);
        } catch (IOException ioe) {
            System.err.println("Error in getting response");
        }
    }
}