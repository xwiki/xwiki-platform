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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.internal.OfficeImporterContext;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Transforms an html document into xwiki 2.0 document.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class HtmlToXWikiTwoZeroTransformer extends AbstractHtmlToXWikiTransformer
{
    /**
     * Default constructor.
     * 
     * @param componentManager {@link ComponentManager} used to lookup for components.
     */
    public HtmlToXWikiTwoZeroTransformer(ComponentManager componentManager)
    {
        super(componentManager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts xhtml code to xwiki syntax 2.0. This method makes use of
     * {@link WikiModelXHTMLParser} and {@link XWikiSyntaxRenderer}.
     * </p>
     */
    public void transform(OfficeImporterContext importerContext) throws OfficeImporterException
    {
        importerContext.setTargetDocumentSyntaxId(new Syntax(SyntaxType.XWIKI, "2.0")
            .toIdString());
        String encodedContent = importerContext.getEncodedContent();
        String filteredXhtml = filterHTML(encodedContent, importerContext);
        try {
            Parser parser =
                (Parser) componentManager.lookup(Parser.ROLE, new Syntax(SyntaxType.XHTML, "1.0")
                    .toIdString());
            XDOM xdom = parser.parse(new StringReader(filteredXhtml));
            WikiPrinter printer = new DefaultWikiPrinter();
            Listener listener = new XWikiSyntaxRenderer(printer);
            xdom.traverse(listener);
            importerContext.setTargetDocumentContent(printer.toString());
            importerContext.finalizeDocument(false);
        } catch (ComponentLookupException ex) {
            getLogger().error("Erro while looking up for xhtml to xwiki 2.0 parser.", ex);
            throw new OfficeImporterException(ex);
        } catch (ParseException ex) {
            getLogger().error("Error while parsing the xhtml document.", ex);
            throw new OfficeImporterException(ex);
        }
    }
}
