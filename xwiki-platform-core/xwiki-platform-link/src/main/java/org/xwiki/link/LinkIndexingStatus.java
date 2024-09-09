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
package org.xwiki.link;

import java.util.concurrent.TimeUnit;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provides access to the indexing status of links in the {@link LinkStore}.
 *
 * @version $Id$
 * @since 16.8.0RC1
 * @since 15.10.13
 * @since 16.4.4
 *
 * @version $Id$
 */
@Unstable
@Role
public interface LinkIndexingStatus
{
    /**
     * @return the number of entities that are waiting to be indexed by the link store
     */
    int getQueueSize();

    /**
     * Wait for the link indexing queue to become empty, or the timeout to happen.
     *
     * @param timeout the maximum wait time
     * @param unit the unit of the wait time
     * @return the size of the queue after waiting
     * @throws InterruptedException if interrupted while waiting
     */
    int waitForEmpty(long timeout, TimeUnit unit) throws InterruptedException;
}
