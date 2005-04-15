/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 20 août 2004
 * Time: 21:27:00
 */
package com.xpn.xwiki.pdf.impl;

import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

public class PdfURLFactory extends XWikiServletURLFactory {

    public PdfURLFactory() {
    }

    public URL createAttachmentURL(String filename, String web, String name, String action, String xwikidb, XWikiContext context) {
        try {
            File tempdir = (File) context.get("pdfexportdir");
            File file = new File(tempdir, web + "." + name + "." + filename);
            if (!file.exists()) {
                XWikiDocument doc = null;
                doc = context.getWiki().getDocument(web + "." + name, context);
                XWikiAttachment attachment = doc.getAttachment(filename);
                byte[] data = new byte[0];
                data = attachment.getContent(context);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            }
            return file.toURL();
        } catch (Exception e) {
            e.printStackTrace();
            return super.createAttachmentURL(filename, web, name, action, xwikidb, context);
        }
    }

    public PdfURLFactory(URL serverURL, String servletPath, String actionPath) {
        super(serverURL, servletPath, actionPath);
    }

    public PdfURLFactory(XWikiContext context) {
        super(context);
    }

    public String getURL(URL url, XWikiContext context) {
        if (url==null)
            return "";
        return url.toString();
    }
}
