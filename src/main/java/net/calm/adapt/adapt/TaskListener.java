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
