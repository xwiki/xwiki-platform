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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;

import com.google.gwt.user.client.ui.UIObject;

/**
 * Aggregates many {@link UIExtension} objects that have the same role.
 * 
 * @version $Id$
 */
public class CompositeUIExtension implements UIExtension
{
    /**
     * The list of {@link UIExtension} aggregated by this object.
     */
    private final List<UIExtension> extensions;

    /**
     * @see UIExtension#getRole()
     */
    private final String role;

    /**
     * Creates a new composite user interface extension with the specified role.
     * 
     * @param role The extension point where the newly created composite UI extension fits.
     */
    public CompositeUIExtension(String role)
    {
        this.role = role;
        extensions = new ArrayList<UIExtension>();
    }

    @Override
    public String[] getFeatures()
    {
        final List<String> allFeatures = new ArrayList<String>();
        for (UIExtension uie : extensions) {
            String[] features = uie.getFeatures();
            if (features == null) {
                continue;
            }
            for (int i = 0; i < features.length; i++) {
                allFeatures.add(features[i]);
            }
        }
        return allFeatures.toArray(new String[allFeatures.size()]);
    }

    @Override
    public String getRole()
    {
        return role;
    }

    @Override
    public UIObject getUIObject(String feature)
    {
        for (UIExtension uie : extensions) {
            UIObject uiObject = uie.getUIObject(feature);
            if (uiObject != null) {
                return uiObject;
            }
        }
        return null;
    }

    @Override
    public boolean isEnabled(String feature)
    {
        boolean enabled = false;
        for (UIExtension uie : extensions) {
            enabled = enabled || uie.isEnabled(feature);
        }
        return enabled;
    }

    @Override
    public void setEnabled(String feature, boolean enabled)
    {
        for (UIExtension uie : extensions) {
            uie.setEnabled(feature, enabled);
        }
    }

    /**
     * Aggregates the specified {@link UIExtension}.
     * 
     * @param uie A user interface extension having the same role as this composite.
     */
    public void addUIExtension(UIExtension uie)
    {
        if (role.equals(uie.getRole())) {
            extensions.add(uie);
        }
    }

    /**
     * Removes the specified {@link UIExtension} from the composite.
     * 
     * @param uie A user interface extension.
     */
    public void removeUIExtension(UIExtension uie)
    {
        extensions.remove(uie);
    }
}
