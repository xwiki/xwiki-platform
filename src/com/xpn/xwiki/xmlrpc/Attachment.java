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
 * Date: 18 juin 2004
 * Time: 11:21:56
 */
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Date;
import java.util.Hashtable;

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
