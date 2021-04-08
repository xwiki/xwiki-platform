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

import org.xwiki.component.annotation.Role;

/**
 * Configuration options for the URL module.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Role
public interface URLConfiguration
{
    /**
     * @return the id of the URL format to use when converting a URL to a Resource. This allows to plug in different
     *         implementations and thus allows to completely control the format of XWiki URLs.
     */
    String getURLFormatId();

    /**
     * @return true means that we'll check for the last modification date of a resource to put it as a query parameter
     *         of the URL. This mechanism allows to avoid cache issues with the browser when modifying a file.
     *         false means we'll fallback on the wiki version to handle this cache issue.
     *         Its default value is true.
     * @since 11.1RC1
     */
    default boolean useResourceLastModificationDate()
    {
        return true;
    }
}
