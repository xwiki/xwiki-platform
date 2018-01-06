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
package com.xpn.xwiki.web;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.pdf.api.PdfExport.ExportType;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;

/**
 * Exports a document as PDF.
 *
 * @deprecated Use {@link ExportAction}.
 * @version $Id$
 */
@Deprecated
public class PDFAction extends XWikiAction
{
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiURLFactory urlf =
            context.getWiki().getURLFactoryService().createURLFactory(XWikiContext.MODE_PDF, context);
        context.setURLFactory(urlf);
        PdfExportImpl pdfexport = new PdfExportImpl();
        XWikiDocument doc = context.getDoc();
        handleRevision(context);

        try {
            context.getResponse().setContentType(ExportType.PDF.getMimeType());

            // Compute the name of the export. Since it's gong to be saved on the user's file system it needs to be a
            // valid File name. Thus we use the "path" serializer but replace the "/" separator by "_" since we're not
            // computing a directory hierarchy but a file name
            EntityReferenceSerializer<String> serializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "path");
            String filename = serializer.serialize(doc.getDocumentReference()).replaceAll("/", "_");
            // Make sure we don't go over 255 chars since several filesystems don't support filename longer than that!
            filename = StringUtils.abbreviateMiddle(filename, "__", 255);

            context.getResponse().addHeader("Content-disposition",
                String.format("inline; filename=%s.%s", filename, ExportType.PDF.getExtension()));

            pdfexport.export(doc, context.getResponse().getOutputStream(), ExportType.PDF, context);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while sending response", e);
        }
        return null;
    }
}
