/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 00:00:31
 */
package com.xpn.xwiki.doc;

import org.apache.commons.jrcs.rcs.*;
import org.apache.commons.jrcs.diff.DiffException;
import org.apache.tools.ant.filters.StringInputStream;

import java.util.Date;
import java.io.FileNotFoundException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;


public class XWikiSimpleDoc extends XWikiDefaultDoc {
    private String parent;
    private String web;
    private String name;
    private String content;
    private String meta;
    private String format;
    private String author;
    private Archive archive;
    private Date date;
    private Version version;
    private long id = 0;
    private boolean mostRecent = false;

    // Used to make sure the MetaData String is regenerated
    private boolean isContentDirty = false;
    // Used to make sure the MetaData String is regenerated
    private boolean isMetaDataDirty = false;

    public long getId() {
        if (id==0) {
            id = getFullName().hashCode();
        }
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getVersion() {
        return getRCSVersion().toString();
    }

    public void setVersion(String version) {
        this.version = new Version(version);
    }

    public Version getRCSVersion() {
        if (version == null) {
            version = new Version("1.1");
        }
        return version;
    }

    public void setRCSVersion(Version version) {
        this.version = version;
    }

    public XWikiSimpleDoc() {
        this("Main", "WebHome");
    }

    public XWikiSimpleDoc(String web, String name) {
        this.web = web;
        this.name = name;
        this.date = new Date();
        this.parent = "";
        this.content = "";
        this.format = "";
        this.author = "";
        this.archive = null;
    }

    public XWikiDocInterface getParentDoc() {
        return new XWikiSimpleDoc(web, parent);
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        if (!parent.equals(this.parent)) {
            setMetaDataDirty(true);
        }
        this.parent = parent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (!content.equals(this.content)) {
         setContentDirty(true);
        }
        this.content = content;
    }

    public String getRenderedContent(XWikiContext context) {
        return context.getWiki().getRenderingEngine().renderDocument(this, context);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
     StringBuffer buf = new StringBuffer();
     buf.append(getWeb());
     buf.append(".");
     buf.append(getName());
     return buf.toString();
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
        if (!format.equals(this.format)) {
            setMetaDataDirty(true);
        }
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        if (!author.equals(this.author)) {
            setMetaDataDirty(true);
        }
        this.author = author;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        if (!date.equals(this.date)) {
            setMetaDataDirty(true);
        }
        this.date = date;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        if (!meta.equals(this.meta)) {
            setMetaDataDirty(true);
        }
        this.meta = meta;
    }

    public void appendMeta(String meta) {
     StringBuffer buf = new StringBuffer(this.meta);
     buf.append(meta);
     buf.append("\n");
     this.meta = buf.toString();
     setMetaDataDirty(true);
    }

    public boolean isContentDirty() {
        return isContentDirty;
    }

    public void incrementVersion() {
        if (version==null)
            version = new Version("1.1");
        else {
         version = version.next();
        }
    }

    public void setContentDirty(boolean contentDirty) {
        isContentDirty = contentDirty;
    }

    public boolean isMetaDataDirty() {
        return isMetaDataDirty;
    }

    public void setMetaDataDirty(boolean metaDataDirty) {
        isMetaDataDirty = metaDataDirty;
    }

    public String getActionUrl(String action, XWikiContext context) {
        StringBuffer url = new StringBuffer();
        url.append(context.getWiki().getBase());
        url.append(action);
        url.append("/");
        url.append(getWeb());
        url.append("/");
        url.append(getName());
        return url.toString();
    }

    public String getViewUrl(XWikiContext context) {
        return getActionUrl("view", context);
    }

    public String getEditUrl(XWikiContext context) {
        return getActionUrl("edit", context);
    }

    public String getPreviewUrl(XWikiContext context) {
        return getActionUrl("preview", context);
    }

    public String getSaveUrl(XWikiContext context) {
        return getActionUrl("save", context);
    }

    public String getAttachUrl(XWikiContext context) {
        return getActionUrl("attach", context);
    }

    public Version[] getRevisions() throws XWikiException {
        return getStore().getXWikiDocVersions(this);
    }

    public String[] getRecentRevisions() throws XWikiException {
        Version[] revisions = getStore().getXWikiDocVersions(this);
        int length = 5;
        if (revisions.length<5)
          length = revisions.length;

        String[] recentrevs = new String[length];
        for (int i = 1; i <= length; i++)
            recentrevs[i-1
                    ] = revisions[revisions.length-i].toString();
        return recentrevs;
    }

    public Archive getRCSArchive() {
        return archive;
    }

    public void setRCSArchive(Archive archive) {
        this.archive = archive;
    }

    public String getArchive() throws XWikiException {
        if (archive==null)
            updateArchive(content);
        if (archive==null)
            return "";
        else {
            StringBuffer buffer = new StringBuffer();
            archive.toString(buffer);
            return buffer.toString();
        }
    }

    public void setArchive(String text) throws XWikiException {
        try {
            StringInputStream is = new StringInputStream(text);
            archive = new Archive(getFullName(), is);
        }
        catch (Exception e) {
                   Object[] args = { getFullName() };
                   throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                           "Exception while manipulating the archive for doc {0}", e, args);
               }
       }

    public void updateArchive(String text) throws XWikiException {
        try {
        Lines lines = new Lines(text);
        if (archive!=null)
             archive.addRevision(lines.toArray(),"");
        else
             archive = new Archive(lines.toArray(),getFullName(),getVersion());
        }
        catch (Exception e) {
            Object[] args = { getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for doc {0}", e, args);
        }
    }

    public boolean isMostRecent() {
        return mostRecent;
    }

    public void setMostRecent(boolean mostRecent) {
        this.mostRecent = mostRecent;
    }

}
