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
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.internal.converter.OfficeConverterFileStorage;
import org.xwiki.officeimporter.internal.document.ByteArrayOfficeDocumentArtifact;
import org.xwiki.officeimporter.internal.document.FileOfficeDocumentArtifact;
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

    @Override
    public XHTMLOfficeDocument build(InputStream officeFileStream, String officeFileName, DocumentReference reference,
        boolean filterStyles) throws OfficeImporterException
    {
        String inputFileName = OfficeConverterFileStorage.getSafeInputFilenameFromExtension(officeFileName);

        // Invoke the office document converter.
        Map<String, InputStream> inputStreams = new HashMap<>();
        inputStreams.put(inputFileName, officeFileStream);
        // The office converter uses the output file name extension to determine the output format/syntax.
        String outputFileName = "output.html";
        OfficeConverterResult officeConverterResult;
        try {
            officeConverterResult =
                this.officeServer.getConverter().convertDocument(inputStreams, inputFileName, outputFileName);
        } catch (OfficeConverterException ex) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), ex);
        }

        // Replace the prefix "output_html" that JODConverter/LibreOffice prepend based on the output file name by
        // prefix based on the user-provided input name
        String replacePrefix = "output_html_";
        String replacementPrefix = StringUtils.substringBeforeLast(officeFileName, ".") + "_";

        Document xhtmlDoc = this.cleanAndCreateFile(reference, filterStyles, officeConverterResult,
            replacePrefix, replacementPrefix);
        Map<String, OfficeDocumentArtifact> artifacts = this.handleArtifacts(xhtmlDoc, officeConverterResult,
            replacePrefix, replacementPrefix);

        // Return a new XHTMLOfficeDocument instance.
        return new XHTMLOfficeDocument(xhtmlDoc, artifacts, officeConverterResult);
    }

    private Document cleanAndCreateFile(DocumentReference reference, boolean filterStyles,
        OfficeConverterResult officeConverterResult, String replacePrefix, String replacementPrefix)
        throws OfficeImporterException
    {
        // Prepare the parameters for HTML cleaning.
        Map<String, String> params = new HashMap<String, String>();
        params.put("targetDocument", this.entityReferenceSerializer.serialize(reference));
        // Extract the images that are embedded through the Data URI scheme and add them to the other artifacts so that
        // they end up as attachments.
        params.put("attachEmbeddedImages", "true");
        // Replace the prefix of the static output filename by the replacement based on the user-provided input
        // filename.
        params.put("replaceImagePrefix", replacePrefix);
        params.put("replacementImagePrefix", replacementPrefix);
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

    private Map<String, OfficeDocumentArtifact> handleArtifacts(Document xhtmlDoc,
        OfficeConverterResult officeConverterResult, String replacePrefix, String replacementPrefix)
    {
        Map<String, OfficeDocumentArtifact> artifacts = new HashMap<>();
        for (File file : officeConverterResult.getAllFiles()) {
            // Rename the file if it starts with the static prefix similar to the image filter.
            String filename = file.getName();
            if (StringUtils.startsWith(filename, replacePrefix)) {
                filename = replacementPrefix + StringUtils.removeStart(filename, replacePrefix);
            }
            artifacts.put(filename, new FileOfficeDocumentArtifact(file.getName(), file));
        }
        // Remove the output file from the artifacts
        artifacts.remove(officeConverterResult.getOutputFile().getName());

        @SuppressWarnings("unchecked")
        Map<String, byte[]> embeddedImages = (Map<String, byte[]>) xhtmlDoc.getUserData("embeddedImages");
        if (embeddedImages != null) {
            for (Map.Entry<String, byte[]> embeddedImage : embeddedImages.entrySet()) {
                String fileName = embeddedImage.getKey();
                artifacts.put(fileName, new ByteArrayOfficeDocumentArtifact(fileName, embeddedImage.getValue()));
            }
        }
        return artifacts;
    }
}
