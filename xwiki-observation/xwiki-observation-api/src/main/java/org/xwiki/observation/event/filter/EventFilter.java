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
 * Allows writing complex Event matching algorithms for the {@link org.xwiki.observation.event.Event#matches(Object)}
 * method.
 * <p>
 * For example, this allows writing {@link org.xwiki.observation.event.filter.RegexEventFilter} that can be used to
 * easily match several documents at once. The following will match all Documents which are saved which have a name
 * matching the <tt>".*Doc.*"</tt> regex: <code><pre>
 * new DocumentSaveEvent(new RegexEventFilter(&quot;.*Doc.*&quot;))
 * </pre></code>
 * </p>
 * 
 * @version $Id$
 */
public interface EventFilter
{
    /**
     * Provides access to the filter's criterion.
     * 
     * @return the filter used in the {@link #matches(EventFilter)} method to verify if a passed event filter matches
     *         it.
     * @see #matches(EventFilter)
     */
    String getFilter();

    /**
     * Compares two event filters to see if they <em>match</em>, meaning that the "contexts" of two events are
     * compatible. For example, a {@link FixedNameEventFilter} matches another filter only if they both have the same
     * name set as the filter, while an {@link AlwaysMatchingEventFilter} matches any other event filter. A listener
     * that registered to receive notifications <em>like</em> <code>referenceEvent</code> and with
     * <code>referenceEventFilter</code>, will be notified of any occuring event for which
     * <code>referenceEvent.matches(occuringEvent)</code> will return <code>true</code> and
     * <code>referenceEvent.getEventFilter().matches(occurringEvent.getEventFilter())</code>.
     * 
     * @param eventFilter the event filter to compare to the filter value
     * @return <code>true</code> if both event filters match. The matching algorithm is left to the filter event
     *         implementation. For example the {@link RegexEventFilter Regex event filter} will match another filter if
     *         that other filter matches the regex.
     */
    boolean matches(EventFilter eventFilter);
}
