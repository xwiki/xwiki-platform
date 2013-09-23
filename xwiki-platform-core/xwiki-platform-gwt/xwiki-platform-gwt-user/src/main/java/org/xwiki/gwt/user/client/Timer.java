/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.gwt.user.client;

/**
 * Extends {@link com.google.gwt.user.client.Timer} by allowing timer listeners to be registered and notified whenever
 * the run method is being called.
 * 
 * @version $Id$
 */
public class Timer extends com.google.gwt.user.client.Timer implements SourcesTimerEvents
{
    /**
     * The registered listeners.
     */
    private final TimerListenerCollection timerListeners = new TimerListenerCollection();

    @Override
    public void addTimerListener(TimerListener listener)
    {
        timerListeners.add(listener);
    }

    @Override
    public void removeTimerListener(TimerListener listener)
    {
        timerListeners.remove(listener);
    }

    @Override
    public void run()
    {
        // notify all registered listeners
        timerListeners.fireTimer(this);
    }
}
