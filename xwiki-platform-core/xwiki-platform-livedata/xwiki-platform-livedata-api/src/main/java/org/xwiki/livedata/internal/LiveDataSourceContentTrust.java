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
package org.xwiki.livedata.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.livedata.LiveDataQuery;

/**
 * Tells whether the content a Live Data source produces for a given source configuration can be displayed without
 * being sanitized.
 * <p>
 * {@link LiveDataRenderer} asks this question only when the Live Data has no advanced configuration and its author
 * doesn't have script right. The content is then trusted only if the source confirms that, with these source
 * parameters, the entries and their HTML come from code it controls rather than from a location the Live Data author
 * can choose. Implementations are looked up with the source identifier as hint, and a source without an implementation
 * is never trusted in that case.
 *
 * @version $Id$
 * @since 19.0.0RC1
 * @since 18.10.0
 * @since 18.4.9
 * @since 17.10.16
 * @since 16.10.20
 */
@Role
public interface LiveDataSourceContentTrust
{
    /**
     * @param source the source configuration of the Live Data, with its identifier and parameters
     * @return {@code true} if the content produced by the source for this configuration can be displayed without
     *     being sanitized, {@code false} otherwise
     */
    boolean isContentTrusted(LiveDataQuery.Source source);
}
