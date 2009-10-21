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
package com.xpn.xwiki.wysiwyg.server.plugin;

import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.server.cleaner.HTMLCleaner;

/**
 * Api for the WysiwygPlugin.
 * 
 * @version $Id$
 */
public class WysiwygPluginApi extends Api
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(WysiwygPluginApi.class);

    /**
     * The plugin instance.
     */
    private WysiwygPlugin plugin;

    /**
     * Creates a new API instance for the given plug-in in the specified context.
     * 
     * @param plugin The underlying plug-in of this plug-in API.
     * @param context The XWiki context.
     */
    public WysiwygPluginApi(WysiwygPlugin plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    /**
     * @return The underlying plug-in of this plug-in API.
     * @see #plugin
     */
    public WysiwygPlugin getPlugin()
    {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    /**
     * @param plugin The underlying plug-in of this plug-in API.
     * @see #plugin
     */
    public void setPlugin(WysiwygPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Checks if there is a parser and a renderer available for the specified syntax.
     * 
     * @param syntaxId the syntax identifier, like <em>xwiki/2.0</em>
     * @return {@code true} if the specified syntax is currently supported by the editor, {@code false} otherwise
     */
    public boolean isSyntaxSupported(String syntaxId)
    {
        try {
            Utils.getComponent(Parser.class, syntaxId);
            Utils.getComponent(PrintRendererFactory.class, syntaxId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses the given HTML fragment and renders the result in annotated XHTML syntax.
     * 
     * @param html the HTML fragment to be rendered
     * @param syntax the storage syntax
     * @return the XHTML result of rendering the given HTML fragment
     */
    public String parseAndRender(String html, String syntax)
    {
        try {
            // Parse
            Parser parser = Utils.getComponent(Parser.class, Syntax.XHTML_1_0.toIdString());
            HTMLCleaner cleaner = Utils.getComponent(HTMLCleaner.class);
            XDOM xdom = parser.parse(new StringReader(cleaner.clean(html)));

            // Execute macros
            SyntaxFactory syntaxFactory = Utils.getComponent(SyntaxFactory.class);
            TransformationManager txManager = Utils.getComponent(TransformationManager.class);
            txManager.performTransformations(xdom, syntaxFactory.createSyntaxFromIdString(syntax));

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            BlockRenderer renderer = Utils.getComponent(BlockRenderer.class, Syntax.ANNOTATED_XHTML_1_0.toIdString());
            renderer.render(xdom, printer);

            return printer.toString();
        } catch (Exception e) {
            LOG.error("Couldn't refresh WYSIWYG content!", e);
            return html;
        }
    }
}
