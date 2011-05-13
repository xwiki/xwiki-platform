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
package org.xwiki.gwt.wysiwyg.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a menu item.
 * 
 * @version $Id$
 */
public class MenuItemDescriptor
{
    /**
     * The feature exposed by this menu item.
     */
    private String feature;

    /**
     * The sub-menu.
     */
    private final List<MenuItemDescriptor> subMenu = new ArrayList<MenuItemDescriptor>();

    /**
     * Creates a new menu item that exposes the specified feature.
     * 
     * @param feature the feature name
     */
    public MenuItemDescriptor(String feature)
    {
        this.feature = feature;
    }

    /**
     * @return the feature exposed by this menu item
     */
    public String getFeature()
    {
        return feature;
    }

    /**
     * Sets the feature exposed by this menu item.
     * 
     * @param feature the feature name
     */
    public void setFeature(String feature)
    {
        this.feature = feature;
    }

    /**
     * @return the sub-menu
     */
    public List<MenuItemDescriptor> getSubMenu()
    {
        return subMenu;
    }
}
