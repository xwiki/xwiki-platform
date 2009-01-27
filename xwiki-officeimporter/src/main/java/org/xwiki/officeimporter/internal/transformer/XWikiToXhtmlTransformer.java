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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.transformer.DocumentTransformer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfiguration;
import org.xwiki.rendering.internal.parser.DefaultAttachmentParser;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.XHTMLRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Transforms an xwiki 2.0 document into xhtml 1.0 document.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class XWikiToXhtmlTransformer extends AbstractLogEnabled implements DocumentTransformer, Composable
{
    /**
     * Document access bridge used to access wiki documents.
     */
    private DocumentAccessBridge docBridge;
    
    /**
     * Component manager used to lookup for other components.
     */
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }
    
    /**
     * {@inheritDoc}
     */
    public void transform(OfficeImporterContext importerContext) throws OfficeImporterException
    {
        importerContext.setTargetDocumentSyntaxId(new Syntax(SyntaxType.XHTML, "1.0").toIdString());
        try {
            Parser parser =
                (Parser) componentManager.lookup(Parser.ROLE, new Syntax(SyntaxType.XWIKI, "2.0").toIdString());
            XDOM xdom = parser.parse(new StringReader(importerContext.getEncodedContent()));
            WikiPrinter printer = new DefaultWikiPrinter();
            RenderingConfiguration configuraiton = new DefaultRenderingConfiguration();
            AttachmentParser attachmentParser = new DefaultAttachmentParser();
            Listener listener = new XHTMLRenderer(printer, docBridge, configuraiton, attachmentParser);
            xdom.traverse(listener);
            importerContext.setTargetDocumentContent(printer.toString());
        } catch (ComponentLookupException ex) {
            String message = "Internal error while looking up for xwiki 2.0 parser.";
            getLogger().error(message, ex);
            throw new OfficeImporterException(ex);
        } catch (ParseException ex) {
            String message = "Internal error while parsing xwiki 2.0 content.";
            getLogger().error(message, ex);
            throw new OfficeImporterException(message, ex);
        }
    }

}
