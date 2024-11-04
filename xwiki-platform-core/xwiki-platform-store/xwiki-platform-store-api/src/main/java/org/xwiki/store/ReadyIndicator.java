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
package org.xwiki.store;

import java.util.concurrent.Future;

import org.xwiki.stability.Unstable;

/**
 * An indicator if a store is ready, i.e., has completed all writes/indexing tasks that have been submitted before
 * the indicator was requested. If the indexing tasks cannot be completed, e.g., because the indexer has been
 * stopped, the {@link Future} is completed with an exception.
 *
 * @since 16.9.0RC1
 * @version $Id$
 */
@Unstable
public interface ReadyIndicator extends Future<Void>
{
    /**
     *
     * @return a value between 0 and 100 that expresses the progress towards being ready. Values might jump
     * non-linearly and might not be fully accurate.
     */
    int getProgressPercentage();
}
