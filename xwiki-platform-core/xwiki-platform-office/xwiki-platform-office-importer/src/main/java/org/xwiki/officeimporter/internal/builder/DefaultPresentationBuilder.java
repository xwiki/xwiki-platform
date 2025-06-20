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

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.converter.OfficeConverterFileStorage;
import org.xwiki.officeimporter.internal.document.FileOfficeDocumentArtifact;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ExpandedMacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.xml.XMLUtils;
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
     * Provides the component manager used by {@link XDOMOfficeDocument}.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Used to obtain document converter.
     */
    @Inject
    private OfficeServer officeServer;

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
     * Office HTML cleaner.
     */
    @Inject
    @Named("openoffice")
    private HTMLCleaner officeHTMLCleaner;

    /**
     * The component used to parse the XHTML obtained after cleaning.
     */
    @Inject
    @Named("xhtml/1.0")
    private Parser xhtmlParser;

    @Inject
    private PresentationBuilderConfiguration presentationBuilderConfiguration;

    @Override
    public XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName,
        DocumentReference documentReference) throws OfficeImporterException
    {
        // Invoke the office document converter.
        OfficeConverterResult officeConverterResult = importPresentation(officeFileStream, officeFileName);

        Pair<String, Map<String, OfficeDocumentArtifact>> htmlPresentationResult;
        // Create presentation HTML.
        try {
            htmlPresentationResult = buildPresentationHTML(officeConverterResult,
                StringUtils.substringBeforeLast(officeFileName, "."));
        } catch (IOException e) {
            throw new OfficeImporterException("Error while preparing the presentation artifacts.", e);
        }

        // Clear and adjust presentation HTML (slide image URLs are updated to point to the corresponding attachments).
        String html = cleanPresentationHTML(htmlPresentationResult.getLeft(), documentReference);

        // Create the XDOM.
        XDOM xdom = buildPresentationXDOM(html, documentReference);

        return new XDOMOfficeDocument(xdom, htmlPresentationResult.getRight(),
            this.contextComponentManagerProvider.get(), officeConverterResult);
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
    protected OfficeConverterResult importPresentation(InputStream officeFileStream, String officeFileName)
        throws OfficeImporterException
    {
        String inputFileName = OfficeConverterFileStorage.getSafeInputFilenameFromExtension(officeFileName);
        Map<String, InputStream> inputStreams = Map.of(inputFileName, officeFileStream);
        try {
            // The office converter uses the output file name extension to determine the output format/syntax.
            // We perform a conversion to PDF to then use a PDF to image conversion.
            return this.officeServer.getConverter().convertDocument(inputStreams, inputFileName, "presentation.pdf");
        } catch (OfficeConverterException e) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), e);
        }
    }

    /**
     * Builds the presentation HTML from the presentation PDF: we convert all PDF page to an image using naming
     * convention {@code slideX.imageFormatExtension} where X is the slide number.
     * 
     * @param officeConverterResult the map of presentation artifacts; this method removes some of the presentation
     *            artifacts and renames others so be aware of the side effects
     * @param nameSpace the prefix to add in front of all slide image names to prevent name conflicts
     * @return the presentation HTML
     */
    protected Pair<String, Map<String, OfficeDocumentArtifact>> buildPresentationHTML(
        OfficeConverterResult officeConverterResult, String nameSpace) throws IOException
    {
        Map<String, OfficeDocumentArtifact> artifactFiles = new HashMap<>();
        String imageFormat = this.presentationBuilderConfiguration.getImageFormat();
        float quality = this.presentationBuilderConfiguration.getQuality();

        // We converted the slides to PDF. Now convert each page of the PDF to an image.
        List<String> filenames = new ArrayList<>();
        try (PDDocument document = PDDocument.load(officeConverterResult.getOutputFile())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int numberOfPages = document.getPages().getCount();
            for (int pageCounter = 0; pageCounter < numberOfPages; ++pageCounter) {
                // note that the page number parameter is zero based
                // Compute the scale based on the slide width.
                int outputWidth = this.presentationBuilderConfiguration.getSlideWidth();
                // Get the width of the slide in points.
                float pageWidth = document.getPage(pageCounter).getMediaBox().getWidth();
                // Compute the scale based on the slide width.
                // Add 0.5 to the output width to ensure that rounding down does not make the image smaller than
                // desired.
                float scale = (outputWidth + 0.5f) / pageWidth;

                BufferedImage bim = pdfRenderer.renderImage(pageCounter, scale, ImageType.RGB);

                String slideFileName = String.format("slide%s.%s", pageCounter, imageFormat);

                // Store the image in the output directory as this will be cleaned up automatically at the end.
                File imageFile = new File(officeConverterResult.getOutputDirectory(), slideFileName);
                try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(imageFile))) {
                    ImageIOUtil.writeImage(bim, imageFormat, outputStream, (int) (scale * 72f), quality);
                }

                String slideImageName = String.format("%s-slide%s.%s", nameSpace, pageCounter, imageFormat);
                artifactFiles.put(slideImageName, new FileOfficeDocumentArtifact(slideImageName, imageFile));
                // suffix in filename will be used as the file format

                // Append slide image to the presentation HTML.
                // We need to encode the slide image name in case it contains special URL characters.
                String slideImageURL = URLEncoder.encode(slideImageName, StandardCharsets.UTF_8);
                // We do not want to encode the spaces in '+' since '+' will be then reencoded in
                // ImageFilter to keep it and not consider it as a space when decoding it.
                // This is link to a bug in libreoffice that does not convert properly the '+', so we cannot distinguish
                // them from spaces in filenames. This should be removed once
                // https://github.com/sbraconnier/jodconverter/issues/125 is fixed.
                slideImageURL = slideImageURL.replace('+', ' ');

                filenames.add(slideImageURL);
            }
        }

        // We sort by number so that the filenames are ordered by slide number.
        String presentationHTML = filenames.stream()
            .map(entry -> String.format("<p><img src=\"%s\"/></p>", XMLUtils.escapeAttributeValue(entry)))
            .collect(Collectors.joining());
        return Pair.of(presentationHTML, artifactFiles);
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
        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument",
            this.entityReferenceSerializer.serialize(targetDocumentReference)));
        Document xhtmlDocument = this.officeHTMLCleaner.clean(new StringReader(dirtyHTML), configuration);
        HTMLUtils.stripHTMLEnvelope(xhtmlDocument);
        return HTMLUtils.toString(xhtmlDocument);
    }

    /**
     * Parses the given HTML text into an XDOM tree.
     * 
     * @param html the HTML text to parse
     * @param targetDocumentReference specifies the document where the presentation will be imported; we use the target
     *            document reference to get the syntax of the target document and to set the {@code BASE} meta data on
     *            the created XDOM
     * @return a XDOM tree
     * @throws OfficeImporterException if parsing the given HTML fails
     */
    protected XDOM buildPresentationXDOM(String html, DocumentReference targetDocumentReference)
        throws OfficeImporterException
    {
        try {
            ComponentManager contextComponentManager = this.contextComponentManagerProvider.get();
            String syntaxId = this.documentAccessBridge.getTranslatedDocumentInstance(targetDocumentReference)
                .getSyntax().toIdString();
            BlockRenderer renderer = contextComponentManager.getInstance(BlockRenderer.class, syntaxId);

            Map<String, String> galleryParameters = Collections.emptyMap();
            ExpandedMacroBlock gallery =
                new ExpandedMacroBlock("gallery", galleryParameters, renderer, false, contextComponentManager);
            gallery.addChild(this.xhtmlParser.parse(new StringReader(html)));

            XDOM xdom = new XDOM(Collections.singletonList((Block) gallery));
            // Make sure (image) references are resolved relative to the target document reference.
            xdom.getMetaData().addMetaData(MetaData.BASE, entityReferenceSerializer.serialize(targetDocumentReference));
            return xdom;
        } catch (Exception e) {
            throw new OfficeImporterException("Failed to build presentation XDOM.", e);
        }
    }
}
