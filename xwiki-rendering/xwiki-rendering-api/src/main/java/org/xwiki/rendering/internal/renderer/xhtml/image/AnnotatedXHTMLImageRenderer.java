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

import org.xwiki.rendering.internal.renderer.xwiki20.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

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
     * The default XHTML Link Renderer that we're wrapping.
     */
    @Requirement
    private XHTMLImageRenderer defaultImageRenderer;

    /**
     * Used to get the original image reference syntax.
     */
    private XWikiSyntaxImageRenderer imageRenderer;

    public AnnotatedXHTMLImageRenderer()
    {
        this.imageRenderer = new XWikiSyntaxImageRenderer();
    }

    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.defaultImageRenderer.setXHTMLWikiPrinter(printer);
    }

    public XHTMLWikiPrinter getXHTMLWikiPrinter()
    {
        return this.defaultImageRenderer.getXHTMLWikiPrinter();
    }

    public void onImage(Image image, boolean isFreeStandingURI,
        Map<String, String> parameters)
    {
        // We need to save the image location in XML comment so that it can be reconstructed later on when moving
        // from XHTML to wiki syntax.
        getXHTMLWikiPrinter().printXMLComment("startimage:" + this.imageRenderer.renderImage(image), true);
        this.defaultImageRenderer.onImage(image, isFreeStandingURI, parameters);
        getXHTMLWikiPrinter().printXMLComment("stopimage");
    }
}