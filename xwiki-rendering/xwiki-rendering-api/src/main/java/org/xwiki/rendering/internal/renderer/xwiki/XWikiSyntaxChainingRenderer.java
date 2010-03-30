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
package org.xwiki.rendering.internal.renderer.xwiki;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.internal.renderer.printer.XWikiSyntaxEscapeWikiPrinter;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.StackableChainingListener;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.LinkReferenceSerializer;
import org.xwiki.rendering.renderer.XWikiSyntaxListenerChain;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.VoidWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Convert listener events to XWiki Syntax 2.0 output.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class XWikiSyntaxChainingRenderer extends AbstractChainingPrintRenderer implements StackableChainingListener
{
    private XWikiSyntaxLinkRenderer linkRenderer;

    private XWikiSyntaxImageRenderer imageRenderer;

    private XWikiSyntaxMacroRenderer macroPrinter;

    private LinkReferenceSerializer linkReferenceSerializer;

    // Custom States

    private boolean isFirstElementRendered = false;

    private StringBuffer listStyle = new StringBuffer();

    private Map<String, String> previousFormatParameters;

    public XWikiSyntaxChainingRenderer(ListenerChain listenerChain, LinkReferenceSerializer linkReferenceSerializer)
    {
        setListenerChain(listenerChain);

        this.linkReferenceSerializer = linkReferenceSerializer;
        this.linkRenderer = new XWikiSyntaxLinkRenderer(getXWikiSyntaxListenerChain(), linkReferenceSerializer);
        this.imageRenderer = new XWikiSyntaxImageRenderer();
        this.macroPrinter = new XWikiSyntaxMacroRenderer();
    }

    // State

    private BlockStateChainingListener getBlockState()
    {
        return getXWikiSyntaxListenerChain().getBlockStateChainingListener();
    }

    /**
     * {@inheritDoc}
     * 
     * @see StackableChainingListener#createChainingListenerInstance()
     */
    public StackableChainingListener createChainingListenerInstance()
    {
        XWikiSyntaxChainingRenderer renderer = new XWikiSyntaxChainingRenderer(getListenerChain(),
            this.linkReferenceSerializer);
        renderer.setPrinter(getPrinter());
        return renderer;
    }

    private XWikiSyntaxListenerChain getXWikiSyntaxListenerChain()
    {
        return (XWikiSyntaxListenerChain) getListenerChain();
    }

    private XWikiSyntaxLinkRenderer getLinkRenderer()
    {
        return this.linkRenderer;
    }

    private XWikiSyntaxImageRenderer getImageRenderer()
    {
        return this.imageRenderer;
    }

    private XWikiSyntaxMacroRenderer getMacroPrinter()
    {
        return this.macroPrinter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginGroup(Map)
     */
    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        if (!getBlockState().isInLine()) {
            printEmptyLine();
        }

        if (parameters.size() > 0) {
            printParameters(parameters, true);
        }

        print("(((");
        print("\n");

        // Create a new listener stack in order to preserve current states, to handle the group.
        getListenerChain().pushAllStackableListeners();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDocument(java.util.Map)
     */
    @Override
    public void endDocument(Map<String, String> parameters)
    {
        // Ensure that all data in the escape printer have been flushed
        getXWikiPrinter().flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endGroup(Map)
     */
    @Override
    public void endGroup(Map<String, String> parameters)
    {
        print("\n");
        print(")))");

        // Restore previous listeners that were stacked
        getListenerChain().popAllStackableListeners();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginLink(org.xwiki.rendering.listener.Link,
     *      boolean, java.util.Map)
     */
    @Override
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Flush test content before the link.
        // TODO: improve the block state renderer to be able to make the difference between what is bufferized
        // before the link and what in the label link
        getXWikiPrinter().setBeforeLink(true);
        getXWikiPrinter().flush();
        getXWikiPrinter().setBeforeLink(false);

        int linkDepth = getBlockState().getLinkDepth();

        // If we are at a depth of 2 or greater it means we're in a link inside a link and in this case we
        // shouldn't output the nested link as a link unless it's a free standing link.
        if (linkDepth < 2) {
            getLinkRenderer().beginRenderLink(getXWikiPrinter(), link, isFreeStandingURI, parameters);

            XWikiSyntaxEscapeWikiPrinter linkLabelPrinter =
                    new XWikiSyntaxEscapeWikiPrinter(new DefaultWikiPrinter(), getXWikiSyntaxListenerChain());

            // Make sure the escape handler knows there is already characters before
            linkLabelPrinter.setOnNewLine(getXWikiPrinter().isOnNewLine());

            // Defer printing the link content since we need to gather all nested elements
            pushPrinter(linkLabelPrinter);
        } else if (isFreeStandingURI) {
            print(getLinkRenderer().serialize(link));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#endLink(org.xwiki.rendering.listener.Link,
     *      boolean, java.util.Map)
     */
    @Override
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // The links in a top level link label are not rendered as link (only the label is printed)
        if (getBlockState().getLinkDepth() == 1) {
            XWikiSyntaxEscapeWikiPrinter linkBlocksPrinter = getXWikiPrinter();
            linkBlocksPrinter.flush();
            String content = linkBlocksPrinter.toString();
            popPrinter();

            getLinkRenderer().renderLinkContent(getXWikiPrinter(), content);
            getLinkRenderer().endRenderLink(getXWikiPrinter(), link, isFreeStandingURI, parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginFormat(org.xwiki.rendering.listener.Format,
     *      java.util.Map)
     */
    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
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
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractChainingPrintRenderer#endFormat(org.xwiki.rendering.listener.Format, java.util.Map)
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
            this.previousFormatParameters = parameters;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        printEmptyLine();
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
        this.previousFormatParameters = null;

        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getXWikiPrinter().flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        // - If we're inside a table cell, a paragraph, a list or a section header then if we have already outputted
        // a new line before then this new line should be a line break in order not to break the table cell,
        // paragraph, list or section header.

        // - If the new line is the last element of the paragraph, list or section header then it should be a line break
        // as otherwise it'll be considered as an empty line event next time the generated syntax is read by the XWiki
        // parser.

        if (getBlockState().isInLine()) {
            if (getXWikiSyntaxListenerChain().getConsecutiveNewLineStateChainingListener().getNewLineCount() > 1) {
                print("\\\\");
            } else if (getXWikiSyntaxListenerChain().getLookaheadChainingListener().getNextEvent().eventType.isInlineEnd()) {
                print("\\\\");
            } else {
                print("\n");
            }
        } else {
            print("\n");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractChainingPrintRenderer#onMacro(String, java.util.Map, String, boolean)
     */
    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean isInline)
    {
        if (!isInline) {
            printEmptyLine();
            print(getMacroPrinter().renderMacro(id, parameters, content, isInline));
        } else {
            getXWikiPrinter().printInlineMacro(getMacroPrinter().renderMacro(id, parameters, content, isInline));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      String, java.util.Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        printEmptyLine();
        printParameters(parameters);
        print(StringUtils.repeat("=", level.getAsInt()) + " ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#endHeader(org.xwiki.rendering.listener.HeaderLevel,
     *      String, java.util.Map)
     */
    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        print(" " + StringUtils.repeat("=", level.getAsInt()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractChainingPrintRenderer#onWord(String)
     */
    @Override
    public void onWord(String word)
    {
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
        printDelayed(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractChainingPrintRenderer#onSpecialSymbol(char)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        printDelayed("" + symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginList(org.xwiki.rendering.listener.ListType,
     *      java.util.Map)
     */
    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        if (getBlockState().getListDepth() == 1) {
            printEmptyLine();
        } else {
            getPrinter().print("\n");
        }

        if (listType == ListType.BULLETED) {
            this.listStyle.append("*");
        } else {
            this.listStyle.append("1");
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
        if (getBlockState().getListItemIndex() > 0) {
            getPrinter().print("\n");
        }

        print(this.listStyle.toString());
        if (StringUtils.contains(this.listStyle.toString(), '1')) {
            print(".");
        }
        print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractChainingPrintRenderer#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.listStyle.setLength(this.listStyle.length() - 1);

        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getXWikiPrinter().flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endListItem()
     */
    @Override
    public void endListItem()
    {
        this.previousFormatParameters = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#beginMacroMarker(String, java.util.Map, String,
     *      boolean)
     */
    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        if (!isInline) {
            printEmptyLine();
        }

        // When we encounter a macro marker we ignore all other blocks inside since we're going to use the macro
        // definition wrapped by the macro marker to construct the xwiki syntax.
        pushPrinter(new XWikiSyntaxEscapeWikiPrinter(VoidWikiPrinter.VOIDWIKIPRINTER, getXWikiSyntaxListenerChain()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractChainingPrintRenderer#endMacroMarker(String, java.util.Map, String, boolean)
     */
    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        this.previousFormatParameters = null;

        popPrinter();

        print(getMacroPrinter().renderMacro(name, parameters, content, isInline));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#onId(String)
     */
    @Override
    public void onId(String name)
    {
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
        printEmptyLine();
        printParameters(parameters);
        print("----");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatim(String, boolean, Map)
     */
    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        if (!isInline) {
            printEmptyLine();
        }
        printParameters(parameters);

        print("{{{");
        getXWikiPrinter().printVerbatimContent(protectedString);
        print("}}}");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        print(StringUtils.repeat("\n", count));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    @Override
    public void beginDefinitionList(Map<String, String> parameters)
    {
        if (getBlockState().getDefinitionListDepth() == 1 && !getBlockState().isInList()) {
            printEmptyLine();
        } else {
            print("\n");
        }
        printParameters(parameters);
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
        if (getBlockState().getDefinitionListItemIndex() > 0) {
            getPrinter().print("\n");
        }

        if (this.listStyle.length() > 0) {
            print(this.listStyle.toString());
            if (this.listStyle.charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", getBlockState().getDefinitionListDepth() - 1));
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
        if (getBlockState().getDefinitionListItemIndex() > 0) {
            getPrinter().print("\n");
        }

        if (this.listStyle.length() > 0) {
            print(this.listStyle.toString());
            if (this.listStyle.charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", getBlockState().getDefinitionListDepth() - 1));
        print(": ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionDescription()
     */
    @Override
    public void endDefinitionDescription()
    {
        this.previousFormatParameters = null;

        getXWikiPrinter().flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDefinitionTerm()
     */
    @Override
    public void endDefinitionTerm()
    {
        this.previousFormatParameters = null;

        getXWikiPrinter().flush();
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
        if (!getBlockState().isInQuotationLine()) {
            printEmptyLine();
        }

        if (!parameters.isEmpty()) {
            printParameters(parameters);
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
        if (getBlockState().getQuotationLineIndex() > 0) {
            getPrinter().print("\n");
        }

        print(StringUtils.repeat(">", getBlockState().getQuotationDepth()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endQuotationLine()
     */
    @Override
    public void endQuotationLine()
    {
        this.previousFormatParameters = null;

        getXWikiPrinter().flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        printEmptyLine();
        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
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
        if (getBlockState().getCellRow() > 0) {
            print("\n");
        }

        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;

        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getXWikiPrinter().flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, Map)
     */
    @Override
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        Link link = new Link();
        link.setReference("image:" + getImageRenderer().renderImage(image));
        link.setType(LinkType.URI);

        getLinkRenderer().beginRenderLink(getXWikiPrinter(), link, isFreeStandingURI, parameters);
        getLinkRenderer().endRenderLink(getXWikiPrinter(), link, isFreeStandingURI, parameters);
    }

    protected void printParameters(Map<String, String> parameters)
    {
        printParameters(parameters, true);
    }

    protected void printParameters(Map<String, String> parameters, boolean newLine)
    {
        StringBuffer parametersStr = new StringBuffer();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = entry.getValue();
            String key = entry.getKey();

            if (key != null && value != null) {
                // Escape quotes in value to not break parameter value syntax
                value = value.replaceAll("[~\"]", "~$0");
                // Escape ending custom parameters syntax
                value = value.replace("%)", "~%)");
                parametersStr.append(' ').append(key).append('=').append('\"').append(value).append('\"');
            }
        }

        if (parametersStr.length() > 0) {
            StringBuffer buffer = new StringBuffer("(%");
            buffer.append(parametersStr);
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
            getXWikiPrinter().printDelayed(text);
        } else {
            getPrinter().print(text);
        }
    }

    private void printEmptyLine()
    {
        if (this.isFirstElementRendered) {
            print("\n\n");
        } else {
            this.isFirstElementRendered = true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#setPrinter(org.xwiki.rendering.renderer.printer.WikiPrinter)
     * @since 2.0M3
     */
    public void setPrinter(WikiPrinter printer)
    {
        // If the printer is already a XWiki Syntax Escape printer don't wrap it again. This case happens when
        // the createChainingListenerInstance() method is called, ie when this renderer's state is stacked
        // (for example when a Group event is being handled).
        if (printer instanceof XWikiSyntaxEscapeWikiPrinter) {
            super.setPrinter(printer);
        } else {
            super.setPrinter(new XWikiSyntaxEscapeWikiPrinter(printer, (XWikiSyntaxListenerChain) getListenerChain()));
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
