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
package org.xwiki.rendering.internal.renderer.xhtml;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultAttribute;
import org.dom4j.tree.DefaultElement;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;

/**
 * @version $Id: $
 * @since 1.5RC1
 */
public class XHTMLLinkRenderer
{
    private DocumentAccessBridge documentAccessBridge;

    private RenderingConfiguration configuration;

    public XHTMLLinkRenderer(DocumentAccessBridge documentAccessBridge, RenderingConfiguration configuration)
    {
        this.documentAccessBridge = documentAccessBridge;
        this.configuration = configuration;
    }

    public void renderLink(XMLWriter xmlWriter, Link link, boolean isFreeStandingURI)
    {
        try {
            List<Element> elements = createDOM4JLink(link, isFreeStandingURI);
            xmlWriter.write(elements);
        } catch (Exception e) {
            // TODO add error log
        }
    }

    public List<Element> createDOM4JLink(Link link, boolean isFreeStandingURI) throws Exception
    {
        List<Element> elements = new ArrayList<Element>();

        // span element
        DefaultElement spanElement = new DefaultElement("span");
        Attribute spanClassAttribute = new DefaultAttribute(new QName("class"));

        // a element
        DefaultElement aElement = new DefaultElement("a");
        Attribute aHrefAttribute = new DefaultAttribute(new QName("href"));

        // add a element to span element
        spanElement.add(aElement);

        // add span element to element list
        elements.add(spanElement);

        Element labelContainer = aElement;
        if (link.isExternalLink()) {
            spanClassAttribute.setValue("wikiexternallink");

            if (isFreeStandingURI) {
                aElement.add(new DefaultAttribute("class", "wikimodel-freestanding"));
            }

            // href attribute
            if (link.getType() == LinkType.INTERWIKI) {
                // TODO: Resolve the Interwiki link
            } else {
                aHrefAttribute.setValue(link.getReference());
            }
        } else {
            // This is a link to a document. Check for the document existence.
            if (link.getReference() == null || this.documentAccessBridge.exists(link.getReference())) {
                spanClassAttribute.setValue("wikilink");
                aHrefAttribute.setValue(this.documentAccessBridge.getURL(link.getReference(), "view", link
                    .getQueryString(), link.getAnchor()));
            } else {
                spanClassAttribute.setValue("wikicreatelink");
                aHrefAttribute.setValue(this.documentAccessBridge.getURL(link.getReference(), "edit", link
                    .getQueryString(), link.getAnchor()));

                labelContainer = new DefaultElement("span");
                labelContainer.add(new DefaultAttribute("class", "wikicreatelinktext"));

                aElement.add(labelContainer);

                DefaultElement qmElement = new DefaultElement("span");
                qmElement.add(new DefaultAttribute("class", "wikicreatelinkqm"));
                qmElement.addText("?");

                aElement.add(qmElement);
            }
        }

        spanElement.add(spanClassAttribute);

        aElement.add(aHrefAttribute);
        if (link.getTarget() != null) {
            // We prefix with "_" since a target can be any token and we need to differentiate with
            // other valid rel tokens.
            aElement.add(new DefaultAttribute("rel", "_" + link.getTarget()));
        }

        labelContainer.addText(getLinkLabelToPrint(link));

        return elements;
    }

    private String getLinkLabelToPrint(Link link)
    {
        String labelToPrint;

        // If the usser has specified a label use it, if not then use the rendering configuration value to find out
        // what should be printed.

        if (link.getLabel() != null) {
            labelToPrint = link.getLabel();
        } else {
            // TODO we need to use a DocumentName and DocumentNameFactory to transform a reference as a String
            // Then use the LinkLabelResolver
            labelToPrint = link.getReference();
        }

        return labelToPrint;
    }
}
