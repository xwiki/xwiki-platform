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

import java.util.Map;

import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactoryManager;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginManager;
import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;


/**
 * Mock plugin manager to be used in unit tests.
 * 
 * @version $Id$
 */
public class MockPluginManager implements PluginManager
{
    /**
     * The map of user interface extensions.
     */
    private Map<String, Map<String, UIExtension>> uiExtensions;

    /**
     * Creates a new mock plugin manager that uses the given {@link UIExtension}s.
     * 
     * @param uiExtensions the map of user interface extensions to be used
     */
    public MockPluginManager(Map<String, Map<String, UIExtension>> uiExtensions)
    {
        this.uiExtensions = uiExtensions;
    }

    @Override
    public PluginFactoryManager getPluginFactoryManager()
    {
        return null;
    }

    @Override
    public UIExtension getUIExtension(String role, String feature)
    {
        return uiExtensions.get(role).get(feature);
    }

    @Override
    public void load(String pluginName)
    {
    }

    @Override
    public void setPluginFactoryManager(PluginFactoryManager pfm)
    {
    }

    @Override
    public void unload(String pluginName)
    {
    }

    @Override
    public void unloadAll()
    {
    }
}
