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
package org.xwiki.filter.instance.input;

import java.util.Collection;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.descriptor.FilterStreamDescriptor;
import org.xwiki.filter.instance.internal.InstanceFilter;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Role
public interface InstanceInputEventGenerator extends InstanceFilter
{
    /**
     * Set custom parameter related to the farm.
     * 
     * @param parameters the parameters to set
     * @throws FilterException when failing to set parameters
     * @since 6.2M1
     */
    void setWikiFarmParameters(FilterEventParameters parameters) throws FilterException;

    /**
     * Set custom parameter related to the wiki.
     * 
     * @param name the name of wiki
     * @param parameters the parameters to set
     * @throws FilterException when failing to set parameters
     * @since 6.2M1
     */
    void setWikiParameters(String name, FilterEventParameters parameters) throws FilterException;

    /**
     * Set custom parameter related to the space.
     * 
     * @param name the name of space
     * @param parameters the parameters to set
     * @throws FilterException when failing to set parameters
     * @since 6.2M1
     */
    void setWikiSpaceParameters(String name, FilterEventParameters parameters) throws FilterException;

    /**
     * Set custom parameter related to the document.
     * 
     * @param name the name of document
     * @param parameters the parameters to set
     * @throws FilterException when failing to set parameters
     * @since 6.2M1
     */
    void setWikiDocumentParameters(String name, FilterEventParameters parameters) throws FilterException;

    /**
     * @param filter the filter to send events to
     */
    void setFilter(Object filter);

    /**
     * @param properties the event generator properties
     */
    void setProperties(Map<String, Object> properties);

    /**
     * @return The FilterStreamDescriptor describes a FilterStream and has the list of bean class parameters or
     *         properties.
     */
    FilterStreamDescriptor getDescriptor();

    /**
     * @return the filters supported by this stream factory
     * @throws FilterException when failing to get filters interfaces
     */
    Collection<Class< ? >> getFilterInterfaces() throws FilterException;
}
