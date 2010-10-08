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

import org.xwiki.rendering.internal.renderer.ParametersPrinter;
import org.xwiki.rendering.listener.reference.ResourceReference;
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
     * Character to separate Link reference and parameters in XHTML comments.
     */
    private static final String COMMENT_SEPARATOR = "|-|";

    /**
     * Used to print Link Parameters in XHTML comments.
     */
    private ParametersPrinter parametersPrinter = new ParametersPrinter();

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

        // Print if the Image Reference is typed, the Link Reference Type and the Image Reference itself
        buffer.append(reference.isTyped());
        buffer.append(COMMENT_SEPARATOR);
        buffer.append(reference.getType().getScheme());
        buffer.append(COMMENT_SEPARATOR);
        buffer.append(reference.getReference());

        // Print Image Reference parameters. We need to do this so that the XHTML parser doesn't have
        // to parse the query string to extract the parameters. Doing so could lead to false result since
        // for example the XHTML renderer can add a parent parameter in the query string for images to non
        // existing attachments.
        //
        // Also note that we don't need to print Image parameters since they are added as XHTML class
        // attributes by the XHTML Renderer and thus the XHTML parser will be able to get them
        // agian as attributes.
        Map<String, String> imageReferenceParameters = reference.getParameters();
        if (!imageReferenceParameters.isEmpty()) {
            buffer.append(COMMENT_SEPARATOR);
            buffer.append(this.parametersPrinter.print(imageReferenceParameters, '\\'));
        }

        getXHTMLWikiPrinter().printXMLComment(buffer.toString(), true);
        this.defaultImageRenderer.onImage(reference, isFreeStandingURI, parameters);
        getXHTMLWikiPrinter().printXMLComment("stopimage");
    }
}
