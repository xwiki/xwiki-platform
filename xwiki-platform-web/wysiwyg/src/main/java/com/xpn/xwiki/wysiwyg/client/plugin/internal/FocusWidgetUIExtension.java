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
package com.xpn.xwiki.wysiwyg.client.plugin.internal;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.UIObject;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;

/**
 * Concrete implementation of the {@link UIExtension} interface. Each feature must have associated an user interface
 * object derived from {@link FocusWidget}.
 */
public class FocusWidgetUIExtension extends AbstractUIExtension
{
    public FocusWidgetUIExtension(String role)
    {
        super(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#isEnabled(String)
     */
    public boolean isEnabled(String feature)
    {
        UIObject uiObject = getUIObject(feature);
        if (uiObject != null && uiObject instanceof FocusWidget) {
            return ((FocusWidget) uiObject).isEnabled();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#setEnabled(String, boolean)
     */
    public void setEnabled(String feature, boolean enabled)
    {
        UIObject uiObject = getUIObject(feature);
        if (uiObject != null && uiObject instanceof FocusWidget) {
            ((FocusWidget) uiObject).setEnabled(enabled);
        }
    }
}
