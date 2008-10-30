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

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.Plugin;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginManager;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.util.Config;

/**
 * Default implementation of the {@link PluginManager} interface. We don't plan to provide another implementation.
 */
public class DefaultPluginManager implements PluginManager
{
    /**
     * The map of loaded plug-ins. The key is the name of the plug-in, as returned by
     * {@link PluginFactory#getPluginName()}.
     */
    private final Map<String, Plugin> loadedPlugins;

    /**
     * The map of available user interface extensions. The first key is the extension point, as returned by
     * {@link UIExtension#getRole()}, and the second key is the feature, as returned by
     * {@link UIExtension#getFeatures()}.
     */
    private final Map<String, Map<String, UIExtension>> uiExtensions;

    private final Wysiwyg wysiwyg;

    private final XRichTextArea textArea;

    private final Config config;

    private PluginFactoryManager pfm;

    public DefaultPluginManager(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        this.wysiwyg = wysiwyg;
        this.textArea = textArea;
        this.config = config;
        loadedPlugins = new HashMap<String, Plugin>();
        uiExtensions = new HashMap<String, Map<String, UIExtension>>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginManager#getPluginFactoryManager()
     */
    public PluginFactoryManager getPluginFactoryManager()
    {
        return pfm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginManager#setPluginFactoryManager(PluginFactoryManager)
     */
    public void setPluginFactoryManager(PluginFactoryManager pfm)
    {
        this.pfm = pfm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginManager#load(String)
     */
    public void load(String pluginName)
    {
        if (loadedPlugins.containsKey(pluginName)) {
            // plug-in already loaded
            return;
        }
        PluginFactory factory = pfm.getPluginFactory(pluginName);
        if (factory == null) {
            // factory not found
            return;
        }
        // create a new plug-in instance and initialize it
        Plugin plugin = factory.newInstance();
        loadedPlugins.put(pluginName, plugin);
        plugin.init(wysiwyg, textArea, config);
        // cache new plug-in's UI extensions
        UIExtension[] newUIExtensions = plugin.getUIExtensions();
        for (int i = 0; i < newUIExtensions.length; i++) {
            UIExtension newUIExtension = newUIExtensions[i];
            String[] newFeatures = newUIExtension.getFeatures();
            Map<String, UIExtension> oldUIExtensions = getUIExtensions(newUIExtension.getRole());
            for (int j = 0; j < newFeatures.length; j++) {
                oldUIExtensions.put(newFeatures[j], newUIExtension);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginManager#unload(String)
     */
    public void unload(String pluginName)
    {
        Plugin plugin = (Plugin) loadedPlugins.remove(pluginName);
        if (plugin != null) {
            // remove UI extensions from cache
            UIExtension[] pluginUIExtensions = plugin.getUIExtensions();
            for (int i = 0; i < pluginUIExtensions.length; i++) {
                UIExtension pluginUIExtension = pluginUIExtensions[i];
                String[] pluginFeatures = pluginUIExtension.getFeatures();
                Map<String, UIExtension> loadedUIExtensions = getUIExtensions(pluginUIExtension.getRole());
                for (int j = 0; j < pluginFeatures.length; j++) {
                    if (pluginUIExtension.equals(loadedUIExtensions.get(pluginFeatures[j]))) {
                        loadedUIExtensions.remove(pluginFeatures[j]);
                    }
                }
            }
            plugin.destroy();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PluginManager#getUIExtension(String, String)
     */
    public UIExtension getUIExtension(String role, String feature)
    {
        return getUIExtensions(role).get(feature);
    }

    private Map<String, UIExtension> getUIExtensions(String role)
    {
        Map<String, UIExtension> roleUIExtensions = uiExtensions.get(role);
        if (roleUIExtensions == null) {
            roleUIExtensions = new HashMap<String, UIExtension>();
            uiExtensions.put(role, roleUIExtensions);
        }
        return roleUIExtensions;
    }
}
