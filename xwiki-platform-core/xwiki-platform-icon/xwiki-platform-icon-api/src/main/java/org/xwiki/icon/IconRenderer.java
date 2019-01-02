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

/**
 * Component to render an icon (either with Wiki Syntax or HTML).
 *
 * @version $Id$
 * @since 6.2M1
 */
@Role
public interface IconRenderer
{
    /**
     * Generate the wiki syntax to display an icon.
     *
     * @param iconName name of the icon to render
     * @param iconSet icon set that contains the icon to display
     * @return the wiki syntax that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     */
    String render(String iconName, IconSet iconSet) throws IconException;

    /**
     * Render an icon with the specified renderer.
     *
     * <p>The renderer contains velocity code which should use the <b>$icon</b> variable.
     * For instance:
     * <pre>String renderer = "fa fa-$icon";</pre>
     *
     * @param iconName name of the icon to render
     * @param iconSet icon set that contains the icon to display
     * @param renderer velocity code to render
     * @return rendered icon using the renderer through velocity
     * @throws IconException if problems occur
     * @since 10.6RC1
     */
    default String render(String iconName, IconSet iconSet, String renderer) throws IconException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Generate the HTML code to display an icon.
     *
     * @param iconName name of the icon to render
     * @param iconSet icon set that contains the icon to display
     * @return the HTML code that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     */
    String renderHTML(String iconName, IconSet iconSet) throws IconException;

    /**
     * Pull the necessary resources to use the specified icon set.
     *
     * @param iconSet icon set to use
     * @throws IconException if problems occur
     * @since 10.6RC1
     */
    default void use(IconSet iconSet) throws IconException
    {
        throw new UnsupportedOperationException();
    }
}
