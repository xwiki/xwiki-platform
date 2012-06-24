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

package org.xwiki.gwt.wysiwyg.client.plugin.internal;

import org.xwiki.gwt.user.client.ui.MenuItem;

import com.google.gwt.user.client.ui.UIObject;

/**
 * Concrete implementation of the {@link AbstractUIExtension}. Each feature must have associated a menu item.
 * 
 * @version $Id$
 */
public class MenuItemUIExtension extends AbstractUIExtension
{
    /**
     * Creates a new user interface extension that will extend the specified extension point, a menu, with menu items.
     * 
     * @param role The role of the newly created user interface extension.
     */
    public MenuItemUIExtension(String role)
    {
        super(role);
    }

    @Override
    public boolean isEnabled(String feature)
    {
        UIObject uiObject = getUIObject(feature);
        if (uiObject instanceof MenuItem) {
            return ((MenuItem) uiObject).isEnabled();
        }
        return false;
    }

    @Override
    public void setEnabled(String feature, boolean enabled)
    {
        UIObject uiObject = getUIObject(feature);
        if (uiObject instanceof MenuItem) {
            ((MenuItem) uiObject).setEnabled(enabled);
        }
    }
}
