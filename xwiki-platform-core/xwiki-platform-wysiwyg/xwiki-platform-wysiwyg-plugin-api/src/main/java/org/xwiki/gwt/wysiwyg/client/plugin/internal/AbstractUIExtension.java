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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;

import com.google.gwt.user.client.ui.UIObject;

/**
 * Abstract implementation of the {@link UIExtension} interface. This could serve as a base class for all kind of user
 * interface extensions. It offers the possibility of adding and removing user interface features.
 * 
 * @version $Id$
 */
public abstract class AbstractUIExtension implements UIExtension
{
    /**
     * The association between features and the user interface objects that trigger/expose those features.
     */
    private final Map<String, UIObject> uiObjects;

    /**
     * The name of the extension point. Specifies what's the role of this UI extension. Examples of extension point
     * (roles) are: tool bar, menu bar or status bar.
     */
    private final String role;

    /**
     * Creates a new user interface extension with the specified role.
     * 
     * @param role The name of the extension point where the newly created UI extension fits.
     */
    public AbstractUIExtension(String role)
    {
        this.role = role;
        uiObjects = new HashMap<String, UIObject>();
    }

    @Override
    public String[] getFeatures()
    {
        Set<String> features = uiObjects.keySet();
        return features.toArray(new String[features.size()]);
    }

    @Override
    public String getRole()
    {
        return role;
    }

    @Override
    public UIObject getUIObject(String feature)
    {
        return uiObjects.get(feature);
    }

    /**
     * Associates an UI object with a feature. This UI object is supposed to trigger or expose the feature.
     * 
     * @param feature A feature like <em>bold</em> or <em>insertMacro</em>.
     * @param uiObject A user interface object like a {@link com.google.gwt.user.client.ui.PushButton}.
     * @return The previously UI object associated with the given feature.
     */
    public UIObject addFeature(String feature, UIObject uiObject)
    {
        return uiObjects.put(feature, uiObject);
    }

    /**
     * Removes the specified feature from this UI extension.
     * 
     * @param feature A feature like <em>bold</em> or <em>insertMacro</em>.
     * @return The previously UI object associated with the given feature.
     */
    public UIObject removeFeature(String feature)
    {
        return uiObjects.remove(feature);
    }

    /**
     * Removes all features from this UI extension.
     */
    public void clearFeatures()
    {
        uiObjects.clear();
    }
}
