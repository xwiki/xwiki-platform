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

package com.xpn.xwiki.xmlrpc;

import java.util.Date;
import java.util.Hashtable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class Attachment {
    private String id;
    private String pageId;
    private String title;
    private String fileName;
    private String fileSize;
    private String contentType;
    private Date created;
    private String creator;
    private String url;

    public Attachment(XWikiDocument doc, XWikiAttachment attachment, XWikiContext context) {
        setId(attachment.getFilename());
        setPageId(doc.getFullName());
        setTitle(attachment.getComment());
        setFileName(attachment.getFilename());
        setFileSize("" + attachment.getFilesize());
        setCreator(attachment.getAuthor());
        setCreated(attachment.getDate());
        setContentType(context.getWiki().getEngineContext().getMimeType(attachment.getFilename()));
        setUrl(doc.getAttachmentURL(attachment.getFilename(), "download", context));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Hashtable getHashtable() {
        Hashtable ht = new Hashtable();
        ht.put("id", getId());
        ht.put("pageId", getPageId());
        ht.put("title", getTitle());
        ht.put("fileName", getFileName());
        ht.put("fileSize", getFileSize());
        ht.put("contentType", (getContentType()!=null) ? getContentType() : "");
        ht.put("created", getCreated());
        ht.put("creator", getCreator());
        ht.put("url", getUrl());
        return ht;
    }
}
