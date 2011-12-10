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
package org.xwiki.url;

import java.util.List;
import java.util.Map;

/**
 * Represents a XWiki URL.
 *
 * @version $Id$
 */
public interface XWikiURL
{
    /**
     * @return the type of URL (Entity URL, Attachment URL, Template URL, etc)
     */
    XWikiURLType getType();

    /**
     * @param value the value of the parameter to add. If null then no value is added.
     */
    void addParameter(String name, String value);

    /**
     * A XWiki URL parameter provides optional additional information about the URL.
     * For example these will find their way into the Query String when the XWiki URL serialized to a standard URL.
     * Note that there can be several values for the same name (since this is allowed in URLs and we want to map a
     * URL to a XWiki URL). Also note that the order in the map is the same as the order in the URL.
     *
     * @return the XWiki URL parameters
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
