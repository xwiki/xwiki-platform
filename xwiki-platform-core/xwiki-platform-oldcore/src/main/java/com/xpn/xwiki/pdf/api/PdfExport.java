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
package com.xpn.xwiki.pdf.api;

import java.io.OutputStream;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * PDF Exporter, converts a wiki {@link XWikiDocument Document} into PDF.
 * 
 * @version $Id$
 */
public interface PdfExport
{
    /** Describes export types. */
    static class ExportType
    {
        /** Export type: PDF. */
        public static final ExportType PDF = new ExportType("application/pdf", "pdf");

        /** Export type: RTF. */
        public static final ExportType RTF = new ExportType("application/rtf", "rtf");

        /** The MIME type corresponding to this export type. */
        private final String mimeType;

        /** The file extension corresponding to this export type. */
        private final String extension;

        /**
         * Constructor, specifying the target MIME type and file extension.
         * 
         * @param mimeType the standard MIME type for this export type
         * @param extension the filename extension for this export type
         */
        public ExportType(String mimeType, String extension)
        {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        /**
         * @return the export content type
         */
        public String getMimeType()
        {
            return mimeType;
        }

        /**
         * @return the filename extension corresponding to this export type
         */
        public String getExtension()
        {
            return extension;
        }
    }

    /**
     * Export a wiki Document into PDF. See
     * {@link PdfExport#export(XWikiDocument, OutputStream, ExportType, XWikiContext)} for more details about the
     * conversion process.
     * 
     * @param doc the document to export
     * @param out where to write the resulting document
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     * @see PdfExport#export(XWikiDocument, OutputStream, ExportType, XWikiContext)
     */
    void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException;

    /**
     * Export a wiki Document into PDF or RTF. The content of the document is rendered into HTML using the
     * {@code pdf.vm} template, the resulting HTML is cleaned up into valid XHTML using JTidy, and custom CSS is applied
     * to it. The XHTML document is transformed into an XSL-FO document, which is finally processed using Apache FOP.
     * 
     * @param doc the document to export
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     */
    void export(XWikiDocument doc, OutputStream out, ExportType type, XWikiContext context) throws XWikiException;

    /**
     * Convert an HTML document to PDF. The HTML is cleaned up, and CSS style is applied to it.
     * 
     * @param html the source document to transform
     * @param out where to write the resulting document
     * @param type the type of the output: PDF or RTF
     * @param context the current request context
     * @throws XWikiException if the conversion fails for any reason
     */
    void exportHtml(String html, OutputStream out, ExportType type, XWikiContext context) throws XWikiException;
}
