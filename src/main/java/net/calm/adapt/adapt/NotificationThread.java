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
package net.calm.adapt.adapt;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A single-background-thread observer/callback wrapper. Subclasses implement
 * {@link #doWork()} (where all work is performed); {@link #run()} calls
 * {@code doWork()} and then notifies any registered {@link TaskListener}s.
 *
 * @author Greg Cope
 */
public abstract class NotificationThread implements Runnable {

    /**
     * Where subclasses perform all work.
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
     * Runs {@link #doWork()} and then notifies all registered listeners.
     */
    public void run() {

        doWork();

        notifyListeners();

    }

}
