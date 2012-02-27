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
 * Schedules updates for an {@link Updatable} object and ensures that only the most recent update is actually executed.
 * 
 * @version $Id$
 */
public class DeferredUpdater implements TimerListener
{
    /**
     * The default number of milliseconds to wait before executing an update.
     */
    public static final int DEFAULT_DELAY = 500;

    /**
     * The underlying object whose update is being deferred.
     */
    private final Updatable updatable;

    /**
     * The timer used to defer the updates.
     */
    private final Timer timer = new Timer();

    /**
     * The number of milliseconds to wait before executing an update.
     */
    private final int delay;

    /**
     * Creates a new deferred updater for the specified {@link Updatable} object. The updates are delayed with the
     * default number of milliseconds, {@value #DEFAULT_DELAY}. Only the most recent update is executed.
     * 
     * @param updatable the object whose updates are going to be deferred
     */
    public DeferredUpdater(Updatable updatable)
    {
        this(updatable, DEFAULT_DELAY);
    }

    /**
     * Creates a new deferred updater for the specified {@link Updatable} object. The updates are delayed with the
     * specified number of milliseconds. Only the most recent update is executed.
     * 
     * @param updatable the object whose updates are going to be deferred
     * @param delay the number of milliseconds to wait before executing an update
     */
    public DeferredUpdater(Updatable updatable, int delay)
    {
        this.updatable = updatable;
        this.delay = delay;
        this.timer.addTimerListener(this);
    }

    /**
     * Cancels any pending updates and schedule a new update for the underlying object.
     */
    public void deferUpdate()
    {
        // The pending update is canceled automatically when we schedule a new update.
        timer.schedule(delay);
    }

    @Override
    public void onElapsed(Timer sender)
    {
        try {
            if (updatable.canUpdate()) {
                updatable.update();
            }
        } catch (Throwable t) {
            Console.getInstance().error("Deferred update failed!", t);
        }
    }
}
