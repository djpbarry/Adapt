/*
 * Copyright (C) 2014 David Barry <david.barry at cancer.org.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.calm.adapt.Adapt;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
/**
 *
 * This abstract class implements the Runnable interface and can be used to
 * notify listeners when the runnable thread has completed. To use this class,
 * first extend it and implement the doRun function - the doRun function is
 * where all work should be performed. Add any listener to update upon completion,
 * then create a new thread with this new object and run. 
 * @author Greg Cope
 *
 *
 *
 */
public abstract class NotificationThread implements Runnable {

    /**
     *
     * An abstract function that children must implement. This function is where      *
     * all work - typically placed in the run of runnable - should be placed.      *
     */
    public abstract void doWork();

    /**
     *
     * Our list of listeners to be notified upon thread completion.
     *
     */
    private java.util.List<TaskListener> listeners = Collections.synchronizedList(new ArrayList<TaskListener>());

    /**
     *
     * Adds a listener to this object.      *
     * @param listener Adds a new listener to this object.      *
     */
    public void addListener(TaskListener listener) {

        listeners.add(listener);

    }

    /**
     *
     * Removes a particular listener from this object, or does nothing if the
     * listener
     *
     * is not registered.      *
     * @param listener The listener to remove.      *
     */
    public void removeListener(TaskListener listener) {

        listeners.remove(listener);

    }

    /**
     *
     * Notifies all listeners that the thread has completed.
     *
     */
    private final void notifyListeners() {

        synchronized (listeners) {

            for (TaskListener listener : listeners) {

                listener.threadComplete(this);

            }

        }

    }

    /**
     *
     * Implementation of the Runnable interface. This function first calls
     * doRun(), then
     *
     * notifies all listeners of completion.
     *
     */
    public void run() {

        doWork();

        notifyListeners();

    }

}
