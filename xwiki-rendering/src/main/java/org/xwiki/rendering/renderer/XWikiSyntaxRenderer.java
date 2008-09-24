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
package org.xwiki.rendering.renderer;

import java.util.Map;
import java.util.Stack;

import org.xwiki.rendering.internal.renderer.XWikiMacroPrinter;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Format;
import org.apache.commons.lang.StringUtils;

/**
 * Generates XWiki Syntax from {@link org.xwiki.rendering.block.XDOM}. This is useful for example to convert other wiki
 * syntaxes to the XWiki syntax. It's also useful in our tests to verify that round tripping from XWiki Syntax to the
 * DOM and back to XWiki Syntax generates the same content as the initial syntax.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiSyntaxRenderer extends AbstractPrintRenderer
{
    private boolean isFirstElementRendered = false;
    
    private StringBuffer listStyle = new StringBuffer();

    private boolean isInsideMacroMarker = false;

    private boolean isBeginListItemFound = false;

    private boolean isEndListItemFound = false;

    private boolean isBeginDefinitionListItemFound = false;

    private boolean isEndDefinitionListItemFound = false;

    private boolean isBeginQuotationLineFound = false;

    private boolean isEndQuotationLineFound = false;

    private int listDepth = 0;

    private int definitionListDepth = 0;

    private int quotationDepth = 0;

    private Stack<Boolean> isEndTableRowFoundStack = new Stack<Boolean>();
    
    private XWikiMacroPrinter macroPrinter;

    public XWikiSyntaxRenderer(WikiPrinter printer)
    {
        super(printer);
        this.macroPrinter = new XWikiMacroPrinter();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginDocument()
     */
    public void beginDocument()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endDocument()
     */
    public void endDocument()
    {
        // Don't do anything
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onLink(org.xwiki.rendering.listener.Link)
     */
    public void onLink(Link link, boolean isFreeStandingURI)
    {
        if (!isFreeStandingURI) {
            print("[[");
            if (link.getLabel() != null) {
                print(link.getLabel());
                print(">");
            }
        }
        print(link.getReference());
        if (link.getAnchor() != null) {
            print("#");
            print(link.getAnchor());
        }
        if (link.getQueryString() != null) {
            print("?");
            print(link.getQueryString());
        }
        if (link.getInterWikiAlias() != null) {
            print("@");
            print(link.getInterWikiAlias());
        }
        if (!isFreeStandingURI) {
            if (link.getTarget() != null) {
                print(">");
                print(link.getTarget());
            }
            print("]]");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginFormat(org.xwiki.rendering.listener.Format)
     */
    public void beginFormat(Format format)
    {
        switch (format) {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                print("//");
                break;
            case STRIKEDOUT:
                print("--");
                break;
            case UNDERLINED:
                print("__");
                break;
            case SUPERSCRIPT:
                print("^^");
                break;
            case SUBSCRIPT:
                print(",,");
                break;
            case MONOSPACE:
                print("##");
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endFormat(org.xwiki.rendering.listener.Format)
     */
    public void endFormat(Format format)
    {
        switch (format) {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                print("//");
                break;
            case STRIKEDOUT:
                print("--");
                break;
            case UNDERLINED:
                print("__");
                break;
            case SUPERSCRIPT:
                print("^^");
                break;
            case SUBSCRIPT:
                print(",,");
                break;
            case MONOSPACE:
                print("##");
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        printNewLine();

        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onLineBreak()
     */
    public void onLineBreak()
    {
        print("\n");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onNewLine()
     */
    public void onNewLine()
    {
        print("\\");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onInlineMacro(String, java.util.Map, String)
     */
    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onStandaloneMacro(String, java.util.Map, String)
     */
    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        printNewLine();
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginSection(org.xwiki.rendering.listener.SectionLevel)
     */
    public void beginSection(SectionLevel level)
    {
        printNewLine();
        print(StringUtils.repeat("=", level.getAsInt()) + " ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endSection(org.xwiki.rendering.listener.SectionLevel)
     */
    public void endSection(SectionLevel level)
    {
        print(" " + StringUtils.repeat("=", level.getAsInt()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onWord(String)
     */
    public void onWord(String word)
    {
        print(word);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onSpace()
     */
    public void onSpace()
    {
        print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        print("" + symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onEscape(String)
     */
    public void onEscape(String escapedString)
    {
        for (int i = 0; i < escapedString.length(); i++) {
            print("~" + escapedString.charAt(i));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        if (this.isBeginListItemFound && !this.isEndListItemFound) {
            print("\n");
            this.isBeginListItemFound = false;
        } else {
            printNewLine();
        }

        if (listType == ListType.BULLETED) {
            this.listStyle.append("*");
        } else {
            this.listStyle.append("1");
        }
        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }

        this.listDepth++;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginListItem()
     */
    public void beginListItem()
    {
        if (this.isEndListItemFound) {
            print("\n");
            this.isEndListItemFound = false;
            this.isBeginListItemFound = false;
        }
        
        this.isBeginListItemFound = true;

        print(this.listStyle.toString());
        if (this.listStyle.charAt(0) == '1') {
            print(".");
        }
        print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.listStyle.setLength(this.listStyle.length() - 1);
        this.listDepth--;
        if (this.listDepth == 0) {
            this.isBeginListItemFound = false;
            this.isEndListItemFound = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endListItem()
     */
    public void endListItem()
    {
        this.isEndListItemFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginXMLElement(String, java.util.Map)
     */
    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML element events.
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endXMLElement(String, java.util.Map)
     */
    public void endXMLElement(String name, Map<String, String> attributes)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML element events.
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginMacroMarker(String, java.util.Map, String)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // When we encounter a macro marker we ignore all other blocks inside since we're going to use the macro
        // definition wrapped by the macro marker to construct the xwiki syntax.
        this.isInsideMacroMarker = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endMacroMarker(String, java.util.Map, String)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.isInsideMacroMarker = false;
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onId(String)
     */
    public void onId(String name)
    {
        print("{{id name=\"" + name + "\"}}");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onHorizontalLine()
     */
    public void onHorizontalLine()
    {
        printNewLine();
        print("----");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        print("{{{" + protectedString + "}}}");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimStandalone(String)
     */
    public void onVerbatimStandalone(String protectedString)
    {
        printNewLine();
        onVerbatimInline(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        print(StringUtils.repeat("\n", count));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     * @since 1.6M2
     */
    public void beginDefinitionList()
    {
        if (this.isBeginListItemFound && !this.isEndListItemFound) {
            print("\n");
            // - we are inside an existing definition list
        } else if (this.isBeginDefinitionListItemFound && !this.isEndDefinitionListItemFound) {
            print("\n");
            this.isBeginDefinitionListItemFound = false;
        } else {
            printNewLine();
        }

        this.definitionListDepth++;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    public void endDefinitionList()
    {
        this.definitionListDepth--;
        if (this.definitionListDepth == 0) {
            this.isBeginDefinitionListItemFound = false;
            this.isEndDefinitionListItemFound = false;
            this.isBeginListItemFound = false;
            this.isEndDefinitionListItemFound = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        if (this.isEndDefinitionListItemFound) {
            print("\n");
            this.isEndDefinitionListItemFound = false;
            this.isBeginDefinitionListItemFound = false;
        }
        this.isBeginDefinitionListItemFound = true;

        if (this.listStyle.length() > 0) {
            print(this.listStyle.toString());
            if (this.listStyle.charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", this.definitionListDepth - 1));
        print("; ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        if (this.isEndDefinitionListItemFound) {
            print("\n");
            this.isEndDefinitionListItemFound = false;
            this.isBeginDefinitionListItemFound = false;
        }
        this.isBeginDefinitionListItemFound = true;

        if (this.listStyle.length() > 0) {
            print(this.listStyle.toString());
            if (this.listStyle.charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", this.definitionListDepth - 1));
        print(": ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        this.isEndDefinitionListItemFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        this.isEndDefinitionListItemFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        if (this.isBeginQuotationLineFound && !this.isEndQuotationLineFound) {
            print("\n");
            this.isBeginQuotationLineFound = false;
        } else {
            printNewLine();
        }

        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }

        this.quotationDepth++;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        this.quotationDepth--;
        if (this.quotationDepth == 0) {
            this.isBeginQuotationLineFound = false;
            this.isEndQuotationLineFound = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        if (this.isEndQuotationLineFound) {
            print("\n");
            this.isEndQuotationLineFound = false;
            this.isBeginQuotationLineFound = false;
        }
        this.isBeginQuotationLineFound = true;

        print(StringUtils.repeat(">", this.quotationDepth));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        this.isEndQuotationLineFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        printNewLine();
        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }

        this.isEndTableRowFoundStack.push(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    public void beginTableCell(Map<String, String> parameters)
    {
        print("|");
        if (!parameters.isEmpty()) {
            printParameters(parameters, false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        print("||");
        if (!parameters.isEmpty()) {
            printParameters(parameters, false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        if (this.isEndTableRowFoundStack.peek()) {
            print("\n");
        }

        if (!parameters.isEmpty()) {
            printParameters(parameters, false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        this.isEndTableRowFoundStack.pop();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        this.isEndTableRowFoundStack.set(this.isEndTableRowFoundStack.size() - 1, true);
    }

    protected void printParameters(Map<String, String> parameters)
    {
        printParameters(parameters, true);
    }

    protected void printParameters(Map<String, String> parameters, boolean newLine)
    {
        StringBuffer buffer = new StringBuffer("(%");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            buffer.append(' ').append(entry.getKey()).append('=').append('\"').append(entry.getValue()).append('\"');
        }
        buffer.append(" %)");

        if (newLine) {
            buffer.append("\n");
        }

        print(buffer.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractPrintRenderer#print(java.lang.String)
     */
    @Override
    protected void print(String text)
    {
        if (!this.isInsideMacroMarker) {
            super.print(text);
        }
    }
    
    private void printNewLine()
    {
        if (this.isFirstElementRendered) {
            print("\n\n");
        } else {
            this.isFirstElementRendered = true;
        }
    }
}
