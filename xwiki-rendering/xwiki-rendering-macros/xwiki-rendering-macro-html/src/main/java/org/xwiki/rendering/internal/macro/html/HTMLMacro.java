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
package org.xwiki.rendering.internal.macro.html;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MacroTransformation;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.html.HTMLMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.xhtml.XHTMLRendererFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLConstants;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Allows inserting HTML and XHTML in wiki pages. This macro also accepts wiki syntax alongside (X)HTML elements (it's
 * also possible to disable this feature using a macro parameter). When wiki syntax is used inside XML elements, the
 * leading and trailing spaces and newlines are stripped.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Component("html")
public class HTMLMacro extends AbstractMacro<HTMLMacroParameters> implements Composable
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Inserts HTML or XHTML code into the page.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "The HTML content to insert in the page.";

    /**
     * The syntax representing the output of this macro (used for the RawBlock).
     */
    private static final Syntax XHTML_SYNTAX = new Syntax(SyntaxType.XHTML, "1.0");

    /**
     * To clean the passed HTML so that it's valid XHTML (this is required since we use an XML parser to parse it).
     */
    @Requirement
    private HTMLCleaner htmlCleaner;

    /**
     * Used to find the parser from syntax identifier.
     */
    private ComponentManager componentManager;

    /**
     * Factory to easily create an XHTML Image and Link Renderer.
     */
    @Requirement
    private XHTMLRendererFactory xhtmlRendererFactory;
    
    /**
     * Create and initialize the descriptor of the macro.
     */
    public HTMLMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            HTMLMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(HTMLMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> blocks;

        if (!StringUtils.isEmpty(content)) {

            String normalizedContent = content;

            // If the user has mentioned that there's wiki syntax in the macro then we parse the content using
            // a wiki syntax parser and render it back using a special renderer to print the XDOM blocks into
            // a text representing the resulting XHTML content.
            if (parameters.getWiki()) {
                normalizedContent =
                    renderWikiSyntax(normalizedContent, context.getMacroTransformation(), context.getSyntax());
            }

            // Clean the HTML into valid XHTML if the user has asked (it's the default).
            if (parameters.getClean()) {
                normalizedContent = cleanHTML(normalizedContent, context.isInline());
            }

            blocks = Arrays.asList((Block) new RawBlock(normalizedContent, XHTML_SYNTAX));
        } else {
            blocks = Collections.emptyList();
        }

        return blocks;
    }

    /**
     * Clean the HTML entered by the user, transforming it into valid XHTML.
     * 
     * @param content the content to clean
     * @param isInline true if the content is inline and thus if we need to remove the top level paragraph element
     *            created by the cleaner
     * @return the cleaned HTML as a string representing valid XHTML
     * @throws MacroExecutionException if the macro is inline and the content is not inline HTML
     */
    private String cleanHTML(String content, boolean isInline) throws MacroExecutionException
    {
        String cleanedContent = content;

        // Note that we trim the content since we want to be lenient with the user in case he has entered
        // some spaces/newlines before a XML declaration (prolog). Otherwise the XML parser would fail to parse.
        Document document = this.htmlCleaner.clean(new StringReader(cleanedContent));

        // Since XML can only have a single root node and since we want to allow users to put
        // content such as the following, we need to wrap the content in a root node:
        // <tag1>
        // ..
        // </tag1>
        // <tag2>
        // </tag2>
        // In addition we also need to ensure the XHTML DTD is defined so that valid XHTML entities can be
        // specified.

        // Remove the HTML envelope since this macro is only a fragment of a page which will already have an
        // HTML envelope when rendered. We remove it so that the HTML <head> tag isn't output.
        HTMLUtils.stripHTMLEnvelope(document);

        // If in inline mode verify we have inline HTML content and remove the top level paragraph if there's one
        if (isInline) {
            // TODO: Improve this since when're inside a table cell or a list item we can allow non inline items too
            Element root = document.getDocumentElement();
            if (root.getChildNodes().getLength() == 1 && root.getFirstChild().getNodeType() == Node.ELEMENT_NODE
                && root.getFirstChild().getNodeName().equalsIgnoreCase("p")) {
                HTMLUtils.stripFirstElementInside(document, HTMLConstants.TAG_HTML, HTMLConstants.TAG_P);
            } else {
                throw new MacroExecutionException(
                    "When using the HTML macro inline, you can only use inline HTML content."
                        + " Block HTML content (such as tables) cannot be displayed."
                        + " Try leaving an empty line before and after the HTML macro.");
            }
        }

        // Don't print the XML declaration nor the XHTML DocType.
        cleanedContent = XMLUtils.toString(document, true, true);

        // Don't print the top level html element (which is always present and at the same location
        // since it's been normalized by the HTML cleaner)
        // Note: we trim the first 7 characters since they correspond to a leading new line (generated by
        // XMLUtils.toString() since the doctype is printed on a line by itself followed by a new line) +
        // the 6 chars from "<html>".
        cleanedContent = cleanedContent.substring(7, cleanedContent.length() - 8);

        return cleanedContent;
    }

    /**
     * Parse the passed context using a wiki syntax parser and render the result as an XHTML string.
     * 
     * @param content the content to parse
     * @param macroTransformation the macro transformation to execute macros when wiki is set to true
     * @param wikiSyntax the wiki syntax used inside the HTML macro
     * @return the output XHTML as a string containing the XWiki Syntax resolved as XHTML
     * @throws MacroExecutionException in case there's a parsing problem
     */
    private String renderWikiSyntax(String content, MacroTransformation macroTransformation, Syntax wikiSyntax)
        throws MacroExecutionException
    {
        String xhtml;

        try {
            // Parse the wiki syntax
            Parser parser = this.componentManager.lookup(Parser.class, wikiSyntax.toIdString());
            XDOM xdom = parser.parse(new StringReader(content));

            // Force clean=false for sub HTML macro:
            // - at this point we don't know the context of the macro, it can be some <div> directly followed by the
            // html macro, it this case the macro will be parsed as inline block
            // - by forcing clean=false, we also make the html macro merge the whole html before cleaning so the cleaner
            // have the chole context and can clean better
            List<MacroBlock> macros = xdom.getChildrenByType(MacroBlock.class, true);
            for (MacroBlock macro : macros) {
                if (macro.getName().equals("html")) {
                    macro.setParameter("clean", "false");
                }
            }

            // Execute transformations
            macroTransformation.transform(xdom, parser.getSyntax());

            // Render the whole parsed content as a XHTML string
            WikiPrinter printer = new DefaultWikiPrinter();
            PrintRenderer renderer = new HTMLMacroXHTMLRenderer(printer, 
                this.xhtmlRendererFactory.createXHTMLLinkRenderer(),
                this.xhtmlRendererFactory.createXHTMLImageRenderer());
            xdom.traverse(renderer);

            xhtml = printer.toString();

        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse content [" + content + "] written in [" + wikiSyntax
                + "] syntax.", e);
        }

        return xhtml;
    }
}
