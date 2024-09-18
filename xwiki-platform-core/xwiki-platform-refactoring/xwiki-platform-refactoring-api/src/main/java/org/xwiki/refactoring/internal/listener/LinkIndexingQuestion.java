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
package org.xwiki.refactoring.internal.listener;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * The question if the job should continue waiting for link indexing to complete.
 *
 * @version $Id$
 * @since 16.8.0
 * @since 16.4.4
 * @since 15.10.13
 */
public class LinkIndexingQuestion
{
    private boolean continueWaiting;

    /**
     * @return if the job should continue waiting for link indexing to complete
     */
    public boolean isContinueWaiting()
    {
        return this.continueWaiting;
    }

    /**
     * @param continueWaiting if the job should continue waiting for link indexing to complete
     */
    @PropertyDescription("Continue waiting for link indexing to complete. If not selected, back-links will be "
        + "refactored with possibly incomplete information.")
    public void setContinueWaiting(boolean continueWaiting)
    {
        this.continueWaiting = continueWaiting;
    }
}
