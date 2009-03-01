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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Schedules updates for an {@link Updatable} object and ensures that only the most recent update is actually executed.
 * 
 * @version $Id$
 */
public class DeferredUpdater
{
    /**
     * A deferred command that executes only the most recent update.
     */
    private final class UpdateCommand implements Command
    {
        /**
         * The index of this update.
         */
        private final long index;

        /**
         * Creates a new update command.
         */
        public UpdateCommand()
        {
            index = DeferredUpdater.this.incUpdateIndex();
        }

        /**
         * Executes the update only if it's the most recent one.
         */
        public void execute()
        {
            if (index == DeferredUpdater.this.getUpdateIndex()) {
                DeferredUpdater.this.onUpdate();
            }
        }
    }

    /**
     * The index of the last update.
     */
    private long updateIndex = -1;

    /**
     * The underlying object whose update is being deferred.
     */
    private final Updatable updatable;

    /**
     * Creates a new deferred updater for the specified {@link Updatable} object.
     * 
     * @param updatable {@link #updatable}
     */
    public DeferredUpdater(Updatable updatable)
    {
        this.updatable = updatable;
    }

    /**
     * @return {@link #updateIndex}
     */
    private long getUpdateIndex()
    {
        return updateIndex;
    }

    /**
     * @return the update index after it was incremented
     */
    private long incUpdateIndex()
    {
        return ++updateIndex;
    }

    /**
     * Executes the most recent update.
     */
    private void onUpdate()
    {
        updatable.update();
    }

    /**
     * Schedule an update for the underlying object.
     */
    public void deferUpdate()
    {
        DeferredCommand.addCommand(new UpdateCommand());
    }
}
