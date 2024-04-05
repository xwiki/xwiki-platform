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

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.image.style.model.ImageStyle;

/**
 * Gives access to the image styles of the wiki.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Role
public interface ImageStyleManager
{
    /**
     * The list of styles for a given wiki.
     *
     * @param wikiName the wiki name (e.g. {@code xwiki})
     * @return the list of styles for the given wiki
     * @throws ImageStyleException if an error occurs while retrieving the image styles
     */
    Set<ImageStyle> getImageStyles(String wikiName) throws ImageStyleException;
}
