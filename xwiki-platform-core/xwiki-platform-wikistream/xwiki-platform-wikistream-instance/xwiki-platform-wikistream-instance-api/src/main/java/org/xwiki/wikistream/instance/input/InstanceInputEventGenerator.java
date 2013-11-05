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
package org.xwiki.wikistream.instance.input;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.InstanceFilter;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
@Role
public interface InstanceInputEventGenerator extends InstanceFilter
{
    /**
     * Set custom parameter related to the farm.
     * 
     * @param parameters the parameters to set
     * @throws WikiStreamException when failing to set parameters
     * @since 5.3M2
     */
    void setWikiFarmParameters(FilterEventParameters parameters) throws WikiStreamException;

    /**
     * Set custom parameter related to the wiki.
     * 
     * @param name the name of wiki
     * @param parameters the parameters to set
     * @throws WikiStreamException when failing to set parameters
     * @since 5.3M2
     */
    void setWikiParameters(String name, FilterEventParameters parameters) throws WikiStreamException;

    /**
     * Set custom parameter related to the space.
     * 
     * @param name the name of space
     * @param parameters the parameters to set
     * @throws WikiStreamException when failing to set parameters
     * @since 5.3M2
     */
    void setWikiSpaceParameters(String name, FilterEventParameters parameters) throws WikiStreamException;

    /**
     * Set custom parameter related to the document.
     * 
     * @param name the name of document
     * @param parameters the parameters to set
     * @throws WikiStreamException when failing to set parameters
     * @since 5.3M2
     */
    void setWikiDocumentParameters(String name, FilterEventParameters parameters) throws WikiStreamException;

    /**
     * @param filter the filter to send events to
     */
    void setFilter(Object filter);

    /**
     * @param properties the event generator properties
     */
    void setProperties(Map<String, Object> properties);
}
