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

import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactoryManager;


/**
 * Default implementation of the {@link PluginFactoryManager}. We don't plan to provide another implementation.
 * 
 * @version $Id$
 */
public final class DefaultPluginFactoryManager implements PluginFactoryManager
{
    /**
     * The map of registered factories. The key is the name of the plug-in, as returned by
     * {@link PluginFactory#getPluginName()}.
     */
    private Map<String, PluginFactory> factories = new HashMap<String, PluginFactory>();

    @Override
    public PluginFactory addPluginFactory(PluginFactory factory)
    {
        return factories.put(factory.getPluginName(), factory);
    }

    @Override
    public PluginFactory getPluginFactory(String pluginName)
    {
        return factories.get(pluginName);
    }

    @Override
    public PluginFactory removePluginFactory(String pluginName)
    {
        return factories.remove(pluginName);
    }
}
