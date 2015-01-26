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
}
