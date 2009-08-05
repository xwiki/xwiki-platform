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

import org.xwiki.rendering.renderer.xhtml.*;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.internal.renderer.xwiki.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.ImageType;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Default implementation for rendering images as XHTML. We handle both cases:
 * <ul>
 * <li>when inside a wiki (ie when an implementation of {@link WikiModel} is provided.</li>
 * <li>when outside of a wiki. In this case we only handle external images and document images don't 
 *     display anything.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultXHTMLImageRenderer implements XHTMLImageRenderer, Initializable
{
    /**
     * @see #setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
    private XHTMLWikiPrinter xhtmlPrinter;

    /**
     * Use to resolve local image URL when the image is attached to a document.
     */
    private WikiModel wikiModel;

    /**
     * Used to get the original image reference syntax.
     */
    private XWikiSyntaxImageRenderer imageRenderer;

    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // TODO: Transform it into a component later on
        this.imageRenderer = new XWikiSyntaxImageRenderer();

        // Try to find a WikiModel implementation and set it if it can be found. If not it means we're in
        // non wiki mode (i.e. no attachment in wiki documents and no links to documents for example).
        try {
            this.wikiModel = this.componentManager.lookup(WikiModel.class);
        } catch (ComponentLookupException e) {
            // There's no WikiModel implementation available. this.wikiModel stays null.
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLImageRenderer#setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.xhtmlPrinter = printer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLImageRenderer#onImage(Image, boolean, Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        Map<String, String> attributes = new LinkedHashMap<String, String>();

        // First we need to compute the image URL.
        String imageURL;
        if (image.getType() == ImageType.DOCUMENT) {
            DocumentImage documentImage = (DocumentImage) image;
            imageURL = this.wikiModel.getAttachmentURL(documentImage.getDocumentName(), 
                documentImage.getAttachmentName());
        } else {
            URLImage urlImage = (URLImage) image;
            imageURL = urlImage.getURL();
        }

        // Then add it as an attribute of the IMG element.
        attributes.put(SRC, imageURL);

        // Add the class if we're on a freestanding uri
        if (isFreeStandingURI) {
            attributes.put("class", "wikimodel-freestanding");
        }

        // Add the other parameters as attributes
        attributes.putAll(parameters);

        // If not ALT attribute has been specified, add it since the XHTML specifications makes it mandatory.
        if (!parameters.containsKey(ALTERNATE)) {
            attributes.put(ALTERNATE, image.getName());
        }

        // And generate the XHTML IMG element. We need to save the image location in XML comment so that
        // it can be reconstructed later on when moving from XHTML to wiki syntax.
        this.xhtmlPrinter.printXMLComment("startimage:" + this.imageRenderer.renderImage(image), true);
        this.xhtmlPrinter.printXMLElement(IMG, attributes);
        this.xhtmlPrinter.printXMLComment("stopimage");
    }
}
