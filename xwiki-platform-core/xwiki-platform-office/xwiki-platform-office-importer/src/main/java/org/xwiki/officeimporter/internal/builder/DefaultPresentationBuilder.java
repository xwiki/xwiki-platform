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
package org.xwiki.officeimporter.internal.builder;

import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ExpandedMacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Default implementation of {@link PresentationBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component
@Singleton
public class DefaultPresentationBuilder implements PresentationBuilder
{
    /**
     * Component manager used by {@link XDOMOfficeDocument}.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Used to obtain document converter.
     */
    @Inject
    private OpenOfficeManager officeManager;

    /**
     * Used to access current context document.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to serialize the reference document name.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * OpenOffice HTML cleaner.
     */
    @Inject
    @Named("openoffice")
    private HTMLCleaner ooHTMLCleaner;

    /**
     * The component used to parse the XHTML obtained after cleaning.
     */
    @Inject
    @Named("xhtml/1.0")
    private Parser xhtmlParser;

    @Override
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName,
        DocumentReference documentReference) throws OfficeImporterException
    {
        // Invoke OpenOffice document converter.
        Map<String, byte[]> artifacts = importPresentation(officeFileStream, officeFileName);

        // Create presentation HTML.
        String html = buildPresentationHTML(artifacts, StringUtils.substringBeforeLast(officeFileName, "."));

        // Clear and adjust presentation HTML (slide image URLs are updated to point to the corresponding attachments).
        html = cleanPresentationHTML(html, documentReference);

        // Create the XDOM.
        XDOM xdom = buildPresentationXDOM(html, documentReference);

        return new XDOMOfficeDocument(xdom, artifacts, this.componentManager);
    }

    /**
     * Invokes the Office Server to convert the given input stream. The result is a map of artifacts including slide
     * images.
     * 
     * @param officeFileStream the office presentation byte stream
     * @param officeFileName the name of the office presentation that is being imported
     * @return the map of artifacts created by the Office Server
     * @throws OfficeImporterException if converting the office presentation fails
     */
    protected Map<String, byte[]> importPresentation(InputStream officeFileStream, String officeFileName)
        throws OfficeImporterException
    {
        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        inputStreams.put(officeFileName, officeFileStream);
        try {
            // The OpenOffice converter uses the output file name extension to determine the output format/syntax.
            // The returned artifacts are of three types: imgX.jpg (slide screen shot), imgX.html (HTML page that
            // display the corresponding slide screen shot) and textX.html (HTML page that display the text extracted
            // from the corresponding slide). We use "img0.html" as the output file name because the corresponding
            // artifact displays a screen shot of the first presentation slide.
            return this.officeManager.getConverter().convert(inputStreams, officeFileName, "img0.html");
        } catch (OpenOfficeConverterException e) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), e);
        }
    }

    /**
     * Builds the presentation HTML from the presentation artifacts. There are two types of presentation artifacts:
     * slide image and slide text. The returned HTML will display all the slide images. Slide text is currently ignored.
     * All artifacts except slide images are removed from {@code presentationArtifacts}. Slide images names are prefixed
     * with the given {@code nameSpace} to avoid name conflicts.
     * 
     * @param presentationArtifacts the map of presentation artifacts; this method removes some of the presentation
     *        artifacts and renames others so be aware of the side effects
     * @param nameSpace the prefix to add in front of all slide image names to prevent name conflicts
     * @return the presentation HTML
     */
    protected String buildPresentationHTML(Map<String, byte[]> presentationArtifacts, String nameSpace)
    {
        StringBuilder presentationHTML = new StringBuilder();

        // Iterate all the slides.
        int i = 0;
        String slideImageKeyFormat = "img%s.jpg";
        byte[] slideImage = presentationArtifacts.remove(String.format(slideImageKeyFormat, i));
        while (slideImage != null) {
            // Remove unused artifacts.
            // imgX.html is an HTML page that displays the corresponding slide image.
            presentationArtifacts.remove(String.format("img%s.html", i));
            // textX.html is an HTML page that displays the text extracted from the corresponding slide.
            presentationArtifacts.remove(String.format("text%s.html", i));

            // Rename slide image to prevent name conflicts when it will be attached to the target document.
            String slideImageName = String.format("%s-slide%s.jpg", nameSpace, i);
            presentationArtifacts.put(slideImageName, slideImage);

            // Append slide image to the presentation HTML.
            String slideImageURL = null;
            try {
                // We need to encode the slide image name in case it contains special URL characters.
                slideImageURL = URLEncoder.encode(slideImageName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // This should never happen.
            }
            presentationHTML.append(String.format("<p><img src=\"%s\"/></p>", slideImageURL));

            // Move to the next slide.
            slideImage = presentationArtifacts.remove(String.format(slideImageKeyFormat, ++i));
        }

        return presentationHTML.toString();
    }

    /**
     * Cleans the presentation HTML. This method must be called mainly to ensure that the slide image URLs are updated
     * to point to the corresponding attachments.
     * 
     * @param dirtyHTML the HTML to be cleaned
     * @param targetDocumentReference the document where the slide images will be attached
     * @return the cleaned HTML
     */
    protected String cleanPresentationHTML(String dirtyHTML, DocumentReference targetDocumentReference)
    {
        HTMLCleanerConfiguration configuration = this.ooHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", this.entityReferenceSerializer
            .serialize(targetDocumentReference)));
        Document xhtmlDocument = this.ooHTMLCleaner.clean(new StringReader(dirtyHTML), configuration);
        HTMLUtils.stripHTMLEnvelope(xhtmlDocument);
        return HTMLUtils.toString(xhtmlDocument);
    }

    /**
     * Parses the given HTML text into an XDOM tree.
     * 
     * @param html the HTML text to parse
     * @param targetDocumentReference specifies the document where the presentation will be imported; we use the target
     *        document reference to get the syntax of the target document;
     * @return a XDOM tree
     * @throws OfficeImporterException if parsing the given HTML fails
     */
    protected XDOM buildPresentationXDOM(String html, DocumentReference targetDocumentReference)
        throws OfficeImporterException
    {
        try {
            String syntaxId = this.documentAccessBridge.getDocument(targetDocumentReference).getSyntax().toIdString();
            BlockRenderer renderer = this.componentManager.getInstance(BlockRenderer.class, syntaxId);

            Map<String, String> galleryParameters = Collections.emptyMap();
            ExpandedMacroBlock gallery = new ExpandedMacroBlock("gallery", galleryParameters, renderer, false);
            gallery.addChild(this.xhtmlParser.parse(new StringReader(html)));

            return new XDOM(Collections.singletonList((Block) gallery));
        } catch (Exception e) {
            throw new OfficeImporterException("Failed to build presentation XDOM.", e);
        }
    }
}
