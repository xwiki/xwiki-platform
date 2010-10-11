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
package org.xwiki.rendering.internal.renderer.xhtml.link;

import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;

/**
 * Render links as XHTML, using annotations (see
 * {@link org.xwiki.rendering.internal.renderer.xhtml.AnnotatedXHTMLRenderer} for more details).
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component("annotated")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class AnnotatedXHTMLLinkRenderer implements XHTMLLinkRenderer
{
    /**
     * Used to print Image reference as XHTML comments.
     */
    @Requirement("xhtmlmarker")
    private ResourceReferenceSerializer xhtmlMarkerSerializer;

    /**
     * The default XHTML Link Renderer that we're wrapping.
     */
    @Requirement
    private XHTMLLinkRenderer defaultLinkRenderer;

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkRenderer#setXHTMLWikiPrinter(org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter)
     */
    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.defaultLinkRenderer.setXHTMLWikiPrinter(printer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkRenderer#setHasLabel(boolean)
     */
    public void setHasLabel(boolean hasLabel)
    {
        this.defaultLinkRenderer.setHasLabel(hasLabel);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.LinkListener#beginLink(org.xwiki.rendering.listener.reference.ResourceReference ,
     *      boolean, java.util.Map)
     */
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Add an XML comment as a placeholder so that the XHTML parser can find the document name.
        // Otherwise it would be too difficult to transform a URL into a document name especially since
        // a link can refer to an external URL.
        StringBuffer buffer = new StringBuffer("startwikilink:");
        buffer.append(this.xhtmlMarkerSerializer.serialize(reference));

        getXHTMLWikiPrinter().printXMLComment(buffer.toString(), true);
        this.defaultLinkRenderer.beginLink(reference, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.LinkListener#endLink(org.xwiki.rendering.listener.reference.ResourceReference ,
     *      boolean, java.util.Map)
     */
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.defaultLinkRenderer.endLink(reference, isFreeStandingURI, parameters);

        // Add a XML comment to signify the end of the link.
        getXHTMLWikiPrinter().printXMLComment("stopwikilink");
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLLinkRenderer#getXHTMLWikiPrinter()
     */
    public XHTMLWikiPrinter getXHTMLWikiPrinter()
    {
        return this.defaultLinkRenderer.getXHTMLWikiPrinter();
    }
}
