/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 26 févr. 2004
 * Time: 17:30:21
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Version;

import java.util.Date;
import java.util.List;

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
