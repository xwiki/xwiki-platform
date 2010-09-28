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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
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
public class DefaultPresentationBuilder implements PresentationBuilder
{
    /**
     * Component manager used by {@link XDOMOfficeDocument}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to obtain document converter.
     */
    @Requirement
    private OpenOfficeManager officeManager;

    /**
     * Used to access current context document.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to serialize the reference document name.
     */
    @Requirement
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * OpenOffice HTML cleaner.
     */
    @Requirement("openoffice")
    private HTMLCleaner ooHTMLCleaner;

    /**
     * XHTML/1.0 syntax parser used to build an XDOM from an XHTML input.
     */
    @Requirement("xhtml/1.0")
    private Parser xhtmlParser;

    /**
     * {@inheritDoc}
     */
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName,
        DocumentReference documentReference) throws OfficeImporterException
    {
        // Invoke OpenOffice document converter.
        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        inputStreams.put(officeFileName, officeFileStream);
        Map<String, byte[]> artifacts;
        try {
            // The OpenOffice converter uses the output file name extension to determine the output format/syntax.
            // The returned artifacts are of three types: imgX.jpg (slide screen shot), imgX.html (HTML page that
            // display the corresponding slide screen shot) and textX.html (HTML page that display the text extracted
            // from the corresponding slide). We use "img0.html" as the output file name because the corresponding
            // artifact displays a screen shot of the first presentation slide.
            artifacts = officeManager.getConverter().convert(inputStreams, officeFileName, "img0.html");
        } catch (OpenOfficeConverterException e) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), e);
        }

        // Create presentation HTML.
        String html = buildPresentationHTML(artifacts, StringUtils.substringBeforeLast(officeFileName, "."));

        // Clear and adjust presentation HTML (slide image URLs are updated to point to the corresponding attachments).
        HTMLCleanerConfiguration configuration = ooHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", entityReferenceSerializer
            .serialize(documentReference)));
        Document xhtmlDocument = ooHTMLCleaner.clean(new StringReader(html), configuration);
        HTMLUtils.stripHTMLEnvelope(xhtmlDocument);
        html = HTMLUtils.toString(xhtmlDocument);

        // Parse presentation HTML.
        try {
            return new XDOMOfficeDocument(xhtmlParser.parse(new StringReader(html)), artifacts, this.componentManager);
        } catch (ParseException e) {
            throw new OfficeImporterException("Failed to parse presentation HTML.", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.builder.PresentationBuilder#build(java.io.InputStream, java.lang.String)
     */
    @Deprecated
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName) throws OfficeImporterException
    {
        return build(officeFileStream, officeFileName, documentAccessBridge.getCurrentDocumentReference());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.officeimporter.builder.PresentationBuilder#build(byte[])
     */
    @Deprecated
    public XDOMOfficeDocument build(byte[] officeFileData) throws OfficeImporterException
    {
        return build(new ByteArrayInputStream(officeFileData), "input.tmp");
    }

    /**
     * Builds the presentation HTML from the presentation artifacts. There are two types of presentation artifacts:
     * slide image and slide text. The returned HTML will display all the slide images. Slide text is currently ignored.
     * All artifacts except slide images are removed from {@code presentationArtifacts}. Slide images names are prefixed
     * with the given {@code nameSpace} to avoid name conflicts.
     * 
     * @param presentationArtifacts the map of presentation artifacts; this method removes some of the presentation
     *            artifacts and renames others so be aware of the side effects
     * @param nameSpace the prefix to add in front of all slide image names to prevent name conflicts
     * @return the presentation HTML
     */
    private String buildPresentationHTML(Map<String, byte[]> presentationArtifacts, String nameSpace)
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
}
