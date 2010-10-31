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
package com.xpn.xwiki.pdf.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class PdfURLFactory extends XWikiServletURLFactory
{
    @Override
    public URL createAttachmentURL(String filename, String space, String name, String action, String querystring,
        String xwikidb, XWikiContext context)
    {
        try {
            File tempdir = (File) context.get("pdfexportdir");
            File file = new File(tempdir, space + "." + name + "." + filename);
            if (!file.exists()) {
                XWikiDocument doc = null;
                doc = context.getWiki().getDocument(new DocumentReference(context.getDatabase(), space, name), context);
                XWikiAttachment attachment = doc.getAttachment(filename);
                FileOutputStream fos = new FileOutputStream(file);
                IOUtils.copy(attachment.getContentInputStream(context), fos);
                fos.close();
            }
            return file.toURL();
        } catch (Exception e) {
            e.printStackTrace();
            return super.createAttachmentURL(filename, space, name, action, null, xwikidb, context);
        }
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String space, String name, String revision, String xwikidb,
        XWikiContext context)
    {
        try {
            File tempdir = (File) context.get("pdfexportdir");
            File file = new File(tempdir, space + "." + name + "." + filename);
            if (!file.exists()) {
                XWikiDocument doc = null;
                doc = context.getWiki().getDocument(new DocumentReference(context.getDatabase(), space, name), context);
                XWikiAttachment attachment = doc.getAttachment(filename).getAttachmentRevision(revision, context);
                FileOutputStream fos = new FileOutputStream(file);
                IOUtils.copy(attachment.getContentInputStream(context), fos);
                fos.close();
            }
            return file.toURL();
        } catch (Exception e) {
            e.printStackTrace();
            return super.createAttachmentRevisionURL(filename, space, name, revision, xwikidb, context);
        }
    }

    @Override
    public String getURL(URL url, XWikiContext context)
    {
        if (url == null) {
            return "";
        }
        return Util.escapeURL(url.toString());
    }
}
