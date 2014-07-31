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
package org.xwiki.icon;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Component to render an icon, depending on the current icon theme set on the preferences.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Role
@Unstable
public interface IconManager
{
    /**
     * Generate the wiki syntax to display an icon.
     * @param iconName name of the icon to render
     * @return the wiki syntax that displays the icon
     * @throws IconException if problems occur
     */
    String render(String iconName) throws IconException;

    /**
     * Generate the HTML code to display an icon.
     * @param iconName name of the icon to render
     * @return the HTML code that displays the icon
     * @throws IconException if problems occur
     */
    String renderHTML(String iconName) throws IconException;

}
