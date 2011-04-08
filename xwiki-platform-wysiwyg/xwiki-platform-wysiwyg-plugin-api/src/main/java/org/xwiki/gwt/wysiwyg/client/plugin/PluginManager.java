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
 * Manages the list of installed plug-ins.
 * 
 * @version $Id$
 */
public interface PluginManager
{
    /**
     * @return The manager of the available plug-in factories.
     */
    PluginFactoryManager getPluginFactoryManager();

    /**
     * Sets the manager of the available plug-in factories.
     * 
     * @param pfm The plug-in factory manager to use.
     */
    void setPluginFactoryManager(PluginFactoryManager pfm);

    /**
     * Loads the specified plug-in. A new instance of this plug-in is obtained using the current plug-in factory
     * manager. The
     * {@link Plugin#init(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, com.xpn.xwiki.wysiwyg.client.util.Config)}
     * method is called.
     * 
     * @param pluginName The name of the plug-in to be loaded. This name should match the value returned by
     *            {@link PluginFactory#getPluginName()} method of this plug-in's factory.
     */
    void load(String pluginName);

    /**
     * Unloads the specified plug-in. The {@link Plugin#destroy()} method is called.
     * 
     * @param pluginName The name of the plug-in to be unloaded. This name should match the value returned by
     *            {@link PluginFactory#getPluginName()} method of this plug-in's factory.
     */
    void unload(String pluginName);

    /**
     * Unloads all the plug-ins for which the {@link #load(String)} method has been called.
     */
    void unloadAll();

    /**
     * @param role The extension point. See {@link UIExtension#getRole()}.
     * @param feature The feature name. See {@link UIExtension#getFeatures()}.
     * @return The user interface extension for the given role and the given feature.
     */
    UIExtension getUIExtension(String role, String feature);
}
