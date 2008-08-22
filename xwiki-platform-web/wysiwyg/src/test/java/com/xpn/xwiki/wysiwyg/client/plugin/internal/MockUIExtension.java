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

import com.google.gwt.user.client.ui.UIObject;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;

/**
 * Mock user interface extension to be used in unit tests.
 */
public class MockUIExtension implements UIExtension
{
    private final String role;

    private final String feature;

    public MockUIExtension(String role, String feature)
    {
        this.role = role;
        this.feature = feature;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#getFeatures()
     */
    public String[] getFeatures()
    {
        return new String[] {feature};
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#getRole()
     */
    public String getRole()
    {
        return role;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#getUIObject(String)
     */
    public UIObject getUIObject(String feature)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#isEnabled(String)
     */
    public boolean isEnabled(String feature)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#setEnabled(String, boolean)
     */
    public void setEnabled(String feature, boolean enabled)
    {
        // ignore
    }
}
