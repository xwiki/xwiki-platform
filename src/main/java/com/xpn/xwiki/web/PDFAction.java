package com.xpn.xwiki.web;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.pdf.impl.PdfURLFactory;

public class PDFAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        context.setURLFactory(new PdfURLFactory(context));
        PdfExportImpl pdfexport = new PdfExportImpl();
        XWikiDocument doc = context.getDoc();
        handleRevision(context);
            
        try {
         context.getResponse().setContentType("application/pdf");
         context.getResponse().addHeader("Content-disposition", "attachment; filename=" + doc.getWeb() + "_" + doc.getName() + ".pdf");

         pdfexport.exportToPDF(doc, context.getResponse().getOutputStream(), context);
        } catch (IOException e) {
           throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response", e);
        }
        return null;
	}
}
