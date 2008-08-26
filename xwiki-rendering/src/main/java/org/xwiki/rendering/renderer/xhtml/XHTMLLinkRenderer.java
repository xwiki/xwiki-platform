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

import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.DocumentManager;
import org.xwiki.rendering.configuration.RenderingConfiguration;

/**
 * @version $Id: $
 * @since 1.5RC1
 */
public class XHTMLLinkRenderer
{
    private DocumentManager documentManager;

    private RenderingConfiguration configuration;

    public XHTMLLinkRenderer(DocumentManager documentManager, RenderingConfiguration configuration)
    {
        this.documentManager = documentManager;
        this.configuration = configuration;
    }

    public String renderLink(Link link)
    {
        StringBuffer sb = new StringBuffer();

        if (link.isExternalLink()) {
            sb.append("<span class=\"wikiexternallink\">");
            sb.append("<a href=\"");
            if (link.getType() == LinkType.INTERWIKI) {
                // TODO: Resolve the Interwiki link
            } else {
                sb.append(link.getReference());
            }
            sb.append("\"");
            if (link.getTarget() != null) {
                // We prefix with "_" since a target can be any token and we need to differentiate with
                // other valid rel tokens.
                sb.append(" rel=\"_").append(link.getTarget()).append("\"");
            }
            sb.append(">");
            sb.append(getLinkLabelToPrint(link));
            sb.append("</a></span>");
        } else {
            // This is a document link. Check for document existence.
            try {
                if (link.getReference() == null || this.documentManager.exists(link.getReference())) {
                    sb.append("<span class=\"wikilink\">");
                    sb.append("<a href=\"");
                    sb.append(this.documentManager.getURL(link.getReference(), "view", link.getQueryString(), link
                        .getAnchor()));
                    sb.append("\"");
                    if (link.getTarget() != null) {
                        // We prefix with "_" since a target can be any token and we need to differentiate with
                        // other valid rel tokens.
                        sb.append(" rel=\"_").append(link.getTarget()).append("\"");
                    }
                    sb.append(">");
                    sb.append(getLinkLabelToPrint(link));
                    sb.append("</a></span>");
                } else {
                    sb.append("<a class=\"wikicreatelink\" href=\"");
                    sb.append(this.documentManager.getURL(link.getReference(), "edit", link.getQueryString(), link
                        .getAnchor()));
                    sb.append("\"");
                    if (link.getTarget() != null) {
                        // We prefix with "_" since a target can be any token and we need to differentiate with
                        // other valid rel tokens.
                        sb.append(" rel=\"_").append(link.getTarget()).append("\"");
                    }
                    sb.append(">");
                    sb.append("<span class=\"wikicreatelinktext\">");
                    sb.append(getLinkLabelToPrint(link));
                    sb.append("</span><span class=\"wikicreatelinkqm\">?</span></a>");
                }
            } catch (Exception e) {
                // An exception occurred, don't render any XHTML but raise an error in the logs
                // TODO: Log error
            }
        }

        return sb.toString();
    }

    private String getLinkLabelToPrint(Link link)
    {
        String labelToPrint;

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
