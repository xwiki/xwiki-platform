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

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.renderer.XWikiMacroPrinter;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxLinkRenderer;
import org.xwiki.rendering.internal.renderer.printer.XWikiSyntaxEscapeWikiPrinter;
import org.xwiki.rendering.internal.renderer.state.BlockStateListener;
import org.xwiki.rendering.internal.renderer.state.ConsecutiveNewLineStateListener;
import org.xwiki.rendering.internal.renderer.state.XWikiSyntaxState;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.VoidWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

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
    public XWikiSyntaxRenderer(WikiPrinter printer)
    {
        super(printer, new StackedStateListener<XWikiSyntaxState>(XWikiSyntaxState.class));
    }

    public StackedStateListener<XWikiSyntaxState> getStackedState()
    {
        return (StackedStateListener<XWikiSyntaxState>) getStateListener();
    }

    public XWikiSyntaxState getState()
    {
        return getStackedState().peek();
    }

    private BlockStateListener getBlockStateListener()
    {
        return getState().getBlockStateListener();
    }

    private ConsecutiveNewLineStateListener getConsecutiveNewLineStateListener()
    {
        return getState().getConsecutiveNewLineStateListener();
    }

    private XWikiSyntaxLinkRenderer getLinkRenderer()
    {
        return getState().getLinkRenderer();
    }

    private XWikiSyntaxImageRenderer getImageRenderer()
    {
        return getState().getImageRenderer();
    }

    private XWikiMacroPrinter getMacroPrinter()
    {
        return getState().getMacroPrinter();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginDocument()
     */
    @Override
    public void beginDocument()
    {
        if (getBlockStateListener().isInDocument()) {
            if (!getBlockStateListener().isInLine()) {
                printNewLine();
            }

            print("(((");
        }

        super.beginDocument();

        // Push a new printer for the embedded document
        pushPrinter(new XWikiSyntaxEscapeWikiPrinter(getMainPrinter(), getState()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endDocument()
     */
    @Override
    public void endDocument()
    {
        // Return to parent document printer
        popPrinter();

        super.endDocument();

        if (getBlockStateListener().isInDocument()) {
            print(")))");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginLink(Link, boolean, Map)
     */
    @Override
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        boolean isInLink = getBlockStateListener().isInLink();

        super.beginLink(link, isFreeStandingURI, parameters);

        if (!isInLink) {
            getLinkRenderer().beginRenderLink(getPrinter(), link, isFreeStandingURI, parameters);

            // Defer printing the link content since we need to gather all nested elements
            pushPrinter(new XWikiSyntaxEscapeWikiPrinter(new DefaultWikiPrinter(), getState()));
        } else if (isFreeStandingURI) {
            print(getLinkRenderer().renderLinkReference(link));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endLink(Link, boolean, Map)
     */
    @Override
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // The links in a top level link label are not rendered as link (only the label is printed)
        if (getBlockStateListener().getLinkDepth() == 1) {
            XWikiSyntaxEscapeWikiPrinter linkBlocksPrinter = getXWikiPrinter();
            linkBlocksPrinter.flush();
            String content = linkBlocksPrinter.toString();
            popPrinter();

            getLinkRenderer().renderLinkContent(getPrinter(), content);
            getLinkRenderer().endRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
        }

        super.endLink(link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginFormat(Format, Map)
     */
    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                getXWikiPrinter().printBeginBold();
                break;
            case ITALIC:
                getXWikiPrinter().printBeginItalic();
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
        if (getState().getPreviousFormatParameters() != null
            && !getState().getPreviousFormatParameters().equals(parameters)) {
            getState().setPreviousFormatParameters(null);
            printParameters(parameters, false);
        } else if (getState().getPreviousFormatParameters() == null) {
            getState().setPreviousFormatParameters(null);
            printParameters(parameters, false);
        } else {
            getState().setPreviousFormatParameters(null);
        }

        super.beginFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endFormat(Format, Map)
     */
    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                getXWikiPrinter().printEndItalic();
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
            getState().setPreviousFormatParameters(parameters);
        }

        super.endFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginParagraph(java.util.Map)
     */
    @Override
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
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        getState().setPreviousFormatParameters(null);

        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getXWikiPrinter().flush();

        super.endParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        super.onNewLine();

        // If we're inside a table cell, a paragraph, a list or a section header then if we have already outputted
        // a new line before then this new line should be a line break in order not to break the table cell,
        // paragraph, list or section header.
        if (getConsecutiveNewLineStateListener().getNewLineCount() > 1 && getBlockStateListener().isInLine()) {
            print("\\\\");
        } else {
            print("\n");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onMacro(String, java.util.Map, String, boolean)
     */
    @Override
    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        super.onMacro(name, parameters, content, isInline);

        if (!isInline) {
            printNewLine();
            print(getMacroPrinter().print(name, parameters, content));
        } else {
          getXWikiPrinter().printInlineMacro(getMacroPrinter().print(name, parameters, content));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginHeader(HeaderLevel, Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        super.beginHeader(level, parameters);

        printNewLine();
        printParameters(parameters);
        print(StringUtils.repeat("=", level.getAsInt()) + " ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endHeader(HeaderLevel, Map)
     */
    @Override
    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        print(" " + StringUtils.repeat("=", level.getAsInt()));

        super.endHeader(level, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onWord(String)
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        super.beginList(listType, parameters);

        if (getState().isBeginListItemFound() && !getState().isEndListItemFound()) {
            print("\n");
            getState().setBeginListItemFound(false);
        } else {
            printNewLine();
        }

        if (listType == ListType.BULLETED) {
            getState().getListStyle().append("*");
        } else {
            getState().getListStyle().append("1");
        }
        printParameters(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        super.beginListItem();

        if (getState().isEndListItemFound()) {
            print("\n");
            getState().setEndListItemFound(false);
            getState().setBeginListItemFound(false);
        }

        getState().setBeginListItemFound(true);

        print(getState().getListStyle().toString());
        if (getState().getListStyle().charAt(0) == '1') {
            print(".");
        }
        print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        super.endList(listType, parameters);

        getState().getListStyle().setLength(getState().getListStyle().length() - 1);
        if (!getBlockStateListener().isInList()) {
            getState().setBeginListItemFound(false);
            getState().setEndListItemFound(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endListItem()
     */
    @Override
    public void endListItem()
    {
        getState().setEndListItemFound(true);

        super.endListItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginXMLNode(XMLNode)
     */
    @Override
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
    @Override
    public void endXMLNode(XMLNode node)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML node events.

        super.endXMLNode(node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginMacroMarker(String, java.util.Map, String, boolean)
     */
    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        super.beginMacroMarker(name, parameters, content, isInline);

        // When we encounter a macro marker we ignore all other blocks inside since we're going to use the macro
        // definition wrapped by the macro marker to construct the xwiki syntax.
        getStackedState().push();
        pushPrinter(new XWikiSyntaxEscapeWikiPrinter(VoidWikiPrinter.VOIDWIKIPRINTER, getState()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endMacroMarker(String, java.util.Map, String, boolean)
     */
    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        popPrinter();
        getStackedState().pop();

        if (!isInline) {
            printNewLine();
        }

        print(getMacroPrinter().print(name, parameters, content));

        super.endMacroMarker(name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onId(String)
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void beginDefinitionList()
    {
        super.beginDefinitionList();

        if (getState().isBeginListItemFound() && !getState().isEndListItemFound()) {
            print("\n");
            // - we are inside an existing definition list
        } else if (getState().isBeginDefinitionListItemFound() && !getState().isEndDefinitionListItemFound()) {
            print("\n");
            getState().setBeginDefinitionListItemFound(false);
        } else {
            printNewLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    @Override
    public void endDefinitionList()
    {
        super.endDefinitionList();

        if (!getBlockStateListener().isInDefinitionList()) {
            getState().setBeginDefinitionListItemFound(false);
            getState().setEndDefinitionListItemFound(false);
            getState().setBeginListItemFound(false);
            getState().setEndDefinitionListItemFound(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    @Override
    public void beginDefinitionTerm()
    {
        super.beginDefinitionTerm();

        if (getState().isEndDefinitionListItemFound()) {
            print("\n");
            getState().setEndDefinitionListItemFound(false);
            getState().setBeginDefinitionListItemFound(false);
        }
        getState().setBeginDefinitionListItemFound(true);

        if (getState().getListStyle().length() > 0) {
            print(getState().getListStyle().toString());
            if (getState().getListStyle().charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", getBlockStateListener().getDefinitionListDepth() - 1));
        print("; ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    @Override
    public void beginDefinitionDescription()
    {
        super.beginDefinitionDescription();

        if (getState().isEndDefinitionListItemFound()) {
            print("\n");
            getState().setEndDefinitionListItemFound(false);
            getState().setBeginDefinitionListItemFound(false);
        }
        getState().setBeginDefinitionListItemFound(true);

        if (getState().getListStyle().length() > 0) {
            print(getState().getListStyle().toString());
            if (getState().getListStyle().charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", getBlockStateListener().getDefinitionListDepth() - 1));
        print(": ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    @Override
    public void endDefinitionTerm()
    {
        getState().setEndDefinitionListItemFound(true);

        super.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    @Override
    public void endDefinitionDescription()
    {
        getState().setEndDefinitionListItemFound(true);

        super.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        super.beginQuotation(parameters);

        if (getState().isBeginQuotationLineFound() && !getState().isEndQuotationLineFound()) {
            print("\n");
            getState().setBeginQuotationLineFound(false);
        } else {
            printNewLine();
        }

        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        super.endQuotation(parameters);

        if (!getBlockStateListener().isInQuotation()) {
            getState().setBeginQuotationLineFound(false);
            getState().setEndQuotationLineFound(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    @Override
    public void beginQuotationLine()
    {
        super.beginQuotationLine();

        if (getState().isEndQuotationLineFound()) {
            print("\n");
            getState().setEndQuotationLineFound(false);
            getState().setBeginQuotationLineFound(false);
        }
        getState().setBeginQuotationLineFound(true);

        print(StringUtils.repeat(">", getBlockStateListener().getQuotationDepth()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    @Override
    public void endQuotationLine()
    {
        getState().setEndQuotationLineFound(true);

        super.endQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        super.beginTable(parameters);

        printNewLine();
        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }

        getState().pushEndTableRowFound(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    @Override
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
    @Override
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
    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        super.beginTableRow(parameters);

        if (getState().isEndTableRowFound()) {
            print("\n");
        }

        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    @Override
    public void endTable(Map<String, String> parameters)
    {
        getState().popEndTableRowFound();

        super.endTable(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        getState().setPreviousFormatParameters(null);

        super.endTableCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        getState().setPreviousFormatParameters(null);

        super.endTableHeadCell(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        getState().getIsEndTableRowFoundStack().set(getState().getIsEndTableRowFoundStack().size() - 1, true);

        super.endTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, Map)
     */
    @Override
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super.onImage(image, isFreeStandingURI, parameters);

        Link link = new Link();
        link.setReference("image:" + getImageRenderer().renderImage(image));
        link.setType(LinkType.URI);

        getLinkRenderer().beginRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
        getLinkRenderer().endRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(String, String)
     * @since 1.7M3
     */
    @Override
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
    @Override
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
                String value = entry.getValue();
                // Escape quotes in value to not break parameter value syntax
                value = value.replaceAll("[\\\\\"]", "\\\\$0");
                // Escape ending custom parameters syntax
                value = value.replaceAll("\\%\\)", "~%)");
                buffer.append(' ').append(entry.getKey()).append('=').append('\"').append(value).append('\"');
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
        if (getState().getPreviousFormatParameters() != null) {
            getPrinter().print("(%%)");
            getState().setPreviousFormatParameters(null);
        }

        if (isDelayed) {
            getXWikiPrinter().printDelayed(text);
        } else {
            getPrinter().print(text);
        }
    }

    private void printNewLine()
    {
        if (getState().isFirstElementRendered()) {
            print("\n\n");
        } else {
            getState().setFirstElementRendered(true);
        }
    }

    /**
     * Allows exposing the additional methods of {@link XWikiSyntaxEscapeWikiPrinter}, namely the ability to delay
     * printing some text and the ability to escape characters that would otherwise have a meaning in XWiki syntax.
     */
    public XWikiSyntaxEscapeWikiPrinter getXWikiPrinter()
    {
        return (XWikiSyntaxEscapeWikiPrinter) super.getPrinter();
    }

    @Override
    protected void popPrinter()
    {
        // Ensure that any not printed characters are flushed
        getXWikiPrinter().flush();

        super.popPrinter();
    }
}
