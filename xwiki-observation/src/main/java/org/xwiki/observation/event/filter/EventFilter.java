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
 *
 */
package org.xwiki.observation.event.filter;

/**
* Allows writing complex Event algorithms for the
 *{@link org.xwiki.observation.event.Event#matches(Object)} method.
 *
 * <p>
 * For example this allows
 * writing {@link org.xwiki.observation.event.filter.RegexEventFilter} that can be used to
 * easily match several documents at once. For example the following will match all Documents
 * which are saved which have a name matching the ".*Doc.*" regex: 
 * <code><pre>
 *   new DocumentSaveEvent(new RegexEventFilter(".*Doc.*"))
 * </pre></code>
 * </p>
 */
public interface EventFilter
{
    /**
     * @return the filter used in the {@link #matches(EventFilter)} method to verify if a
     *         passed event filter matches it.
     * @see #matches(EventFilter) 
     */
    String getFilter();

    /**
     * @param eventFilter the event filter to compare to the filter value
     * @return true if both event filters match. The matching algorithm is left to the filter
     *         event implementation. For example the Regex event filter will match another
     *         filter if that other filter matches the regex.
     */
    boolean matches(EventFilter eventFilter);
}
