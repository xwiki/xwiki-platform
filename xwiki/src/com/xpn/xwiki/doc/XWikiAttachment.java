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

 * Created by
 * User: Ludovic Dubost
 * Date: 28 janv. 2004
 * Time: 22:43:54
 */
package com.xpn.xwiki.doc;

import org.apache.commons.jrcs.rcs.*;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.dom.DOMElement;
import java.util.*;

import com.xpn.xwiki.XWikiException;
import net.sf.hibernate.ObjectNotFoundException;

public class XWikiAttachment {
    private XWikiDocInterface doc;

    private int filesize;
    private String filename;
    private String author;
    private Version version;
    private String comment;
    private Date date;
    // Meta Data Archive
    private Archive metaArchive;

    private XWikiAttachmentContent attachment_content;
    private XWikiAttachmentArchive attachment_archive;
    private boolean isMetaDataDirty = false;

    public XWikiAttachment(XWikiSimpleDoc doc, String filename) {
        this();
        setDoc(doc);
        setFilename(filename);
    }

    public XWikiAttachment() {
        filesize = 0;
        filename = "";
        author = "";
        comment = "";
        date = new Date();
    }

    public long getId() {
        if (doc==null)
         return filename.hashCode();
        else
         return (doc.getFullName() + "/" + filename).hashCode();
    }

    public void setDocId(long id) {
    }

    public long getDocId() {
        return doc.getId();
    }

    public void setId(long id) {
    }



    public int getFilesize() {
        return filesize;
    }

    public void setFilesize(int filesize) {
        if (filesize != this.filesize) {
                setMetaDataDirty(true);
            }
        this.filesize = filesize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (!filename.equals(this.filename)) {
                setMetaDataDirty(true);
            }
        this.filename = filename;
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

    public String getComment() {
        if (!comment.equals(this.comment)) {
                setMetaDataDirty(true);
            }
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public XWikiDocInterface getDoc() {
        return doc;
    }

    public void setDoc(XWikiDocInterface doc) {
        this.doc = doc;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


    public boolean isContentDirty() {
        if (attachment_content==null)
         return false;
        else
         return attachment_content.isContentDirty();
    }

    public void incrementVersion() {
        if (version==null)
            version = new Version("1.1");
        else {
            version = version.next();
        }
    }

    public boolean isMetaDataDirty() {
        return isMetaDataDirty;
    }

    public void setMetaDataDirty(boolean metaDataDirty) {
        isMetaDataDirty = metaDataDirty;
    }

    /*
    // This code should not be needed..
    // Meta Data archiving is done
    // in the main document..
    public Archive getRCSMetaArchive() {
        return metaArchive;
    }

    public void setRCSMetaArchive(Archive metaArchive) {
        this.metaArchive = metaArchive;
    }


    public String getMetaArchive() throws XWikiException {
        if (metaArchive==null)
            updateMetaArchive(toXML());
        if (metaArchive==null)
            return "";
        else {
            StringBuffer buffer = new StringBuffer();
            metaArchive.toString(buffer);
            return buffer.toString();
        }
    }

    public void setMetaArchive(String text) throws XWikiException {
        try {
            StringInputStream is = new StringInputStream(text);
            metaArchive = new Archive(getFilename(), is);
        }
        catch (Exception e) {
            Object[] args = { getFilename() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for file {0}", e, args);
        }
    }

    public void updateMetaArchive(String text) throws XWikiException {
        try {
            Lines lines = new Lines(text);
            if (metaArchive!=null)
                metaArchive.addRevision(lines.toArray(),"");
            else
                metaArchive = new Archive(lines.toArray(),getFilename(),getVersion());
        }
        catch (Exception e) {
            Object[] args = { getFilename() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for file {0}", e, args);
        }
    }
    */

    public Element toXML() {
        Element docel = new DOMElement("attachment");
        Element el = new DOMElement("filename");
        el.addText(getFilename());
        docel.add(el);

        el = new DOMElement("filesize");
        el.addText("" + getFilesize());
        docel.add(el);

        el = new DOMElement("author");
        el.addText(getAuthor());
        docel.add(el);

        long d = getDate().getTime();
        el = new DOMElement("date");
        el.addText("" + d);
        docel.add(el);

        el = new DOMElement("version");
        el.addText(getVersion());
        docel.add(el);

        el = new DOMElement("comment");
        el.addText(getComment());
        docel.add(el);
        return docel;
    }

    public void fromXML(Element docel) throws DocumentException, java.text.ParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        setFilename(docel.element("filename").getText());
        setFilesize(Integer.parseInt(docel.element("filesize").getText()));
        setAuthor(docel.element("author").getText());
        setVersion(docel.element("version").getText());
        setComment(docel.element("comment").getText());

        String sdate = docel.element("date").getText();
        Date date = new Date(Long.parseLong(sdate));
        setDate(date);
    }

    public XWikiAttachmentContent getAttachment_content() {
        return attachment_content;
    }

    public void setAttachment_content(XWikiAttachmentContent attachment_content) {
        this.attachment_content = attachment_content;
    }

    public XWikiAttachmentArchive getAttachment_archive() {
        return attachment_archive;
    }

    public void setAttachment_archive(XWikiAttachmentArchive attachment_archive) {
        this.attachment_archive = attachment_archive;
    }

    public byte[] getContent() throws XWikiException {
        if (attachment_content==null) {
            doc.loadAttachmentContent(this);
        }

        return attachment_content.getContent();
    }

    public Archive getArchive() {
        if (attachment_archive==null)
            return null;
        else
            return attachment_archive.getRCSArchive();
    }

    public Version[] getVersions() {
        Node[] nodes = getArchive().changeLog();
        Version[] versions = new Version[nodes.length];
        for (int i=0;i<nodes.length;i++) {
            versions[i] = nodes[i].getVersion();
        }
        return versions;
    }

    // We assume versions go from 1.1 to the current one
    // This allows not to read the full archive file
    public List getVersionList() throws XWikiException {
            List list = new ArrayList();
            Version v = new Version("1.1");
            while (true) {
                list.add(v);
                if (v.toString().equals(version.toString()))
                    break;
                v.next();
            }
            return list;
    }

    public void setContent(byte[] data) {
        if (attachment_content==null) {
            attachment_content = new XWikiAttachmentContent();
            attachment_content.setAttachment(this);
        }
        attachment_content.setContent(data);
    }

    public void updateContentArchive() throws XWikiException {
        if (attachment_content == null)
         return;

        if (attachment_archive==null) {
            try {
             getDoc().getStore().loadAttachmentArchive(this, true);
            } catch (XWikiException e) {
                if (!(e.getException() instanceof ObjectNotFoundException))
                    throw e;
            }
        }

        if (attachment_archive==null) {
            attachment_archive = new XWikiAttachmentArchive();
            attachment_archive.setAttachment(this);
        }

        attachment_archive.updateArchive(getContent());
    }

}

