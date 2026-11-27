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
package com.xpn.xwiki.internal.plugin;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.event.ComponentDescriptorRemovedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiPluginManager;

/**
 * Add and remove the plugins registered and unregistered as {@link XWikiPluginInterface} components in the root
 * component manager (for example by the installation or uninstallation of an extension on the farm).
 * <p>
 * The plugins already registered when the plugin manager is created are added by
 * {@link XWikiPluginManager#addComponentPlugins(ComponentManager, XWikiContext)}.
 *
 * @version $Id$
 * @since 18.10.0
 */
@Component
@Named(XWikiPluginComponentListener.NAME)
@Singleton
public class XWikiPluginComponentListener extends AbstractEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "com.xpn.xwiki.internal.plugin.XWikiPluginComponentListener";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public XWikiPluginComponentListener()
    {
        super(NAME, List.of(new ComponentDescriptorAddedEvent(XWikiPluginInterface.class),
            new ComponentDescriptorRemovedEvent(XWikiPluginInterface.class)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ComponentManager componentManager = (ComponentManager) source;

        // Plugins are global so only the ones registered in the root component manager are taken into account
        if (componentManager instanceof NamespacedComponentManager namespacedComponentManager
            && namespacedComponentManager.getNamespace() != null) {
            return;
        }

        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiPluginManager pluginManager = getPluginManager(xcontext);
        // If XWiki is not initialized yet, the plugin will be added when the plugin manager is created
        if (pluginManager != null) {
            ComponentDescriptor<?> descriptor = (ComponentDescriptor<?>) data;
            if (event instanceof ComponentDescriptorAddedEvent) {
                onPluginAdded(descriptor.getRoleHint(), componentManager, pluginManager, xcontext);
            } else {
                onPluginRemoved(descriptor, pluginManager);
            }
        }
    }

    private void onPluginRemoved(ComponentDescriptor<?> descriptor, XWikiPluginManager pluginManager)
    {
        // Don't remove a plugin with the same name coming from somewhere else (for example from xwiki.cfg)
        XWikiPluginInterface plugin = pluginManager.getPlugin(descriptor.getRoleHint());
        if (plugin != null && plugin.getClass() == descriptor.getImplementation()) {
            pluginManager.removePlugin(descriptor.getRoleHint());
        }
    }

    private XWikiPluginManager getPluginManager(XWikiContext xcontext)
    {
        if (xcontext != null) {
            XWiki xwiki = xcontext.getWiki();
            if (xwiki != null) {
                return xwiki.getPluginManager();
            }
        }

        return null;
    }

    private void onPluginAdded(String hint, ComponentManager componentManager, XWikiPluginManager pluginManager,
        XWikiContext xcontext)
    {
        XWikiPluginInterface plugin;
        try {
            plugin = componentManager.getInstance(XWikiPluginInterface.class, hint);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup plugin component [{}]. This plugin will not be available.", hint, e);

            return;
        }

        if (pluginManager.addComponentPlugin(hint, plugin, xcontext)) {
            // The wikis which are already initialized won't call the plugin virtualInit
            virtualInit(plugin, xcontext);
        }
    }

    @SuppressWarnings("deprecation")
    private void virtualInit(XWikiPluginInterface plugin, XWikiContext xcontext)
    {
        String currentWiki = xcontext.getWikiId();
        try {
            for (String wiki : xcontext.getWiki().getVirtualWikiList()) {
                xcontext.setWikiId(wiki);
                try {
                    plugin.virtualInit(xcontext);
                } catch (Exception e) {
                    this.logger.error("Failed to initialize plugin [{}] in wiki [{}]", plugin.getName(), wiki, e);
                }
            }
        } finally {
            xcontext.setWikiId(currentWiki);
        }
    }
}
