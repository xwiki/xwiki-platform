/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author wr0ngway
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;

import java.io.IOException;

public class PDFAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiURLFactory urlf = context.getWiki().getURLFactoryService().createURLFactory(XWikiContext.MODE_PDF, context);
        context.setURLFactory(urlf);
        PdfExportImpl pdfexport = new PdfExportImpl();
        XWikiDocument doc = context.getDoc();
        handleRevision(context);
            
        try {
         context.getResponse().setContentType("application/pdf");
         context.getResponse().addHeader("Content-disposition", "inline; filename=" + Utils.encode(doc.getSpace(), context) + "_" + Utils.encode(doc.getName(), context) + ".pdf");

         pdfexport.exportToPDF(doc, context.getResponse().getOutputStream(), context);
        } catch (IOException e) {
           throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response", e);
        }
        return null;
	}
}
