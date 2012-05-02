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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
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
    private OpenOfficeManager officeManager;

    /**
     * OpenOffice HTML cleaner.
     */
    @Inject
    @Named("openoffice")
    private HTMLCleaner ooHtmlCleaner;

    @Override
    public XHTMLOfficeDocument build(InputStream officeFileStream, String officeFileName, DocumentReference reference,
        boolean filterStyles) throws OfficeImporterException
    {
        // Invoke OpenOffice document converter.
        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        inputStreams.put(officeFileName, officeFileStream);
        Map<String, byte[]> artifacts;
        // The OpenOffice converter uses the output file name extension to determine the output format/syntax.
        String outputFileName = StringUtils.substringBeforeLast(officeFileName, ".") + ".html";
        try {
            artifacts = this.officeManager.getConverter().convert(inputStreams, officeFileName, outputFileName);
        } catch (OpenOfficeConverterException ex) {
            String message = "Error while converting document [%s] into html.";
            throw new OfficeImporterException(String.format(message, officeFileName), ex);
        }

        // Prepare the parameters for HTML cleaning.
        Map<String, String> params = new HashMap<String, String>();
        params.put("targetDocument", this.entityReferenceSerializer.serialize(reference));
        if (filterStyles) {
            params.put("filterStyles", "strict");
        }

        // Parse and clean the HTML output.
        InputStream htmlStream = new ByteArrayInputStream(artifacts.remove(outputFileName));
        InputStreamReader htmlReader = null;
        Document xhtmlDoc = null;
        try {
            htmlReader = new InputStreamReader(htmlStream, "UTF-8");
            HTMLCleanerConfiguration configuration = this.ooHtmlCleaner.getDefaultConfiguration();
            configuration.setParameters(params);
            xhtmlDoc = this.ooHtmlCleaner.clean(htmlReader, configuration);
        } catch (UnsupportedEncodingException ex) {
            throw new OfficeImporterException("Error: Could not encode html office content.", ex);
        } finally {
            IOUtils.closeQuietly(htmlReader);
            IOUtils.closeQuietly(htmlStream);
        }

        // Return a new XHTMLOfficeDocument instance.
        return new XHTMLOfficeDocument(xhtmlDoc, artifacts);
    }
}
