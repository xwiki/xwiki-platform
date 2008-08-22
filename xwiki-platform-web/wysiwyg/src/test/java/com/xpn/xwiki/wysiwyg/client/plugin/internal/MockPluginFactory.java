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

import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactory;

/**
 * Mock plug-in factory to be used in unit tests.
 */
public class MockPluginFactory implements PluginFactory
{
    private final String pluginName;

    private final Plugin plugin;

    public MockPluginFactory(String pluginName, Plugin plugin)
    {
        this.pluginName = pluginName;
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginFactory#newInstance()
     */
    public Plugin newInstance()
    {
        return plugin;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginFactory#getPluginName()
     */
    public String getPluginName()
    {
        return pluginName;
    }
}
