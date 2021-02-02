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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.parser.html.HtmlEncodingDetector;
import org.w3c.dom.Document;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

/**
 * Default implementation of {@link XHTMLOfficeDocumentBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component
@Singleton
public class DefaultXHTMLOfficeDocumentBuilder implements XHTMLOfficeDocumentBuilder
{
    /**
     * Used to serialize the reference document name.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Used to obtain document converter.
     */
    @Inject
    private OfficeServer officeServer;

    /**
     * Office HTML cleaner.
     */
    @Inject
    @Named("openoffice")
    private HTMLCleaner officeHtmlCleaner;

    /**
     * Used to determine the encoding of the HTML byte array produced by the office server.
     */
    private HtmlEncodingDetector htmlEncodingDetector = new HtmlEncodingDetector();

    @Override
    public XHTMLOfficeDocument build(InputStream officeFileStream, String officeFileName, DocumentReference reference,
        boolean filterStyles) throws OfficeImporterException
    {
        // Accents seems to cause issues in some conditions
        // See https://jira.xwiki.org/browse/XWIKI-14692
        String cleanedOfficeFileName = StringUtils.stripAccents(officeFileName);

        // Invoke the office document converter.
        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        inputStreams.put(cleanedOfficeFileName, officeFileStream);
        // The office converter uses the output file name extension to determine the output format/syntax.
        String outputFileName = StringUtils.substringBeforeLast(cleanedOfficeFileName, ".") + ".html";
        OfficeConverterResult officeConverterResult;
        try {
            officeConverterResult =
                this.officeServer.getConverter().convertDocument(inputStreams, cleanedOfficeFileName, outputFileName);
        } catch (OfficeConverterException ex) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), ex);
        }

        Document xhtmlDoc = this.cleanAndCreateFile(reference, filterStyles, officeConverterResult);
        Set<File> artifacts = this.handleArtifacts(xhtmlDoc, officeConverterResult);

        // Return a new XHTMLOfficeDocument instance.
        return new XHTMLOfficeDocument(xhtmlDoc, artifacts, officeConverterResult);
    }

    private Document cleanAndCreateFile(DocumentReference reference, boolean filterStyles,
        OfficeConverterResult officeConverterResult) throws OfficeImporterException
    {
        // Prepare the parameters for HTML cleaning.
        Map<String, String> params = new HashMap<String, String>();
        params.put("targetDocument", this.entityReferenceSerializer.serialize(reference));
        // Extract the images that are embedded through the Data URI scheme and add them to the other artifacts so that
        // they end up as attachments.
        params.put("attachEmbeddedImages", "true");
        if (filterStyles) {
            params.put("filterStyles", "strict");
        }

        // Parse and clean the HTML output.
        HTMLCleanerConfiguration configuration = this.officeHtmlCleaner.getDefaultConfiguration();
        configuration.setParameters(params);

        Reader html = null;
        try {
            html = new FileReader(officeConverterResult.getOutputFile());
        } catch (FileNotFoundException e) {
            throw new OfficeImporterException(
                String.format("The output file cannot be found: [%s].", officeConverterResult.getOutputFile()), e);
        }
        return this.officeHtmlCleaner.clean(html, configuration);
    }

    private Set<File> handleArtifacts(Document xhtmlDoc, OfficeConverterResult officeConverterResult)
        throws OfficeImporterException
    {
        Set<File> artifacts = new HashSet<>(officeConverterResult.getAllFiles());
        artifacts.remove(officeConverterResult.getOutputFile());

        @SuppressWarnings("unchecked")
        Map<String, byte[]> embeddedImages = (Map<String, byte[]>) xhtmlDoc.getUserData("embeddedImages");
        if (embeddedImages != null) {
            File outputDirectory = officeConverterResult.getOutputDirectory();
            for (Map.Entry<String, byte[]> embeddedImage : embeddedImages.entrySet()) {
                File outputFile = new File(outputDirectory, embeddedImage.getKey());
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    IOUtils.write(embeddedImage.getValue(), fos);
                } catch (IOException e) {
                    throw new OfficeImporterException(
                        String.format("Error when writing embedded image file [%s]", outputFile.getAbsolutePath()), e);
                }
                artifacts.add(outputFile);
            }
        }
        return artifacts;
    }
}
