/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public interface LocationConsumer {

    public abstract void locationCallback(LocationData location);
    public abstract void detectFailed();
}
