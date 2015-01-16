/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
/**
 *
 * An interface that can be used by the NotificationThread class to notify an
 *
 * object that a thread has completed.  *
 * @author Greg Cope
 *
 */
public interface TaskListener {

    /**
     *
     * Notifies this object that the Runnable object has completed its work.      *
     * @param runner The runnable interface whose work has finished.
     *
     */
    public void threadComplete(Runnable runner);

}
