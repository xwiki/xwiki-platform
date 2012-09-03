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
package org.xwiki.uiextension.internal.scripting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.script.service.ScriptService;
import org.xwiki.uiextension.UIExtension;

/**
 * Allows scripts to easily access Interface Extensions APIs.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("uix")
@Singleton
public class UIExtensionScriptService implements ScriptService
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * We use the Context Component Manager to lookup UI Extensions registered as components.
     * The Context Component Manager allows Extensions to be registered for a specific user, for a specific wiki or for
     * a whole farm.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    /**
     * Used to render the content of the extension to XHTML.
     */
    @Inject
    @Named("xhtml/1.0")
    private BlockRenderer renderer;

    /**
     * The execution context, used to access to the {@link com.xpn.xwiki.XWikiContext}.
     */
    @Inject
    private Execution execution;

    /**
     * Get the extensions for a given extension point. By default extensions are ordered alphabetically by their id.
     *
     * @param extensionPointId the ID of the extension point to retrieve the extensions for
     * @return the list of extensions registered for the given extension point
     */
    public Collection<UIExtension> getExtensions(String extensionPointId)
    {
        Map<String, UIExtension> results = new TreeMap<String, UIExtension>();
        Map<String, UIExtension> extensions = null;

        try {
            extensions = this.componentManager.get().getInstanceMap(UIExtension.class);
        } catch (ComponentLookupException e) {
            logger.error("Failed to lookup for UIExtension instances, error: [{}]", e.getMessage());
        }

        if (extensions != null) {
            for (UIExtension extension : extensions.values()) {
                if (extension.getExtensionPointId().equals(extensionPointId)) {
                    results.put(extension.getId(), extension);
                }
            }
        }

        return results.values();
    }

    /**
     * Render in HTML all the extensions provided for a given extension point.
     *
     * @param extensionPointId the ID of the extension point to render the extensions for
     * @return the HTML resulting of the rendering of the extensions registered for the given extension point
     */
    public String render(String extensionPointId)
    {
        Collection<UIExtension> uiExtensions = getExtensions(extensionPointId);
        XDOM xdom = new XDOM(new ArrayList<Block>());

        for (UIExtension uiExtension : uiExtensions) {
            xdom.addChild(uiExtension.getXDOM());
        }

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(xdom, printer);
        return printer.toString();
    }
}
