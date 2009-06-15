/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.microedition.lcdui.Image;
import java.io.IOException;
import javax.microedition.io.HttpConnection;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.microedition.io.Connector;

/**
 *
 * @author anandi
 */
public class HTTPUtil {
    /*
     * Copied from : http://developers.sun.com/mobility/midp/questions/calcbyte/
     */
    private static byte[] readFromHTTPConnection(HttpConnection hpc) {
        byte[] bytes;

        try {
            InputStream in = hpc.openDataInputStream();
            int length = (int) hpc.getLength();
            if (length == -1) {
                // Reading from an HTTP 1.0 server or a chunked HTTP 1.1
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int c ;
                while (true) {
                    c = in.read();
                    if (c == -1)
                        break;
                    baos.write(c);
                }
                bytes = baos.toByteArray();
                baos.close();
                baos = null;
            } else {
                // Reading a Content-Length labeled payload
                bytes = new byte[length];
                in.read(bytes, 0, length);
            }
            in.close();
            in = null;
        } catch (IOException e) {
            System.out.println("Could not open input stream for HTTP conneciton");
            return null;
        }

        return bytes;
    }

    public static String httpGetRequest(String url) throws IOException {
        HttpConnection hpc = null;
        DataInputStream dis = null;
        try {
            hpc = (HttpConnection) Connector.open(url);
            byte[] data = readFromHTTPConnection(hpc);
            return new String(data);
        } finally {
            if (hpc != null)
                hpc.close();
            if (dis != null)
                dis.close();
        }
    }

    public static Image loadImage(String url) throws IOException {
        HttpConnection hpc = null;
        DataInputStream dis = null;
        try {
            hpc = (HttpConnection) Connector.open(url);
            byte[] data = readFromHTTPConnection(hpc);
            return Image.createImage(data, 0, data.length);
        } finally {
            if (hpc != null)
                hpc.close();
            if (dis != null)
                dis.close();
        }
    }
}
