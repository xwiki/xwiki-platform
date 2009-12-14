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

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xwiki.model.DocumentName;
import org.xwiki.model.DocumentNameSerializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.openoffice.OpenOfficeDocumentConverter;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

/**
 * Default implementation of {@link XHTMLOfficeDocumentBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component
public class DefaultXHTMLOfficeDocumentBuilder extends AbstractLogEnabled implements XHTMLOfficeDocumentBuilder
{
    /**
     * Used to serialize the reference document name.
     */
    @Requirement
    private DocumentNameSerializer nameSerializer;

    /**
     * Document converter used to invoke openoffice server and convert office documents.
     */
    @Requirement
    private OpenOfficeDocumentConverter documentConverter;

    /**
     * OpenOffice html cleaner.
     */
    @Requirement("openoffice")
    private HTMLCleaner ooHtmlCleaner;

    /**
     * {@inheritDoc}
     * @since 2.2M1
     */
    public XHTMLOfficeDocument build(byte[] officeFileData, DocumentName reference, boolean filterStyles)
        throws OfficeImporterException
    {
        // Prepare the parameters for html cleaning.
        Map<String, String> params = new HashMap<String, String>();
        params.put("targetDocument", nameSerializer.serialize(reference));
        if (filterStyles) {
            params.put("filterStyles", "strict");
        }

        // Invoke openoffice document converter.
        Map<String, byte[]> artifacts = documentConverter.convert(officeFileData);

        // Parse and clean the html output.
        InputStream htmlStream = new ByteArrayInputStream(artifacts.remove("output.html"));
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

    /**
     * {@inheritDoc}
     * @deprecated use {@link #build(byte[], org.xwiki.model.DocumentName, boolean)} since 2.2M1
     */
    @Deprecated
    public XHTMLOfficeDocument build(byte[] officeFileData, org.xwiki.bridge.DocumentName reference,
        boolean filterStyles) throws OfficeImporterException
    {
        return build(officeFileData, new DocumentName(reference.getWiki(), reference.getSpace(), reference.getPage()),
            filterStyles);
    }
}
