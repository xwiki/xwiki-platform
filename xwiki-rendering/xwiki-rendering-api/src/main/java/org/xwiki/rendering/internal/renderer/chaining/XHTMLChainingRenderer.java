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
package org.xwiki.rendering.internal.renderer.chaining;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.rendering.internal.renderer.xhtml.XHTMLMacroRenderer;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.DocumentStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener.Event;
import org.xwiki.rendering.listener.xml.XMLComment;
import org.xwiki.rendering.listener.xml.XMLElement;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.xwiki.rendering.renderer.Renderer;
import org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.MonitoringWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.rendering.renderer.xhtml.XHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLLinkRenderer;

/**
 * Convert listener events to XHTML.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class XHTMLChainingRenderer extends AbstractChainingPrintRenderer
{
    private static final String[][] DOCUMENT_DIV_ATTRIBUTES = new String[][] {{"class", "xwiki-document"}};

    private XHTMLLinkRenderer linkRenderer;

    private XHTMLImageRenderer imageRenderer;

    private XHTMLMacroRenderer macroRenderer;

    private XHTMLWikiPrinter xhtmlWikiPrinter;

    /**
     * @param printer the object to which to write the XHTML output to
     * @param documentAccessBridge see {@link #documentAccessBridge}
     * @param configuration the rendering configuration
     */
    public XHTMLChainingRenderer(WikiPrinter printer, XHTMLLinkRenderer linkRenderer, XHTMLImageRenderer imageRenderer,
        ListenerChain listenerChain)
    {
        super(printer, listenerChain);

        this.linkRenderer = linkRenderer;
        this.imageRenderer = imageRenderer;
        this.macroRenderer = new XHTMLMacroRenderer();
        this.xhtmlWikiPrinter = new XHTMLWikiPrinter(printer);
    }

    // State

    private DocumentStateChainingListener getDocumentState()
    {
        return (DocumentStateChainingListener) getListenerChain().getListener(DocumentStateChainingListener.class);
    }

    private BlockStateChainingListener getBlockState()
    {
        return (BlockStateChainingListener) getListenerChain().getListener(BlockStateChainingListener.class);
    }

    // Printer

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#pushPrinter(org.xwiki.rendering.renderer.printer.WikiPrinter)
     */
    @Override
    protected void pushPrinter(WikiPrinter wikiPrinter)
    {
        super.pushPrinter(wikiPrinter);

        if (this.xhtmlWikiPrinter != null) {
            this.xhtmlWikiPrinter.setWikiPrinter(getPrinter());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.chaining.AbstractChainingPrintRenderer#popPrinter()
     */
    @Override
    protected void popPrinter()
    {
        super.popPrinter();

        if (this.xhtmlWikiPrinter != null) {
            this.xhtmlWikiPrinter.setWikiPrinter(getPrinter());
        }
    }

    protected XHTMLWikiPrinter getXHTMLWikiPrinter()
    {
        return this.xhtmlWikiPrinter;
    }

    // Events

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginDocument()
     */
    @Override
    public void beginDocument()
    {
        if (getDocumentState().getDocumentDepth() > 1) {
            getXHTMLWikiPrinter().printXMLStartElement("div", DOCUMENT_DIV_ATTRIBUTES);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endDocument()
     */
    @Override
    public void endDocument()
    {
        if (getDocumentState().getDocumentDepth() > 1) {
            getXHTMLWikiPrinter().printXMLEndElement("div");
        }
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
                getXHTMLWikiPrinter().printXMLStartElement("strong");
                break;
            case ITALIC:
                getXHTMLWikiPrinter().printXMLStartElement("em");
                break;
            case STRIKEDOUT:
                getXHTMLWikiPrinter().printXMLStartElement("del");
                break;
            case UNDERLINED:
                getXHTMLWikiPrinter().printXMLStartElement("ins");
                break;
            case SUPERSCRIPT:
                getXHTMLWikiPrinter().printXMLStartElement("sup");
                break;
            case SUBSCRIPT:
                getXHTMLWikiPrinter().printXMLStartElement("sub");
                break;
            case MONOSPACE:
                getXHTMLWikiPrinter().printXMLStartElement("tt");
                break;
        }
        if (!parameters.isEmpty()) {
            getXHTMLWikiPrinter().printXMLStartElement("span", parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endFormat(Format, Map)
     */
    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        if (!parameters.isEmpty()) {
            getXHTMLWikiPrinter().printXMLEndElement("span");
        }
        switch (format) {
            case BOLD:
                getXHTMLWikiPrinter().printXMLEndElement("strong");
                break;
            case ITALIC:
                getXHTMLWikiPrinter().printXMLEndElement("em");
                break;
            case STRIKEDOUT:
                getXHTMLWikiPrinter().printXMLEndElement("del");
                break;
            case UNDERLINED:
                getXHTMLWikiPrinter().printXMLEndElement("ins");
                break;
            case SUPERSCRIPT:
                getXHTMLWikiPrinter().printXMLEndElement("sup");
                break;
            case SUBSCRIPT:
                getXHTMLWikiPrinter().printXMLEndElement("sub");
                break;
            case MONOSPACE:
                getXHTMLWikiPrinter().printXMLEndElement("tt");
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLStartElement("p", parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endParagraph(java.util.Map)
     */
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLEndElement("p");
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        getXHTMLWikiPrinter().printXMLElement("br");
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginLink(Link, boolean, Map)
     */
    @Override
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Ensure the link renderer is using the latest printer since the original printer used could have been
        // superseded by another one in the printer stack.
        this.linkRenderer.setXHTMLWikiPrinter(getXHTMLWikiPrinter());

        this.linkRenderer.beginLink(link, isFreeStandingURI, parameters);
        pushPrinter(new MonitoringWikiPrinter(getPrinter()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endLink(Link, boolean, Map)
     */
    @Override
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        MonitoringWikiPrinter printer = (MonitoringWikiPrinter) getPrinter();
        popPrinter();
        this.linkRenderer.setHasLabel(printer.hasContentBeenPrinted());
        this.linkRenderer.endLink(link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onMacro(String, java.util.Map, String, boolean)
     */
    @Override
    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        // Do not do any rendering but we still need to save the macro definition in some hidden XHTML
        // so that the macro can be reconstructed when moving back from XHTML to XDOM.
        this.macroRenderer.render(getXHTMLWikiPrinter(), name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginHeader(HeaderLevel, String, Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        Map<String, String> attributes = new LinkedHashMap<String, String>();

        attributes.put("id", id);
        attributes.putAll(parameters);

        getXHTMLWikiPrinter().printXMLStartElement("h" + level.getAsInt(), attributes);
        // We generate a span so that CSS rules have a hook to perform some magic that wouldn't work on just a H
        // element. Like some IE6 magic and others.
        getXHTMLWikiPrinter().printXMLStartElement("span");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endHeader(HeaderLevel, String, Map)
     */
    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLEndElement("span");
        getXHTMLWikiPrinter().printXMLEndElement("h" + level.getAsInt());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onWord(String)
     */
    @Override
    public void onWord(String word)
    {
        getXHTMLWikiPrinter().printXML(word);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onSpace()
     */
    @Override
    public void onSpace()
    {
        // The XHTML printer will decide whether to print a normal space or a &nbsp;
        getXHTMLWikiPrinter().printSpace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onSpecialSymbol(String)
     */
    @Override
    public void onSpecialSymbol(char symbol)
    {
        getXHTMLWikiPrinter().printXML("" + symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        if (listType == ListType.BULLETED) {
            getXHTMLWikiPrinter().printXMLStartElement("ul", parameters);
        } else {
            getXHTMLWikiPrinter().printXMLStartElement("ol", parameters);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginListItem()
     */
    @Override
    public void beginListItem()
    {
        getXHTMLWikiPrinter().printXMLStartElement("li");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        if (listType == ListType.BULLETED) {
            getXHTMLWikiPrinter().printXMLEndElement("ul");
        } else {
            getXHTMLWikiPrinter().printXMLEndElement("ol");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endListItem()
     */
    @Override
    public void endListItem()
    {
        getXHTMLWikiPrinter().printXMLEndElement("li");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginXMLNode(XMLNode)
     */
    @Override
    public void beginXMLNode(XMLNode node)
    {
        switch (node.getNodeType()) {
            case CDATA:
                getXHTMLWikiPrinter().printXMLStartCData();
                break;
            case COMMENT:
                XMLComment commentNode = (XMLComment) node;
                getXHTMLWikiPrinter().printXMLComment(commentNode.getComment());
                break;
            case ELEMENT:
                XMLElement elementNode = (XMLElement) node;
                getXHTMLWikiPrinter().printXMLStartElement(elementNode.getName(), elementNode.getAttributes());
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endXMLNode(XMLNode)
     */
    @Override
    public void endXMLNode(XMLNode node)
    {
        switch (node.getNodeType()) {
            case CDATA:
                getXHTMLWikiPrinter().printXMLEndCData();
                break;
            case ELEMENT:
                XMLElement elementNode = (XMLElement) node;
                getXHTMLWikiPrinter().printXMLEndElement(elementNode.getName());
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginMacroMarker(String, java.util.Map, String, boolean)
     */
    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        if (getBlockState().getMacroDepth() == 1) {
            // Do not do any rendering but we still need to save the macro definition in some hidden XHTML
            // so that the macro can be reconstructed when moving back from XHTML to XDOM.
            this.macroRenderer.beginRender(getXHTMLWikiPrinter(), name, parameters, content);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endMacroMarker(String, java.util.Map, String, boolean)
     */
    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        if (getBlockState().getMacroDepth() == 1) {
            // Do not do any rendering but we still need to save the macro definition in some hidden XHTML
            // so that the macro can be reconstructed when moving back from XHTML to XDOM.
            this.macroRenderer.endRender(getXHTMLWikiPrinter());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onId(String)
     */
    @Override
    public void onId(String name)
    {
        // Note: We're using <a><a/> and not <a/> since some browsers do not support the <a/> syntax (FF3)
        // when the content type is set to HTML instead of XHTML.
        getXHTMLWikiPrinter().printXMLStartElement("a", new String[][] { {"id", name}, {"name", name}});
        getXHTMLWikiPrinter().printXMLEndElement("a");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onHorizontalLine(Map)
     */
    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLElement("hr", parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatim(String, Map, boolean)
     */
    @Override
    public void onVerbatim(String protectedString, Map<String, String> parameters, boolean isInline)
    {
        if (isInline) {
            // Note: We generate a tt element rather than a pre element since pre elements cannot be located inside
            // paragraphs for example. There also no tag in XHTML that has a semantic of preserving inline content so
            // tt is the closed to pre for inline.
            // The class is what is expected by wikimodel to understand the tt as meaning a verbatim and not a Monospace
            // element.
            getXHTMLWikiPrinter().printXMLStartElement("tt", new String[][] {{"class", "wikimodel-verbatim"}});
            getXHTMLWikiPrinter().printXML(protectedString);
            getXHTMLWikiPrinter().printXMLEndElement("tt");
        } else {
            getXHTMLWikiPrinter().printXMLStartElement("pre", parameters);
            getXHTMLWikiPrinter().printXML(protectedString);
            getXHTMLWikiPrinter().printXMLEndElement("pre");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onEmptyLines(int)
     */
    @Override
    public void onEmptyLines(int count)
    {
        // We need to use a special tag for empty lines since in XHTML the BR tag cannot be used outside of content
        // tags.
        // Note: We're using <div><div/> and not <div/> since some browsers do not support the <div/> syntax (FF3)
        // when the content type is set to HTML instead of XHTML.
        for (int i = 0; i < count; ++i) {
            getXHTMLWikiPrinter().printXMLStartElement("div", new String[][] {{"class", "wikimodel-emptyline"}});
            getXHTMLWikiPrinter().printXMLEndElement("div");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     */
    @Override
    public void beginDefinitionList()
    {
        getXHTMLWikiPrinter().printXMLStartElement("dl");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     */
    @Override
    public void endDefinitionList()
    {
        getXHTMLWikiPrinter().printXMLEndElement("dl");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        getXHTMLWikiPrinter().printXMLStartElement("dt");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     */
    @Override
    public void beginDefinitionDescription()
    {
        getXHTMLWikiPrinter().printXMLStartElement("dd");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     */
    @Override
    public void endDefinitionTerm()
    {
        getXHTMLWikiPrinter().printXMLEndElement("dt");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     */
    @Override
    public void endDefinitionDescription()
    {
        getXHTMLWikiPrinter().printXMLEndElement("dd");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     */
    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLStartElement("blockquote", parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     */
    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLEndElement("blockquote");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        // Send a new line if the previous event was endQuotationLine since we need to separate each quotation line
        // or they'll printed next to each other and not on a new line each.
        if (getBlockState().isInQuotation() && getBlockState().getPreviousEvent() == Event.QUOTATION_LINE) {
            onNewLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    @Override
    public void beginTable(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLStartElement("table", parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLStartElement("tr", parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLStartElement("td", parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLStartElement("th", parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    @Override
    public void endTable(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLEndElement("table");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLEndElement("tr");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLEndElement("td");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        getXHTMLWikiPrinter().printXMLEndElement("th");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, Map)
     */
    @Override
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Ensure the image renderer is using the latest printer since the original printer used could have been
        // superseded by another one in the printer stack.
        this.imageRenderer.setXHTMLWikiPrinter(getXHTMLWikiPrinter());
        this.imageRenderer.onImage(image, isFreeStandingURI, parameters);
    }
}
