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
package org.xwiki.skinx.internal.async;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.async.AsyncContextHandler;
import org.xwiki.skinx.SkinExtension;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.skinx.AbstractSkinExtensionPlugin;

/**
 * {@link AsyncContextHandler} handler for skin extensions.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Named(SkinExtensionAsync.USER_TYPE)
@Singleton
public class SkinExtensionAsyncContextHandler implements AsyncContextHandler
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public void use(Collection<Object> values)
    {
        for (Object value : values) {
            use(value);
        }
    }

    private void use(Object value)
    {
        SkinExtensionInfo info = (SkinExtensionInfo) value;

        // Find the right skin extension
        SkinExtension skinExtension;
        try {
            skinExtension = this.componentManager.getInstance(SkinExtension.class, info.getType());
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to load the skin extension with type [{}]", info.getType(), e);

            return;
        }

        // Inject the skin extension
        if (info.getParameters() != null) {
            skinExtension.use(info.getResource(), info.getParameters());
        } else {
            skinExtension.use(info.getResource());
        }
    }

    @Override
    public void addHTMLHead(StringBuilder meta, Collection<Object> values)
    {
        // TODO: A bit hacky right now, the skinx module should be refactored to be fully based on components
        XWikiContext xcontext = this.xcontextProvider.get();
        List<XWikiPluginInterface> plugins = xcontext.getWiki().getPluginManager().getPlugins("endParsing");

        for (Object value : values) {
            SkinExtensionInfo info = (SkinExtensionInfo) value;

            AbstractSkinExtensionPlugin skinPlugin = getPlugin(info.getType(), plugins);

            if (skinPlugin != null) {
                meta.append(skinPlugin.getLink(info.getResource(), xcontext));
            } else {
                this.logger.warn("Cannot find skin extension plugin for resource type [{}]", info.getType());
            }
        }
    }

    private AbstractSkinExtensionPlugin getPlugin(String name, List<XWikiPluginInterface> plugins)
    {
        for (XWikiPluginInterface plugin : plugins) {
            if (plugin instanceof AbstractSkinExtensionPlugin && plugin.getName().equals(name)) {
                return (AbstractSkinExtensionPlugin) plugin;
            }
        }

        return null;
    }
}
