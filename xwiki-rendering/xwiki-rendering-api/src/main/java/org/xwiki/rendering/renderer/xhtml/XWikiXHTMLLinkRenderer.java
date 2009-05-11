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
package org.xwiki.rendering.renderer.xhtml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxLinkRenderer;
import org.xwiki.rendering.listener.Attachment;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Default XWiki implementation for rendering links as XHTML using XWiki Documents.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class XWikiXHTMLLinkRenderer implements XHTMLLinkRenderer
{
    /**
     * The XHTML element <code>class</code> parameter.
     */
    private static final String CLASS = "class";

    /**
     * The name of the XHTML format element.
     */
    private static final String SPAN = "span";

    /**
     * The link reference prefix indicating that the link is targeting an attachment.
     */
    private static final String ATTACH = "attach:";
    
    /**
     * The class attribute 'wikilink'.
     */
    private static final String WIKILINK = "wikilink";

    /**
     * The XHTML printer to use to output links as XHTML.
     */
    private XHTMLWikiPrinter xhtmlPrinter;

    /**
     * @see #setHasLabel(boolean)
     */
    private boolean hasLabel;

    /**
     * Used to generate the link targeting a local document.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to generate a link label.
     */
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * Used to extract the attachment information form the reference if the link is targeting an attachment.
     */
    private AttachmentParser attachmentParser;

    /**
     * Used to save the original syntax of the link reference.
     */
    private XWikiSyntaxLinkRenderer xwikiSyntaxLinkRenderer;

    /**
     * Used to generate document String identifier.
     */
    private DocumentNameSerializer documentNameSerializer;

    /**
     * @param documentAccessBridge used to generate the link targeting a local document.
     * @param linkLabelGenerator used to generate a link label.
     * @param attachmentParser used to extract the attachment information form the reference if the link is targeting an
     *            attachment.
     * @param documentNameSerializer used to generate document String identifier.
     */
    public XWikiXHTMLLinkRenderer(DocumentAccessBridge documentAccessBridge, LinkLabelGenerator linkLabelGenerator,
        AttachmentParser attachmentParser, DocumentNameSerializer documentNameSerializer)
    {
        this.documentAccessBridge = documentAccessBridge;
        this.linkLabelGenerator = linkLabelGenerator;
        this.attachmentParser = attachmentParser;
        this.documentNameSerializer = documentNameSerializer;
        this.xwikiSyntaxLinkRenderer = new XWikiSyntaxLinkRenderer();
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkRenderer#setHasLabel(boolean)
     */
    public void setHasLabel(boolean hasLabel)
    {
        this.hasLabel = hasLabel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkRenderer#setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.xhtmlPrinter = printer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkRenderer#beginLink(Link, boolean, Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Add an XML comment as a placeholder so that the XHTML parser can find the document name.
        // Otherwise it would be too difficult to transform a URL into a document name especially since
        // a link can refer to an external URL.
        this.xhtmlPrinter.printXMLComment("startwikilink:" + this.xwikiSyntaxLinkRenderer.renderLinkReference(link),
            true);

        if (link.isExternalLink()) {
            beginExternalLink(link, isFreeStandingURI, parameters);
        } else {
            beginInternalLink(link, isFreeStandingURI, parameters);
        }
    }
    
    /**
     * Start of an external link.
     * 
     * @param link the link definition (the reference)
     * @param isFreeStandingURI if true then the link is a free standing URI directly in the text
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    private void beginExternalLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        Map<String, String> spanAttributes = new LinkedHashMap<String, String>();
        Map<String, String> aAttributes = new LinkedHashMap<String, String>();

        // Add all parameters to the A attributes
        aAttributes.putAll(parameters);
        
        spanAttributes.put(CLASS, "wikiexternallink");
        if (isFreeStandingURI) {
            aAttributes.put(CLASS, "wikimodel-freestanding");
        }

        // href attribute
        if (link.getType() == LinkType.INTERWIKI) {
            // TODO: Resolve the Interwiki link
        } else {
            if ((link.getType() == LinkType.URI) && link.getReference().startsWith(ATTACH)) {
                // use the default attachment syntax parser to extract document name and attachment name
                Attachment attachment = this.attachmentParser.parse(link.getReference().substring(ATTACH.length()));
                aAttributes.put(HREF, this.documentAccessBridge.getAttachmentURL(attachment.getDocumentName(),
                    attachment.getAttachmentName()));
            } else {
                aAttributes.put(HREF, link.getReference());
            }
        }
        
        this.xhtmlPrinter.printXMLStartElement(SPAN, spanAttributes);
        this.xhtmlPrinter.printXMLStartElement(ANCHOR, aAttributes);
    }
    
    /**
     * Start of an internal link.
     * 
     * @param link the link definition (the reference)
     * @param isFreeStandingURI if true then the link is a free standing URI directly in the text
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    private void beginInternalLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters) 
    {
        Map<String, String> spanAttributes = new LinkedHashMap<String, String>();
        Map<String, String> aAttributes = new LinkedHashMap<String, String>();

        // Add all parameters to the A attributes
        aAttributes.putAll(parameters);
        
        if (StringUtils.isEmpty(link.getReference()) && link.getAnchor() != null) {            
            spanAttributes.put(CLASS, WIKILINK);
            aAttributes.put(HREF, "#" + link.getAnchor());
        } else if (StringUtils.isEmpty(link.getReference()) || this.documentAccessBridge.exists(link.getReference())) {
            spanAttributes.put(CLASS, WIKILINK);                
            aAttributes.put(HREF, this.documentAccessBridge.getURL(link.getReference(), "view", link
                .getQueryString(), link.getAnchor()));
        } else {
            spanAttributes.put(CLASS, "wikicreatelink");

            // Add the parent=<current document name> parameter to the query string of the generated URL so that
            // the new document is created with the current page as its parent.
            String queryString = link.getQueryString();
            if (StringUtils.isBlank(queryString)) {
                DocumentName documentName = this.documentAccessBridge.getCurrentDocumentName();
                if (documentName != null) {
                    queryString = "parent=" + this.documentNameSerializer.serialize(documentName);
                }
            }

            aAttributes.put(HREF, this.documentAccessBridge.getURL(link.getReference(), "edit", queryString, link
                .getAnchor()));

        }
        
        this.xhtmlPrinter.printXMLStartElement(SPAN, spanAttributes);
        this.xhtmlPrinter.printXMLStartElement(ANCHOR, aAttributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkRenderer#endLink(Link, boolean, Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // If there was no link content then generate it based on the passed reference
        if (!this.hasLabel) {
            this.xhtmlPrinter.printXMLStartElement(SPAN, new String[][] {{CLASS, "wikigeneratedlinkcontent"}});
            if (link.getType() == LinkType.DOCUMENT) {
                this.xhtmlPrinter.printXML(this.linkLabelGenerator.generate(link));
            } else {
                this.xhtmlPrinter.printXML(link.getReference());
            }
            this.xhtmlPrinter.printXMLEndElement(SPAN);
        }

        this.xhtmlPrinter.printXMLEndElement(ANCHOR);
        this.xhtmlPrinter.printXMLEndElement(SPAN);

        // Add a XML comment to signify the end of the link.
        this.xhtmlPrinter.printXMLComment("stopwikilink");
    }
}
