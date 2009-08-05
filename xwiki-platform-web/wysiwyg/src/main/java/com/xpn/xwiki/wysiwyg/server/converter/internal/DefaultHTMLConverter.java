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

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.TransformationManager;

import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.server.converter.HTMLConverter;

/**
 * Converts HTML into/from xwiki/2.0 syntax.
 * 
 * @version $Id$
 */
@Component
public class DefaultHTMLConverter implements HTMLConverter
{
    /**
     * {@inheritDoc}
     * 
     * @see HTMLConverter#fromHTML(String, String)
     */
    public String fromHTML(String html, String syntaxId)
    {
        try {
            // Parse
            Parser parser = (Parser) Utils.getComponent(Parser.class, Syntax.XHTML_1_0.toIdString());
            XDOM xdom = parser.parse(new StringReader(html));

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            BlockRenderer renderer = (BlockRenderer) Utils.getComponent(BlockRenderer.class, syntaxId);
            renderer.render(xdom, printer);

            return printer.toString();
        } catch (ParseException e) {
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
            Parser parser = (Parser) Utils.getComponent(Parser.class, syntaxId);
            XDOM xdom = parser.parse(new StringReader(source));

            // Execute transformations
            TransformationManager txManager = (TransformationManager) Utils.getComponent(TransformationManager.class);
            SyntaxFactory syntaxFactory = (SyntaxFactory) Utils.getComponent(SyntaxFactory.class);
            Syntax syntax = syntaxFactory.createSyntaxFromIdString(syntaxId);
            txManager.performTransformations(xdom, syntax);

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            BlockRenderer renderer =
                (BlockRenderer) Utils.getComponent(BlockRenderer.class, Syntax.XHTML_1_0.toIdString());
            renderer.render(xdom, printer);

            return printer.toString();
        } catch (Throwable t) {
            throw new RuntimeException("Exception while rendering HTML", t);
        }
    }
}
