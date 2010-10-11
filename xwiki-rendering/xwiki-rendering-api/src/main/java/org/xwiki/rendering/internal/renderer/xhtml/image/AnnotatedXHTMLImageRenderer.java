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
package org.xwiki.rendering.internal.renderer.xhtml.image;

import java.util.Map;

import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;

/**
 * Render images as XHTML, using annotations (see
 * {@link org.xwiki.rendering.internal.renderer.xhtml.AnnotatedXHTMLRenderer} for more details).
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component("annotated")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class AnnotatedXHTMLImageRenderer implements XHTMLImageRenderer
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
    private XHTMLImageRenderer defaultImageRenderer;

    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.defaultImageRenderer.setXHTMLWikiPrinter(printer);
    }

    public XHTMLWikiPrinter getXHTMLWikiPrinter()
    {
        return this.defaultImageRenderer.getXHTMLWikiPrinter();
    }

    /**
     * {@inheritDoc}
     * @see XHTMLImageRenderer#onImage(org.xwiki.rendering.listener.reference.ResourceReference , boolean, java.util.Map)
     * @since 2.5RC1
     */
    public void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // We need to save the image location in XML comment so that it can be reconstructed later on when moving
        // from XHTML to wiki syntax.
        StringBuffer buffer = new StringBuffer("startimage:");
        buffer.append(this.xhtmlMarkerSerializer.serialize(reference));

        getXHTMLWikiPrinter().printXMLComment(buffer.toString(), true);
        this.defaultImageRenderer.onImage(reference, isFreeStandingURI, parameters);
        getXHTMLWikiPrinter().printXMLComment("stopimage");
    }
}
