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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link EventFilter} that selects only events whose affected document name matches a regular expression.
 * 
 * @version $Id$
 */
public class RegexEventFilter implements EventFilter, Serializable
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /** The regular expression, as a string. */
    private String filter;

    /** The regular expression, as a regexp Pattern object. */
    private Pattern pattern;

    /**
     * Constructor initializing this event filter with a regular expression that should be matched.
     * 
     * @param filter the regular expression to check against document names
     */
    public RegexEventFilter(String filter)
    {
        this.filter = filter;
        this.pattern = Pattern.compile(filter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see EventFilter#getFilter()
     */
    public String getFilter()
    {
        return this.filter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see EventFilter#getFilter()
     */
    public boolean matches(EventFilter eventFilter)
    {
        Matcher matcher = this.pattern.matcher(eventFilter.getFilter());
        return matcher.matches();
    }
}
