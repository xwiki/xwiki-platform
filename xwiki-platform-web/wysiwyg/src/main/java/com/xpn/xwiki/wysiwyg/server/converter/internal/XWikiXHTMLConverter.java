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

import org.xwiki.rendering.DocumentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.WikiPrinter;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.xhtml.WysiwygEditorXHTMLRenderer;
import org.xwiki.rendering.transformation.TransformationManager;

import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.server.converter.XHTMLConverter;
import com.xpn.xwiki.wysiwyg.server.converter.XHTMLConverterException;

public class XWikiXHTMLConverter implements XHTMLConverter
{
    /**
     * {@inheritDoc}
     * 
     * @see XHTMLConverter#fromXHTML(String)
     */
    public String fromXHTML(String xhtml) throws XHTMLConverterException
    {
        try {
            Parser parser = (Parser) Utils.getComponent(Parser.ROLE, "xhtml/1.0");
            XDOM dom = parser.parse(new StringReader(xhtml));
            WikiPrinter printer = new DefaultWikiPrinter();
            XWikiSyntaxRenderer renderer = new XWikiSyntaxRenderer(printer);
            dom.traverse(renderer);
            return printer.toString();
        } catch (ParseException e) {
            throw new XHTMLConverterException("Exception while parsing HTML", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLConverter#toXHTML(String)
     */
    public String toXHTML(String source) throws XHTMLConverterException
    {
        try {
            Parser parser = (Parser) Utils.getComponent(Parser.ROLE, "xwiki/2.0");
            XDOM dom = parser.parse(new StringReader(source));

            TransformationManager txManager = (TransformationManager) Utils.getComponent(TransformationManager.ROLE);
            txManager.performTransformations(dom, new Syntax(SyntaxType.XWIKI, "2.0"));

            WikiPrinter printer = new DefaultWikiPrinter();
            DocumentManager docManager = (DocumentManager) Utils.getComponent(DocumentManager.ROLE);
            WysiwygEditorXHTMLRenderer renderer = new WysiwygEditorXHTMLRenderer(printer, docManager);
            dom.traverse(renderer);
            return printer.toString();
        } catch (Throwable t) {
            throw new XHTMLConverterException("Exception while parsing XWiki", t);
        }
    }
}
