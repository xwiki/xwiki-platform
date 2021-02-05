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
package com.xpn.xwiki.internal.export;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.converter.OfficeDocumentFormat;
import org.xwiki.officeimporter.server.OfficeServer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.web.Utils;

/**
 * Exports a wiki document as an office document.
 * <p>
 * Note: We extend {@link PdfExportImpl} for convenience. This is just a temporary solution. The entire export code
 * needs to be redesigned and moved in a separate module.
 *
 * @version $Id$
 * @since 3.1M1
 */
public class OfficeExporter extends PdfExportImpl
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OfficeExporter.class);

    /**
     * The component used to access the office document converter.
     */
    private OfficeServer officeServer = Utils.getComponent(OfficeServer.class);

    /**
     * @param fileName the output file name, which specifies the export format (e.g. pdf, odt)
     * @return the export type matching the specified format, or {@code null} if the specified format is not supported
     */
    public ExportType getExportType(String fileName)
    {
        if (this.officeServer.getState() == OfficeServer.ServerState.CONNECTED) {
            OfficeDocumentFormat format =
                this.officeServer.getConverter().getDocumentFormat(fileName);
            if (format != null) {
                return new ExportType(format.getMediaType(), format.getExtension());
            }
        }
        return null;
    }

    @Override
    protected void exportXHTML(String xhtml, OutputStream out, ExportType type, XWikiContext context)
        throws XWikiException
    {
        String html = applyXSLT(xhtml, getOfficeExportXSLT(context));

        String inputFileName = "export_input.html";
        String outputFileName = "export_output." + type.getExtension();

        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        // We assume that the HTML was generated using the XWiki encoding.
        Charset charset = Charset.forName(context.getWiki().getEncoding());
        inputStreams.put(inputFileName, new ByteArrayInputStream(html.getBytes(charset)));
        addEmbeddedObjects(inputStreams, context);

        OfficeConverter officeConverter = this.officeServer.getConverter();
        try (OfficeConverterResult officeConverterResult =
                officeConverter.convertDocument(inputStreams, inputFileName, outputFileName))
        {

            for (File file : officeConverterResult.getAllFiles()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    IOUtils.write(IOUtils.toByteArray(fis), out);
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, String.format(
                    "Exception while exporting to %s (%s).", type.getMimeType(), type.getExtension()), e);
        }
    }

    /**
     * Adds the objects referenced in the exported XHTML to the map of input streams, allowing them to be embedded in
     * the output office document.
     *
     * @param inputStreams the map of input streams that is passed to the office converter
     * @param context the XWiki context, used to access the mapping between images embedded in the given XHTML and their
     *            location on the file system
     */
    private void addEmbeddedObjects(Map<String, InputStream> inputStreams, XWikiContext context)
    {
        @SuppressWarnings("unchecked")
        Map<String, File> fileMapping = (Map<String, File>) context.get("pdfexport-file-mapping");
        for (File file : fileMapping.values()) {
            try {
                // Embedded files are placed in the same folder as the HTML input file during office conversion.
                inputStreams.put(file.getName(), new FileInputStream(file));
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to embed %s in the office export.", file.getName()), e);
            }
        }
    }

    /**
     * Get the XSLT for preparing a (valid) XHTML to be converted to an office format.
     *
     * @param context the current request context
     * @return the content of the XSLT as a byte stream
     * @see PdfExportImpl#getXslt(String, String, XWikiContext)
     */
    private InputStream getOfficeExportXSLT(XWikiContext context)
    {
        return getXslt("officeExportXSLT", "officeExport.xsl", context);
    }
}
