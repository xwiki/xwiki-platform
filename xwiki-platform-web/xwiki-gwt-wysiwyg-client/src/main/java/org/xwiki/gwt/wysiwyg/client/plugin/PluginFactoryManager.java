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
package org.xwiki.gwt.wysiwyg.client.plugin;

/**
 * Manages the list of available plug-in factories.
 * 
 * @version $Id$
 */
public interface PluginFactoryManager
{
    /**
     * Registers a new plug-in factory.
     * 
     * @param factory The plug-in factory to be registered.
     * @return The previous plug-in factory registered with the same plug-in name.
     * @see PluginFactory#getPluginName()
     */
    PluginFactory addPluginFactory(PluginFactory factory);

    /**
     * @param pluginName The name of the plug-in whose factory should be returned.
     * @return The factory of the specified plug-in or <code>null</code> is there's no factory for the specified
     *         plug-in.
     */
    PluginFactory getPluginFactory(String pluginName);

    /**
     * Unregisters the factory for the given plug-in name.
     * 
     * @param pluginName The name of the plug-in whose factory should be unregistered.
     * @return The factory that was unregistered, or <code>null</code> if no factory has been registered for the given
     *         plug-in name.
     */
    PluginFactory removePluginFactory(String pluginName);
}
