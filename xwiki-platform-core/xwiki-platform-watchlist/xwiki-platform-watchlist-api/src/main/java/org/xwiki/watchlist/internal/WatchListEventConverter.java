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
package org.xwiki.watchlist.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.watchlist.internal.api.WatchListEvent;

/**
 * Converts a given object to a {@link WatchListEvent}, if possible.
 * 
 * @param <T> the type of the object we are converting from
 * @version $Id$
 */
@Role
public interface WatchListEventConverter<T>
{
    /**
     * @param from the object to convert from
     * @param parameters any additional parameters needed in the conversion
     * @return a {@link WatchListEvent} if the conversion was possible, {@code null} otherwise
     */
    WatchListEvent convert(T from, Object... parameters);
}
