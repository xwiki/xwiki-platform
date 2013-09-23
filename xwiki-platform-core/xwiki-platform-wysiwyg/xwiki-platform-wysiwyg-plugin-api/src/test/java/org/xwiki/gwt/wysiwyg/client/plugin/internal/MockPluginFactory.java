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

import org.xwiki.gwt.wysiwyg.client.plugin.Plugin;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactory;

/**
 * Mock plug-in factory to be used in unit tests.
 * 
 * @version $Id$
 */
public class MockPluginFactory implements PluginFactory
{
    /**
     * @see #getPluginName()
     */
    private final String pluginName;

    /**
     * The plugin instance returned by {@link #newInstance()}.
     */
    private final Plugin plugin;

    /**
     * Creates a new mock plugin factory that doesn't really instantiate but returns the given plugin instance all the
     * time.
     * 
     * @param pluginName The name of the plugin returned by this factory.
     * @param plugin The plugin returned by this factory.
     */
    public MockPluginFactory(String pluginName, Plugin plugin)
    {
        this.pluginName = pluginName;
        this.plugin = plugin;
    }

    @Override
    public Plugin newInstance()
    {
        return plugin;
    }

    @Override
    public String getPluginName()
    {
        return pluginName;
    }
}
