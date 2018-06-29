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

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Component to render an icon, depending on the current icon theme set on the preferences.
 *
 * @version $Id$
 * @since 6.2M1
 */
@Role
public interface IconManager
{
    /**
     * Icon set name used.
     */
    String META_DATA_ICON_SET_NAME = "iconSetName";
    /**
     * Type of the icon set used.
     */
    String META_DATA_ICON_SET_TYPE = "iconSetType";
    /**
     * URL of the icon image.
     */
    String META_DATA_URL = "url";
    /**
     * CSS class of the icon.
     */
    String META_DATA_CSS_CLASS = "cssClass";

    /**
     * Generate the wiki syntax to display an icon with the current icon theme.
     *
     * @param iconName name of the icon to render
     * @return the wiki syntax that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     */
    String render(String iconName) throws IconException;

    /**
     * Generate the wiki syntax to display an icon with the specified icon theme.
     *
     * @param iconName name of the icon to render
     * @param iconSetName name of the icon set to use
     * @return the wiki syntax that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     * @since 6.3RC1
     */
    String render(String iconName, String iconSetName) throws IconException;

    /**
     * Generate the wiki syntax to display an icon with the specified icon theme.
     *
     * @param iconName name of the icon to render
     * @param iconSetName name of the icon set to use
     * @param fallback enable the fallback to the default icon theme if the icon does not exist
     * @return the wiki syntax that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     * @since 6.3RC1
     */
    String render(String iconName, String iconSetName, boolean fallback) throws IconException;

    /**
     * Generate the HTML code to display an icon.
     *
     * @param iconName name of the icon to render
     * @return the HTML code that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     */
    String renderHTML(String iconName) throws IconException;

    /**
     * Generate the HTML code to display an icon with the specified icon theme.
     *
     * @param iconName name of the icon to render
     * @param iconSetName name of the icon set to use
     * @return the HTML code that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     * @since 6.3RC1
     */
    String renderHTML(String iconName, String iconSetName) throws IconException;

    /**
     * Generate the HTML code to display an icon with the specified icon theme.
     *
     * @param iconName name of the icon to render
     * @param iconSetName name of the icon set to use
     * @param fallback enable the fallback to the default icon theme if the icon does not exist
     * @return the HTML code that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     * @since 6.3RC1
     */
    String renderHTML(String iconName, String iconSetName, boolean fallback) throws IconException;

    /**
     * Generate the custom code to display an icon.
     *
     * @param iconName name of the icon to render
     * @return the custom code that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     * @since 10.6RC1
     */
    default Map<String, Object> getMetaData(String iconName) throws IconException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Generate the custom code to display an icon with the specified icon theme.
     *
     * @param iconName name of the icon to render
     * @param iconSetName name of the icon set to use
     * @return the custom code that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     * @since 10.6RC1
     */
    default Map<String, Object> getMetaData(String iconName, String iconSetName) throws IconException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Generate the custom code to display an icon with the specified icon theme.
     *
     * @param iconName name of the icon to render
     * @param iconSetName name of the icon set to use
     * @param fallback enable the fallback to the default icon theme if the icon does not exist
     * @return the custom code that displays the icon or an empty string if the icon does not exist
     * @throws IconException if problems occur
     * @since 10.6RC1
     */
    default Map<String, Object> getMetaData(String iconName, String iconSetName, boolean fallback) throws IconException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the list of the names of all available icons in the current icon set.
     *
     * @return the icon names
     * @throws IconException if problem occurs
     * @since 6.4M1
     */
    List<String> getIconNames() throws IconException;

    /**
     * Get the list of the names of all available icons in the specified icon set.
     *
     * @param iconSetName name of the icon set
     * @return the icon names
     * @throws IconException if problem occurs
     * @since 6.4M1
     */
    List<String> getIconNames(String iconSetName) throws IconException;
}
