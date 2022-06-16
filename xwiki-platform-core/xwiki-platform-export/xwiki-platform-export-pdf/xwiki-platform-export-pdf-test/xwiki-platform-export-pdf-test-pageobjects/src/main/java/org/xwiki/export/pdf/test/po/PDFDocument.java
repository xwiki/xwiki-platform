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
package org.xwiki.export.pdf.test.po;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Utility class to verify the content of a PDF document.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5RC1
 */
public class PDFDocument implements AutoCloseable
{
    private final PDDocument document;

    /**
     * Fetches and parses a PDF document from a given URL.
     * 
     * @param url where to fetch the PDF document from
     * @throws IOException if fetching and parsing the PDF document fails
     */
    public PDFDocument(URL url) throws IOException
    {
        this.document = PDDocument.load(IOUtils.toByteArray(url));
    }

    @Override
    public void close() throws Exception
    {
        this.document.close();
    }

    /**
     * @return the number of pages
     */
    public int getNumberOfPages()
    {
        return this.document.getNumberOfPages();
    }
}
