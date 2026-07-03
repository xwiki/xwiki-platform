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
package org.xwiki.image.style;

import org.xwiki.component.annotation.Role;

/**
 * Give access to the image style configuration.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Role
public interface ImageStyleConfiguration
{
    /**
     * Resolve the default style for a given wiki and, optionally a given document.
     *
     * @param wikiName the name of the wiki (e.g., "xwiki")
     * @param documentReference the reference of the document for which we want to get the default style, ignored
     *     when {@code null}
     * @return the identifier of the default image style (the empty string by default, i.e. no default image style)
     */
    String getDefaultStyle(String wikiName, String documentReference) throws ImageStyleException;

    /**
     * Resolve the force default style for a given wiki and, optionally a given document.
     *
     * @param wikiName the name of the wiki (e.g., "xwiki")
     * @param documentReference the reference of the document for which we want to get the default style, ignored
     *     when {@code null}
     * @return the value of the force default style configuration option
     * @since 14.10.16
     * @since 15.5.2
     * @since 15.8RC1
     */
    default boolean getForceDefaultStyle(String wikiName, String documentReference) throws ImageStyleException
    {
        return false;
    }
}
