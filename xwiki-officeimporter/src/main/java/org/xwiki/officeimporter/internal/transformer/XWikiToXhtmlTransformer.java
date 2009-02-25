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
package org.xwiki.officeimporter.internal.transformer;

import java.io.StringReader;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.transformer.DocumentTransformer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Transforms an xwiki 2.0 document into xhtml 1.0 document.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class XWikiToXhtmlTransformer extends AbstractLogEnabled implements DocumentTransformer
{
    /**
     * Factory to get the XHTML renderer to output XHTML.
     */
    private PrintRendererFactory rendererFactory;

    /**
     * Parser for parsing XWiki 2.0 Syntax
     */
    private Parser parser;
    
    /**
     * {@inheritDoc}
     */
    public void transform(OfficeImporterContext importerContext) throws OfficeImporterException
    {
        try {
            XDOM xdom = this.parser.parse(new StringReader(importerContext.getContent()));
            WikiPrinter printer = new DefaultWikiPrinter();
            Listener listener = this.rendererFactory.createRenderer(new Syntax(SyntaxType.XHTML, "1.0"), printer); 
            xdom.traverse(listener);
            importerContext.setContent(printer.toString());
        } catch (ParseException ex) {
            String message = "Internal error while parsing xwiki 2.0 content.";
            getLogger().error(message, ex);
            throw new OfficeImporterException(message, ex);
        }
    }

}
