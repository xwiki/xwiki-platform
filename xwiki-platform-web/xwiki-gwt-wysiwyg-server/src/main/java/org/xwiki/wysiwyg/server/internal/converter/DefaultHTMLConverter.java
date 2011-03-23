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
package org.xwiki.wysiwyg.server.internal.converter;

import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.gwt.wysiwyg.client.cleaner.HTMLCleaner;
import org.xwiki.gwt.wysiwyg.client.converter.HTMLConverter;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;

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
     * The component used to parse the XHTML obtained after cleaning, when transformations are not executed.
     */
    @Requirement("xhtml/1.0")
    private StreamParser xhtmlStreamParser;

    /**
     * The component used to create syntax instances from syntax identifiers.
     */
    @Requirement
    private SyntaxFactory syntaxFactory;

    /**
     * The component used to execute the XDOM macro transformations before rendering to XHTML.
     * <p>
     * NOTE: We execute only macro transformations because they are the only transformations protected by the WYSIWYG
     * editor. We should use the transformation manager once generic transformation markers are implemented in the
     * rendering module and the WYSIWYG editor supports them.
     * 
     * @see XWIKI-3260: Add markers to modified XDOM by Transformations/Macros
     */
    @Requirement("macro")
    private Transformation macroTransformation;

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

            // Parse & Render
            // Note that transformations are not executed when converting XHTML to source syntax.
            WikiPrinter printer = new DefaultWikiPrinter();
            PrintRendererFactory printRendererFactory = componentManager.lookup(PrintRendererFactory.class, syntaxId);
            xhtmlStreamParser.parse(new StringReader(html), printRendererFactory.createRenderer(printer));

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

            // Execute macro transformations
            TransformationContext txContext = new TransformationContext();
            txContext.setXDOM(xdom);
            txContext.setSyntax(syntaxFactory.createSyntaxFromIdString(syntaxId));
            macroTransformation.transform(xdom, txContext);

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
    public String parseAndRender(String dirtyHTML, String syntaxId)
    {
        try {
            // Clean
            String html = htmlCleaner.clean(dirtyHTML);

            // Parse
            XDOM xdom = xhtmlParser.parse(new StringReader(html));

            // The XHTML parser sets the "syntax" meta data property of the created XDOM to "xhtml/1.0". The syntax meta
            // data is used as the default syntax for macro content. We have to change this to the specified syntax
            // because HTML is used only to be able to edit the source syntax in the WYSIWYG editor.
            Syntax syntax = syntaxFactory.createSyntaxFromIdString(syntaxId);
            xdom.getMetaData().addMetaData(MetaData.SYNTAX, syntax);

            // Execute macro transformations
            TransformationContext txContext = new TransformationContext();
            txContext.setXDOM(xdom);
            txContext.setSyntax(syntax);
            macroTransformation.transform(xdom, txContext);

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
