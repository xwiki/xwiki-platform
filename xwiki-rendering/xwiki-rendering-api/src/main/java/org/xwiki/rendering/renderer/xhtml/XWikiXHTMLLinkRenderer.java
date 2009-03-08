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
 * @version $Id: $
 * @since 1.8RC3
 */
public class XWikiXHTMLLinkRenderer implements XHTMLLinkRenderer
{
    /**
     * @see #setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
	private XHTMLWikiPrinter xhtmlPrinter;
	
    /**
     * @see #setHasLabel(boolean)
     */
	private boolean hasLabel;
	
    private DocumentAccessBridge documentAccessBridge;

    private LinkLabelGenerator linkLabelGenerator;

    private AttachmentParser attachmentParser;

    private XWikiSyntaxLinkRenderer xwikiSyntaxLinkRenderer;

    public XWikiXHTMLLinkRenderer(DocumentAccessBridge documentAccessBridge, LinkLabelGenerator linkLabelGenerator,
        AttachmentParser attachmentParser)
    {
        this.documentAccessBridge = documentAccessBridge;
        this.linkLabelGenerator = linkLabelGenerator;
        this.attachmentParser = attachmentParser;
        this.xwikiSyntaxLinkRenderer = new XWikiSyntaxLinkRenderer();
    }

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#setHasLabel(boolean)
     */
    public void setHasLabel(boolean hasLabel)
    {
        this.hasLabel = hasLabel;
    }

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.xhtmlPrinter = printer;
    }

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#beginLink(Link, boolean, Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Add an XML comment as a placeholder so that the XHTML parser can find the document name.
        // Otherwise it would be too difficult to transform a URL into a document name especially since
        // a link can refer to an external URL.
        this.xhtmlPrinter.printXMLComment("startwikilink:" + this.xwikiSyntaxLinkRenderer.renderLinkReference(link));

        Map<String, String> spanAttributes = new LinkedHashMap<String, String>();
        Map<String, String> aAttributes = new LinkedHashMap<String, String>();

        // Add all parameters to the A attributes
        aAttributes.putAll(parameters);

        if (link.isExternalLink()) {
            spanAttributes.put("class", "wikiexternallink");
            if (isFreeStandingURI) {
                aAttributes.put("class", "wikimodel-freestanding");
            }

            // href attribute
            if (link.getType() == LinkType.INTERWIKI) {
                // TODO: Resolve the Interwiki link
            } else {
                if ((link.getType() == LinkType.URI) && link.getReference().startsWith("attach:")) {
                    // use the default attachment syntax parser to extract document name and attachment name
                    Attachment attachment =
                        this.attachmentParser.parse(link.getReference().substring("attach:".length()));
                    aAttributes.put("href", this.documentAccessBridge.getAttachmentURL(attachment.getDocumentName(),
                        attachment.getAttachmentName()));
                } else {
                    aAttributes.put("href", link.getReference());
                }
            }

            this.xhtmlPrinter.printXMLStartElement("span", spanAttributes);
            this.xhtmlPrinter.printXMLStartElement("a", aAttributes);
        } else {
            // This is a link to a document.

            // Check for the document existence.
            if (StringUtils.isEmpty(link.getReference()) || this.documentAccessBridge.exists(link.getReference())) {
                spanAttributes.put("class", "wikilink");
                aAttributes.put("href", this.documentAccessBridge.getURL(link.getReference(), "view", link
                    .getQueryString(), link.getAnchor()));
                this.xhtmlPrinter.printXMLStartElement("span", spanAttributes);
                this.xhtmlPrinter.printXMLStartElement("a", aAttributes);
            } else {
                spanAttributes.put("class", "wikicreatelink");
                aAttributes.put("href", this.documentAccessBridge.getURL(link.getReference(), "edit", link
                    .getQueryString(), link.getAnchor()));

                this.xhtmlPrinter.printXMLStartElement("span", spanAttributes);
                this.xhtmlPrinter.printXMLStartElement("a", aAttributes);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see XHTMLLinkRenderer#endLink(Link, boolean, Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // If there was no link content then generate it based on the passed reference
        if (!this.hasLabel) {
            this.xhtmlPrinter.printXMLStartElement("span", new String[][] {{"class", "wikigeneratedlinkcontent"}});
            if (link.getType() == LinkType.DOCUMENT) {
                this.xhtmlPrinter.printXML(this.linkLabelGenerator.generate(link));
            } else {
                this.xhtmlPrinter.printXML(link.getReference());
            }
            this.xhtmlPrinter.printXMLEndElement("span");
        }

        this.xhtmlPrinter.printXMLEndElement("a");
        this.xhtmlPrinter.printXMLEndElement("span");

        // Add a XML comment to signify the end of the link.
        this.xhtmlPrinter.printXMLComment("stopwikilink");
    }
}
