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
package org.xwiki.resource;

import java.util.List;
import java.util.Map;

/**
 * Represents a reference to an XWiki Resource (Entity Resource, Attachment Resource, Template Resource, etc).
 *
 * @version $Id$
 * @since 6.1M2
 */
public interface ResourceReference
{
    /**
     * @return the type of Resource (Entity Resource, Attachment Resource, Template Resource, etc)
     */
    ResourceType getType();

    /**
     * @param name the name of the parameter to add
     * @param value the value of the parameter to add. If null then no value is added. Collections are also supported
     *        in which case a multivalued parameter is used.
     */
    void addParameter(String name, Object value);

    /**
     * A Resource Reference parameter provides optional additional information about the Resource Reference.
     * For example these will find their way into the Query String when the Resource Reference is serialized to a
     * standard URL.
     *
     * Note that there can be several values for the same name (since for example this is allowed in URLs and we want
     * to map a URL to an XWiki Resource Reference). Also note that the order in the map is the same as the order in the
     * representation when it was parsed.
     *
     * @return the Resource Reference parameters
     */
    Map<String, List<String>> getParameters();

    /**
     * @param name the parameter name for which to return the values
     * @return all the parameter values matching the passed parameter name
     */
    List<String> getParameterValues(String name);
    
    /**
     * @param name the parameter name for which to return the value
     * @return the first parameter value matching the passed parameter name
     */
    String getParameterValue(String name);
}
