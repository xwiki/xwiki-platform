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

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.internal.renderer.state.BlockStateListener;
import org.xwiki.rendering.internal.renderer.state.BlockStateListener.Event;
import org.xwiki.rendering.internal.renderer.xhtml.XHTMLIdGenerator;
import org.xwiki.rendering.internal.renderer.xhtml.XHTMLLinkRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.XHTMLMacroRenderer;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.ImageType;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.listener.xml.XMLComment;
import org.xwiki.rendering.listener.xml.XMLElement;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.MonitoringWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Generates XHTML from a {@link org.xwiki.rendering.block.XDOM} object being traversed.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLRenderer extends AbstractPrintRenderer
{
    private static final String[][] DOCUMENT_DIV_ATTRIBUTES = new String[][] {{"class", "xwiki-document"}};

    /**
     * A temporary service offering methods manipulating XWiki Documents that are needed to output the correct XHTML.
     * For example this is used to verify if a document exists when computing the HREF attribute for a link. It's
     * temporary because the current Document services have not yet been rewritten with the new architecture. This
     * bridge allows us to be independent of the XWiki Core module, thus preventing a cyclic dependency.
     */
    private DocumentAccessBridge documentAccessBridge;

    private XHTMLLinkRenderer linkRenderer;

    private XHTMLMacroRenderer macroRenderer;

    private XHTMLIdGenerator idGenerator;

    private XHTMLWikiPrinter xhtmlWikiPrinter;

    private XWikiSyntaxImageRenderer imageRenderer;

    /**
     * The temporary Printer used to redirect all outputs when computing the header title.
     * 
     * @see #originalPrinter
     */
    private WikiPrinter headerTitlePrinter;

    /**
     * @param printer the object to which to write the XHTML output to
     * @param documentAccessBridge see {@link #documentAccessBridge}
     * @param configuration the rendering configuration
     */
    public XHTMLRenderer(WikiPrinter printer, DocumentAccessBridge documentAccessBridge,
        RenderingConfiguration configuration, AttachmentParser attachmentParser)
    {
        super(printer, new BlockStateListener());

        this.documentAccessBridge = documentAccessBridge;
        this.linkRenderer = new XHTMLLinkRenderer(documentAccessBridge, configuration, attachmentParser);
        this.macroRenderer = new XHTMLMacroRenderer();
        this.xhtmlWikiPrinter = new XHTMLWikiPrinter(printer);
        this.imageRenderer = new XWikiSyntaxImageRenderer();
        this.idGenerator = new XHTMLIdGenerator();
    }

    // State

    public BlockStateListener getState()
    {
        return (BlockStateListener) getStateListener();
    }

    // Printer

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractPrintRenderer#pushPrinter(org.xwiki.rendering.renderer.printer.WikiPrinter)
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
     * @see org.xwiki.rendering.renderer.AbstractPrintRenderer#popPrinter()
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
        if (getState().isInDocument()) {
            getXHTMLWikiPrinter().printXMLStartElement("div", DOCUMENT_DIV_ATTRIBUTES);
        }

        super.beginDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endDocument()
     */
    @Override
    public void endDocument()
    {
        super.endDocument();

        if (getState().isInDocument()) {
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
        super.beginFormat(format, parameters);
        
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
        
        super.endFormat(format, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        super.beginParagraph(parameters);
        
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
        
        super.endParagraph(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#onNewLine()
     */
    @Override
    public void onNewLine()
    {
        super.onNewLine();
        
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
        super.beginLink(link, isFreeStandingURI, parameters);
        
        this.linkRenderer.beginRender(getXHTMLWikiPrinter(), link, isFreeStandingURI, parameters);
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
        this.linkRenderer.endRender(getXHTMLWikiPrinter(), link, isFreeStandingURI, !printer.hasContentBeenPrinted());
        
        super.endLink(link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onMacro(String, java.util.Map, String, boolean)
     */
    @Override
    public void onMacro(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        super.onMacro(name, parameters, content, isInline);
        
        // Do not do any rendering but we still need to save the macro definition in some hidden XHTML
        // so that the macro can be reconstructed when moving back from XHTML to XDOM.
        this.macroRenderer.render(getXHTMLWikiPrinter(), name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginHeader(HeaderLevel, Map)
     */
    @Override
    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        super.beginHeader(level, parameters);
        
        // Don't output anything yet since we need the header title to generate the unique XHTML id attribute.
        // Thus we're doing the output in the #endHeader() event.

        // Redirect all output to our writer
        this.headerTitlePrinter = new DefaultWikiPrinter();
        pushPrinter(this.headerTitlePrinter);
    }

    private void processBeginHeader(HeaderLevel level, String headerTitle, Map<String, String> parameters)
    {
        Map<String, String> attributes = new LinkedHashMap<String, String>();

        attributes.put("id", this.idGenerator.generateUniqueId(headerTitle));
        attributes.putAll(parameters);

        int levelAsInt = level.getAsInt();
        getXHTMLWikiPrinter().printXMLStartElement("h" + levelAsInt, attributes);
        // We generate a span so that CSS rules have a hook to perform some magic that wouldn't work on just a H
        // element. Like some IE6 magic and others.
        getXHTMLWikiPrinter().printXMLStartElement("span");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endHeader(HeaderLevel, Map)
     */
    @Override
    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        String headerTitle = this.headerTitlePrinter.toString();
        popPrinter();
        processBeginHeader(level, headerTitle, parameters);
        getPrinter().print(headerTitle);

        int levelAsInt = level.getAsInt();
        getXHTMLWikiPrinter().printXMLEndElement("span");
        getXHTMLWikiPrinter().printXMLEndElement("h" + levelAsInt);
        
        super.endHeader(level, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onWord(String)
     */
    @Override
    public void onWord(String word)
    {
        super.onWord(word);
        
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
        super.onSpace();
        
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
        super.onSpecialSymbol(symbol);
        
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
        super.beginList(listType, parameters);
        
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
        super.beginListItem();
        
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
        
        super.endList(listType, parameters);
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
                XMLElement element = (XMLElement) node;
                getXHTMLWikiPrinter().printXMLEndElement(element.getName());
                break;
        }
        
        super.endXMLNode(node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginMacroMarker(String, java.util.Map, String, boolean)
     */
    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        super.beginMacroMarker(name, parameters, content, isInline);
        
        // Do not do any rendering but we still need to save the macro definition in some hidden XHTML
        // so that the macro can be reconstructed when moving back from XHTML to XDOM.
        this.macroRenderer.beginRender(getXHTMLWikiPrinter(), name, parameters, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endMacroMarker(String, java.util.Map, String, boolean)
     */
    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        // Do not do any rendering but we still need to save the macro definition in some hidden XHTML
        // so that the macro can be reconstructed when moving back from XHTML to XDOM.
        this.macroRenderer.endRender(getXHTMLWikiPrinter());
        
        super.endMacroMarker(name, parameters, content, isInline);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onId(String)
     */
    @Override
    public void onId(String name)
    {
        super.onId(name);
        
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
        super.onHorizontalLine(parameters);
        
        getXHTMLWikiPrinter().printXMLElement("hr", parameters);
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
        
        // Note: We generate a tt element rather than a pre element since pre elements cannot be located inside
        // paragraphs for example. There also no tag in XHTML that has a semantic of preserving inline content so
        // tt is the closed to pre for inline.
        // The class is what is expected by wikimodel to understand the tt as meaning a verbatim and not a Monospace
        // element.
        getXHTMLWikiPrinter().printXMLStartElement("tt", new String[][] {{"class", "wikimodel-verbatim"}});
        getXHTMLWikiPrinter().printXML(protectedString);
        getXHTMLWikiPrinter().printXMLEndElement("tt");
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
        
        getXHTMLWikiPrinter().printXMLStartElement("pre", parameters);
        getXHTMLWikiPrinter().printXML(protectedString);
        getXHTMLWikiPrinter().printXMLEndElement("pre");
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
        super.beginDefinitionList();
        
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
        
        super.endDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     */
    @Override
    public void beginDefinitionTerm()
    {
        super.beginDefinitionTerm();
        
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
        super.beginDefinitionDescription();
        
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
        
        super.endDefinitionTerm();
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
        
        super.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     */
    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        super.beginQuotation(parameters);

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

        super.endQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     */
    @Override
    public void beginQuotationLine()
    {
        super.beginQuotationLine();
        
        // Send a new line if the previous event was endQuotationLine since we need to separate each quotation line
        // or they'll printed next to each other and not on a new line each.
        if (getState().isInQuotation() && getState().getPreviousEvent() == Event.QUOTATION_LINE) {
            onNewLine();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     */
    @Override
    public void endQuotationLine()
    {
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
        super.beginTableRow(parameters);
        
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
        super.beginTableCell(parameters);
        
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
        super.beginTableHeadCell(parameters);
        
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
        
        super.endTable(parameters);
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
        
        super.endTableRow(parameters);
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
        getXHTMLWikiPrinter().printXMLEndElement("th");
        
        super.endTableHeadCell(parameters);
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
        
        // First we need to compute the image URL.
        String imageURL;
        if (image.getType() == ImageType.DOCUMENT) {
            DocumentImage documentImage = (DocumentImage) image;
            try {
                imageURL =
                    this.documentAccessBridge.getAttachmentURL(documentImage.getDocumentName(), documentImage
                        .getAttachmentName());
            } catch (Exception e) {
                // TODO: Handle exceptions in a better manner
                throw new RuntimeException("Failed to get attachment URL for [" + image + "]", e);
            }
        } else {
            URLImage urlImage = (URLImage) image;
            imageURL = urlImage.getURL();
        }

        // Then add it as an attribute of the IMG element.
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("src", imageURL);

        // Add the class if we're on a freestanding uri
        if (isFreeStandingURI) {
            attributes.put("class", "wikimodel-freestanding");
        }

        // Add the other parameters as attributes
        attributes.putAll(parameters);

        // If not ALT attribute has been specified, add it since the XHTML specifications makes it mandatory.
        if (!parameters.containsKey("alt")) {
            attributes.put("alt", image.getName());
        }

        // And generate the XHTML IMG element. We need to save the image location in XML comment so that
        // it can be reconstructed later on when moving from XHTML to wiki syntax.
        getXHTMLWikiPrinter().printXMLComment("startimage:" + this.imageRenderer.renderImage(image));
        getXHTMLWikiPrinter().printXMLElement("img", attributes);
        getXHTMLWikiPrinter().printXMLComment("stopimage");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(String, String)
     */
    @Override
    public void beginError(String message, String description)
    {
        super.beginError(message, description);
        
        getXHTMLWikiPrinter().printXMLStartElement("span", new String[][] {{"class", "xwikirenderingerror"}});
        getXHTMLWikiPrinter().printXML(message);
        getXHTMLWikiPrinter().printXMLComment("errordescription:" + description);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endError(String, String)
     */
    @Override
    public void endError(String message, String description)
    {
        getXHTMLWikiPrinter().printXMLEndElement("span");
        
        super.endError(message, description);
    }
}
