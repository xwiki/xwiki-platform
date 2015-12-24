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
package org.xwiki.filter.event.model;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.annotation.Default;
import org.xwiki.filter.annotation.Name;

/**
 * Wiki related events.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public interface WikiFilter
{
    /**
     * @param name the name of the wiki
     * @param parameters the properties of the wiki
     * @throws FilterException when failing to send event
     */
    void beginWiki(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws FilterException;

    /**
     * @param name the name of the wiki
     * @param parameters the properties of the wiki
     * @throws FilterException when failing to send event
     */
    void endWiki(@Name("name") String name,
        @Default(FilterEventParameters.DEFAULT) @Name(FilterEventParameters.NAME) FilterEventParameters parameters)
        throws FilterException;
}
