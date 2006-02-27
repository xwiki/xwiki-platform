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
 * @author sdumitriu
 */


package com.xpn.xwiki.api;

import java.util.Date;
import java.util.List;

import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

public class Attachment extends Api {
    private Document doc;
    private XWikiAttachment attachment;

    public Attachment(Document doc, XWikiAttachment attachment, XWikiContext context) {
       super(context);
       this.doc = doc;
       this.attachment = attachment;
    }

    public Document getDocument() {
        return doc;
    }

    public long getId() {
        return attachment.getId();
    }

    public long getDocId() {
        return doc.getId();
    }

    public int getFilesize() {
        return attachment.getFilesize();
    }

    public String getFilename() {
        return attachment.getFilename();
    }

    public String getAuthor() {
        return attachment.getAuthor();
    }

    public String getVersion() {
        return attachment.getVersion();
    }

    public Version getRCSVersion() {
         return attachment.getRCSVersion();
    }

    public String getComment() {
        return attachment.getComment();
    }

    public Date getDate() {
        return attachment.getDate();
    }

    public byte[] getContent() throws XWikiException {
        return attachment.getContent(context);
    }

    public Archive getArchive() {
        return attachment.getArchive();
    }

    public Version[] getVersions() {
        return attachment.getVersions();
    }

    public List getVersionList() throws XWikiException {
        return attachment.getVersionList();
    }

    public XWikiAttachment getAttachment() {
        if (checkProgrammingRights())
            return attachment;
        else
            return null;
    }

    public String getMimeType() {
        return attachment.getMimeType(context);
    }

    public boolean isImage() {
        return attachment.isImage(context);
    }
}
