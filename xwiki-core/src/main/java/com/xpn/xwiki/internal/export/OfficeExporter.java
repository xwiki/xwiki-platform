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
 *
 */
package com.xpn.xwiki.internal.export;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.document.DefaultDocumentFormatRegistry;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.artofsolving.jodconverter.document.DocumentFormatRegistry;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager.ManagerState;

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
    private static final Log LOG = LogFactory.getLog(OfficeExporter.class);

    /**
     * The component used to access the office document converter.
     */
    private OpenOfficeManager ooManager = Utils.getComponent(OpenOfficeManager.class);

    /**
     * The object used to get the media type corresponding to a filename extension. This object knows all the document
     * formats supported by the office converter.
     * <p>
     * Note: This is a hack! We rely on the fact that currently the office converter uses JODConverter to convert
     * from/to office document formats and so we use its {@link DocumentFormatRegistry} class. We can't expose this in
     * the office importer module because all its interfaces are independent of the underlying tool used for conversion.
     * Writing our own classes to wrap {@link DocumentFormatRegistry} and {@link DocumentFormat} is not worth at this
     * point.
     */
    private DocumentFormatRegistry documentFormatRegistry = new DefaultDocumentFormatRegistry();

    /**
     * @param format the export format; usually this is a filename extension
     * @return the export type matching the specified format, or {@code null} if the specified format is not supported
     */
    public ExportType getExportType(String format)
    {
        if (ooManager.getState() == ManagerState.CONNECTED) {
            DocumentFormat documentFormat = documentFormatRegistry.getFormatByExtension(format);
            if (documentFormat != null) {
                return new ExportType(documentFormat.getMediaType(), documentFormat.getExtension());
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PdfExportImpl#exportXHtml(String, OutputStream, ExportType, XWikiContext)
     */
    @Override
    public void exportXHtml(String xhtml, OutputStream out, ExportType type, XWikiContext context)
        throws XWikiException
    {
        exportXHtml(xhtml, out, documentFormatRegistry.getFormatByExtension(type.getExtension()), context);
    }

    /**
     * Converts the given XHTML to the specified format and writes the result to the output stream.
     * 
     * @param xhtml the XHTML to be exported
     * @param out where to write the export result
     * @param format the office format to convert the XHTML to
     * @param context the XWiki context, used to access the mapping between images embedded in the given XHTML and their
     *            location on the file system
     * @throws XWikiException if the conversion fails
     */
    private void exportXHtml(String xhtml, OutputStream out, DocumentFormat format, XWikiContext context)
        throws XWikiException
    {
        String html = applyXsl(xhtml, "officeExport.xsl");

        String inputFileName = "export_input.html";
        String outputFileName = "export_output." + format.getExtension();

        Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();
        inputStreams.put(inputFileName, new ByteArrayInputStream(html.getBytes()));
        addEmbeddedObjects(inputStreams, context);

        OpenOfficeConverter documentConverter = ooManager.getConverter();
        try {
            Map<String, byte[]> ouput = documentConverter.convert(inputStreams, inputFileName, outputFileName);

            out.write(ouput.values().iterator().next());
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, String.format(
                    "Exception while exporting to %s (%s).", format.getName(), format.getExtension()), e);
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
                LOG.warn(String.format("Failed to embed %s in the office export.", file.getName()), e);
            }
        }
    }
}
