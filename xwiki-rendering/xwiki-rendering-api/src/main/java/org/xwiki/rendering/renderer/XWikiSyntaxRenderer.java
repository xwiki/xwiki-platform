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

import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.renderer.XWikiMacroPrinter;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxLinkRenderer;
import org.xwiki.rendering.internal.renderer.printer.XWikiSyntaxEscapeWikiPrinter;
import org.xwiki.rendering.internal.renderer.state.ConsecutiveNewLineStateListener;
import org.xwiki.rendering.internal.renderer.state.BlockStateListener;
import org.xwiki.rendering.internal.renderer.state.TextOnNewLineStateListener;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.VoidWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.apache.commons.lang.StringUtils;

/**
 * Generates XWiki Syntax from {@link XDOM}. This is useful for example to convert other wiki syntaxes to the XWiki
 * syntax. It's also useful in our tests to verify that round tripping from XWiki Syntax to the DOM and back to XWiki
 * Syntax generates the same content as the initial syntax.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiSyntaxRenderer extends AbstractPrintRenderer
{
    private XWikiSyntaxLinkRenderer linkRenderer;

    private XWikiSyntaxImageRenderer imageRenderer;

    private boolean isFirstElementRendered = false;

    private StringBuffer listStyle = new StringBuffer();

    private boolean isBeginListItemFound = false;

    private boolean isEndListItemFound = false;

    private boolean isBeginDefinitionListItemFound = false;

    private boolean isEndDefinitionListItemFound = false;

    private boolean isBeginQuotationLineFound = false;

    private boolean isEndQuotationLineFound = false;

    private boolean escapeIfSpace = false;

    private int listDepth = 0;

    private int definitionListDepth = 0;

    private int quotationDepth = 0;

    private Stack<Boolean> isEndTableRowFoundStack = new Stack<Boolean>();

    private XWikiMacroPrinter macroPrinter;

    private XWikiSyntaxEscapeWikiPrinter linkBlocksPrinter;

    private Map<String, String> previousFormatParameters;

    private Map<String, String> previousFormatParametersBeforeMacroMarker;

    private BlockStateListener blockListener = new BlockStateListener();

    private TextOnNewLineStateListener textListener = new TextOnNewLineStateListener();

    private ConsecutiveNewLineStateListener newLineListener = new ConsecutiveNewLineStateListener();

    public XWikiSyntaxRenderer(WikiPrinter printer)
    {
        super(new XWikiSyntaxEscapeWikiPrinter(printer));

        getPrinter().setBlockListener(this.blockListener);
        getPrinter().setTextListener(this.textListener);

        registerStateListener(this.blockListener);
        registerStateListener(this.textListener);
        registerStateListener(this.newLineListener);

        this.macroPrinter = new XWikiMacroPrinter();
        this.linkRenderer = new XWikiSyntaxLinkRenderer();
        this.imageRenderer = new XWikiSyntaxImageRenderer();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginDocument()
     */
    public void beginDocument()
    {
        super.beginDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endDocument()
     */
    public void endDocument()
    {
        // Ensure that any not printed characters are flushed
        getPrinter().flush();

        super.endDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginLink(Link, boolean, Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        boolean isInLink = this.blockListener.isInLink();

        super.beginLink(link, isFreeStandingURI, parameters);

        if (!isInLink) {
            this.linkRenderer.beginRenderLink(getPrinter(), link, isFreeStandingURI, parameters);

            // Defer printing the link content since we need to gather all nested elements
            this.linkBlocksPrinter =
                new XWikiSyntaxEscapeWikiPrinter(new DefaultWikiPrinter(), this.blockListener, this.textListener);
            pushPrinter(this.linkBlocksPrinter);
        } else if (isFreeStandingURI) {
            print(this.linkRenderer.renderLinkReference(link));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endLink(Link, boolean, Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // The links in a top level link label are not rendered as link (only the label is printed)
        if (this.blockListener.getLinkDepth() == 1) {
            this.linkBlocksPrinter.flush();
            String content = this.linkBlocksPrinter.toString();
            popPrinter();

            this.linkRenderer.renderLinkContent(getPrinter(), content);
            this.linkRenderer.endRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
        }

        super.endLink(link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginFormat(Format, Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                getPrinter().printBeginBold();
                break;
            case ITALIC:
                getPrinter().printBeginItalic();
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
        // If the previous format had parameters and the parameters are different from the current ones then close them
        if (this.previousFormatParameters != null && !this.previousFormatParameters.equals(parameters)) {
            this.previousFormatParameters = null;
            printParameters(parameters, false);
        } else if (this.previousFormatParameters == null) {
            this.previousFormatParameters = null;
            printParameters(parameters, false);
        } else {
            this.previousFormatParameters = null;
        }

        super.beginFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endFormat(Format, Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                getPrinter().printEndItalic();
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
        if (!parameters.isEmpty()) {
            this.previousFormatParameters = parameters;
        }

        super.endFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        super.beginParagraph(parameters);

        printNewLine();
        printParameters(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;

        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getPrinter().flush();

        super.endParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onNewLine()
     */
    public void onNewLine()
    {
        super.onNewLine();

        // If we're inside a table cell, a paragraph, a list or a section header then if we have already outputted
        // a new line before then this new line should be a line break in order not to break the table cell,
        // paragraph, list or section header.
        if (this.newLineListener.getNewLineCount() > 1
            && (this.blockListener.isInParagraph() || this.blockListener.isInList()
                || this.blockListener.isInDefinitionList() || this.blockListener.isInSection()
                || this.blockListener.isInTableCell() || this.blockListener.isInQuotationLine())) {
            print("\\\\");
        } else {
            print("\n");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onInlineMacro(String, java.util.Map, String)
     */
    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        super.onInlineMacro(name, parameters, content);
        getPrinter().printInlineMacro(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onStandaloneMacro(String, java.util.Map, String)
     */
    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        super.onStandaloneMacro(name, parameters, content);
        printNewLine();
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginSection(SectionLevel, Map)
     */
    public void beginSection(SectionLevel level, Map<String, String> parameters)
    {
        super.beginSection(level, parameters);
        printNewLine();
        printParameters(parameters);
        print(StringUtils.repeat("=", level.getAsInt()) + " ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endSection(SectionLevel, Map)
     */
    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        print(" " + StringUtils.repeat("=", level.getAsInt()));
        super.endSection(level, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onWord(String)
     */
    public void onWord(String word)
    {
        super.onWord(word);
        printDelayed(word);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onSpace()
     */
    public void onSpace()
    {
        super.onSpace();
        printDelayed(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        super.onSpecialSymbol(symbol);
        printDelayed("" + symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        super.beginList(listType, parameters);

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
        printParameters(parameters);

        this.listDepth++;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginListItem()
     */
    public void beginListItem()
    {
        super.beginListItem();

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

        super.endList(listType, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endListItem()
     */
    public void endListItem()
    {
        this.isEndListItemFound = true;

        super.endListItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginXMLNode(XMLNode)
     */
    public void beginXMLNode(XMLNode node)
    {
        super.beginXMLNode(node);

        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML node events.
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endXMLNode(XMLNode)
     */
    public void endXMLNode(XMLNode node)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML node events.

        super.endXMLNode(node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginMacroMarker(String, java.util.Map, String)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        super.beginMacroMarker(name, parameters, content);

        this.previousFormatParametersBeforeMacroMarker = this.previousFormatParameters;

        // When we encounter a macro marker we ignore all other blocks inside since we're going to use the macro
        // definition wrapped by the macro marker to construct the xwiki syntax.
        pushPrinter(new XWikiSyntaxEscapeWikiPrinter(VoidWikiPrinter.VOIDWIKIPRINTER, this.blockListener,
            this.textListener));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endMacroMarker(String, java.util.Map, String)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        popPrinter();

        this.previousFormatParameters = this.previousFormatParametersBeforeMacroMarker;

        print(this.macroPrinter.print(name, parameters, content));

        super.endMacroMarker(name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onId(String)
     */
    public void onId(String name)
    {
        super.onId(name);
        print("{{id name=\"" + name + "\"}}");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        super.onHorizontalLine(parameters);
        printNewLine();
        printParameters(parameters);
        print("----");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        super.onVerbatimInline(protectedString);
        print("{{{" + protectedString + "}}}");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimStandalone(String, Map)
     */
    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        super.onVerbatimStandalone(protectedString, parameters);
        printNewLine();
        printParameters(parameters);
        onVerbatimInline(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        super.onEmptyLines(count);
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
        super.beginDefinitionList();

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

        super.endDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        super.beginDefinitionTerm();

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
        super.beginDefinitionDescription();

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

        super.endDefinitionTerm();
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

        super.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        super.beginQuotation(parameters);

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

        super.endQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        super.beginQuotationLine();

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

        super.endQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        super.beginTable(parameters);

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
        super.beginTableCell(parameters);

        print("|");
        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        super.beginTableHeadCell(parameters);

        print("|=");
        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        super.beginTableRow(parameters);

        if (this.isEndTableRowFoundStack.peek()) {
            print("\n");
        }

        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        this.isEndTableRowFoundStack.pop();

        super.endTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;

        super.endTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;

        super.endTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        this.isEndTableRowFoundStack.set(this.isEndTableRowFoundStack.size() - 1, true);

        super.endTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super.onImage(image, isFreeStandingURI, parameters);

        Link link = new Link();
        link.setReference("image:" + this.imageRenderer.renderImage(image));
        link.setType(LinkType.URI);

        this.linkRenderer.beginRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
        this.linkRenderer.endRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(String, String)
     * @since 1.7M3
     */
    public void beginError(String message, String description)
    {
        super.beginError(message, description);

        // Don't do anything since we don't want errors to be visible in XWiki syntax.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endError(String, String)
     * @since 1.7M3
     */
    public void endError(String message, String description)
    {
        // Don't do anything since we don't want errors to be visible in XWiki syntax.

        super.endError(message, description);
    }

    protected void printParameters(Map<String, String> parameters)
    {
        printParameters(parameters, true);
    }

    protected void printParameters(Map<String, String> parameters, boolean newLine)
    {
        if (!parameters.isEmpty()) {
            StringBuffer buffer = new StringBuffer("(%");
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                buffer.append(' ').append(entry.getKey()).append('=').append('\"').append(
                    entry.getValue().replaceAll("[\\\\\"]", "\\\\$0").replaceAll("\\%\\)", "~%)")).append('\"');
            }
            buffer.append(" %)");

            if (newLine) {
                buffer.append("\n");
            }

            print(buffer.toString());
        }
    }

    private void printDelayed(String text)
    {
        print(text, true);
    }

    private void print(String text)
    {
        print(text, false);
    }

    private void print(String text, boolean isDelayed)
    {
        // Handle empty formatting parameters.
        if (this.previousFormatParameters != null) {
            getPrinter().print("(%%)");
            this.previousFormatParameters = null;
        }

        if (isDelayed) {
            getPrinter().printDelayed(text);
        } else {
            getPrinter().print(text);
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

    /**
     * Allows exposing the additional methods of {@link XWikiSyntaxEscapeWikiPrinter}, namely the ability to delay
     * printing some text and the ability to escape characters that would otherwise have a meaning in XWiki syntax.
     */
    public XWikiSyntaxEscapeWikiPrinter getPrinter()
    {
        return (XWikiSyntaxEscapeWikiPrinter) super.getPrinter();
    }
}
