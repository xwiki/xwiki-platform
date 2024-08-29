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
package org.xwiki.extension.security.internal.analyzer.osv.model.response;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.xwiki.text.XWikiToStringBuilder;

/**
 * See the <a href="https://ossf.github.io/osv-schema/#affectedranges-field">Open Source Vulnerability format API
 * documentation</a>.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class RangeObject
{
    private List<EventObject> events;

    /**
     * @return the events field
     */
    public List<EventObject> getEvents()
    {
        return this.events;
    }

    /**
     * @param events the events field
     */
    public void setEvents(List<EventObject> events)
    {
        this.events = events;
    }

    /**
     * Search for the first {@link #getEvents()} with a non-null {@link EventObject#getIntroduced()} and returns its
     * value.  Otherwise {@code null} is returned.
     *
     * @return the computed start field
     */
    public String getStart()
    {
        return getFirst(EventObject::getIntroduced).orElse(null);
    }

    /**
     * Search for the first {@link #getEvents()} with a non-null {@link EventObject#getFixed()} and returns its value.
     * If none is found, search for the first {@link #getEvents()} with a non-null {@link EventObject#getLastAffected()}
     * and returns its value. Otherwise {@code null} is returned.
     *
     * @return the computed end field
     */
    public String getEnd()
    {
        return getFirst(EventObject::getFixed)
            .or(() -> getFirst(EventObject::getLastAffected))
            .orElse(null);
    }

    private Optional<String> getFirst(Function<EventObject, String> getFixed)
    {
        return this.events.stream()
            .map(getFixed)
            .filter(Objects::nonNull)
            .findFirst();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("events", getEvents())
            .toString();
    }
}
