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

import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;

import com.google.gwt.user.client.ui.UIObject;

/**
 * Mock user interface extension to be used in unit tests.
 * 
 * @version $Id$
 */
public class MockUIExtension implements UIExtension
{
    /**
     * @see #getRole()
     */
    private final String role;

    /**
     * This mock UI extension offers just a single feature.
     * 
     * @see #getFeatures()
     */
    private final String feature;

    /**
     * Creates a new mock UI extension having the specified role and providing the specified feature.
     * 
     * @param role Identifies the extension point.
     * @param feature The name of the feature to be provided by the newly created mock UI extension.
     */
    public MockUIExtension(String role, String feature)
    {
        this.role = role;
        this.feature = feature;
    }

    @Override
    public String[] getFeatures()
    {
        return new String[] {feature};
    }

    @Override
    public String getRole()
    {
        return role;
    }

    @Override
    public UIObject getUIObject(String feature)
    {
        return null;
    }

    @Override
    public boolean isEnabled(String feature)
    {
        return false;
    }

    @Override
    public void setEnabled(String feature, boolean enabled)
    {
        // ignore
    }
}
