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
package org.xwiki.livedata.livetable;

import java.util.Map;
import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.stability.Unstable;

/**
 * Gives the {@link LiveTableNewRowNamingStrategy} components installed in the current wiki.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Role
@Unstable
public interface LiveTableNewRowNamingStrategyManager
{
    /**
     * Gets the strategy installed under the given hint.
     *
     * @param hint the strategy hint, for example {@code uuid} or {@code title}
     * @return the strategy, or {@link Optional#empty()} when the hint is {@code null}, blank, or names no installed
     *     strategy
     * @throws LiveDataException if a strategy is installed under that hint but cannot be loaded
     */
    Optional<LiveTableNewRowNamingStrategy> get(String hint) throws LiveDataException;

    /**
     * Gets every strategy installed in the current wiki, leaving out the ones that cannot be loaded.
     *
     * @return the installed strategies by hint, sorted by hint
     */
    Map<String, LiveTableNewRowNamingStrategy> getAll();
}
