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
package com.xpn.xwiki.wysiwyg.server.converter.internal;

import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.gwt.wysiwyg.client.cleaner.HTMLCleaner;
import org.xwiki.gwt.wysiwyg.client.converter.HTMLConverter;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationManager;


/**
 * Converts HTML into/from xwiki/2.0 syntax.
 * 
 * @version $Id$
 */
public class DefaultHTMLConverter implements HTMLConverter
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(DefaultHTMLConverter.class);

    /**
     * The component used to clean the HTML before the conversion.
     */
    @Requirement()
    private HTMLCleaner htmlCleaner;

    /**
     * The component used to parse the XHTML obtained after cleaning.
     */
    @Requirement("xhtml/1.0")
    private Parser xhtmlParser;

    /**
     * The component used to create syntax instances from syntax identifiers.
     */
    @Requirement
    private SyntaxFactory syntaxFactory;

    /**
     * The component used to execute the XDOM transformations before rendering to XHTML.
     */
    @Requirement
    private TransformationManager transformationManager;

    /**
     * The component used to render a XDOM to XHTML.
     */
    @Requirement("annotatedxhtml/1.0")
    private BlockRenderer xhtmlRenderer;

    /**
     * The component manager. We need it because we have to access some components dynamically based on the input
     * syntax.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see HTMLConverter#fromHTML(String, String)
     */
    public String fromHTML(String dirtyHTML, String syntaxId)
    {
        try {
            // Clean
            String html = htmlCleaner.clean(dirtyHTML);

            // Parse
            XDOM xdom = xhtmlParser.parse(new StringReader(html));

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            BlockRenderer renderer = componentManager.lookup(BlockRenderer.class, syntaxId);
            renderer.render(xdom, printer);

            return printer.toString();
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Exception while parsing HTML", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HTMLConverter#toHTML(String, String)
     */
    public String toHTML(String source, String syntaxId)
    {
        try {
            // Parse
            Parser parser = componentManager.lookup(Parser.class, syntaxId);
            XDOM xdom = parser.parse(new StringReader(source));

            // Execute transformations
            transformationManager.performTransformations(xdom, syntaxFactory.createSyntaxFromIdString(syntaxId));

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            xhtmlRenderer.render(xdom, printer);

            return printer.toString();
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Exception while rendering HTML", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HTMLConverter#parseAndRender(String, String)
     */
    public String parseAndRender(String dirtyHTML, String syntax)
    {
        try {
            // Clean
            String html = htmlCleaner.clean(dirtyHTML);

            // Parse
            XDOM xdom = xhtmlParser.parse(new StringReader(html));

            // Execute transformations
            transformationManager.performTransformations(xdom, syntaxFactory.createSyntaxFromIdString(syntax));

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            xhtmlRenderer.render(xdom, printer);

            return printer.toString();
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Exception while refreshing HTML", e);
        }
    }
}
