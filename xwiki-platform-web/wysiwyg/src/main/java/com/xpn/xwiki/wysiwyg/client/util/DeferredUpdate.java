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

/**
 * Interface for an object whose update can be deferred. Only the most recent update gets executed.
 */
public interface DeferredUpdate
{
    /**
     * A deferred command that executes only the most recent update.
     */
    static final class UpdateCommand implements Command
    {
        /**
         * The object that should be updated.
         */
        private final DeferredUpdate target;

        /**
         * The index of this update.
         */
        private final long index;

        public UpdateCommand(DeferredUpdate target)
        {
            this.target = target;
            index = target.incUpdateIndex();
        }

        public void execute()
        {
            // Executes the update only if it's the most recent one.
            if (index == target.getUpdateIndex()) {
                target.onUpdate();
            }
        }
    }

    /**
     * @return The index of the last update.
     */
    long getUpdateIndex();

    /**
     * @return The update index after it was incremented.
     */
    long incUpdateIndex();

    /**
     * Executes the most recent update.
     */
    void onUpdate();
}
