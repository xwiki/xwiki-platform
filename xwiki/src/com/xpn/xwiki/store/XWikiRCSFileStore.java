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
 * Time: 01:00:44
 */
package com.xpn.xwiki.store;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.util.Util;
import org.apache.commons.jrcs.rcs.*;

import java.io.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class XWikiRCSFileStore extends XWikiDefaultStore {
    private File rcspath;
    private File rcsattachmentpath;

    public XWikiRCSFileStore() {
    }


    public XWikiRCSFileStore(XWiki xwiki, XWikiContext context) {
        String rcspath = xwiki.ParamAsRealPath("xwiki.store.rcs.path", context);
        setPath(rcspath);
        String rcsattachementpath = xwiki.ParamAsRealPath("xwiki.store.rcs.attachmentpath", context);
        setAttachmentPath(rcsattachementpath);
    }

    public XWikiRCSFileStore(String rcspath, String rcsattachmentpath) {
        setPath(rcspath);
        setAttachmentPath(rcsattachmentpath);
    }

    public void setPath(String rcspath) {
        this.rcspath = new File(rcspath);
    }

    public String getPath() {
        return rcspath.toString();
    }

    public void setAttachmentPath(String rcsattachmentpath) {
        this.rcsattachmentpath = new File(rcsattachmentpath);
    }

    public String getAttachmentPath() {
        return rcsattachmentpath.toString();
    }

    public File getFilePath(XWikiDocInterface doc, XWikiContext context) {
        File webdir = new File(getPath(), doc.getWeb().replace('.','/'));
        webdir.mkdirs();
        return new File(webdir, doc.getName() + ".txt");
    }

    public File getVersionedFilePath(XWikiDocInterface doc, XWikiContext context) {
        File webfile = new File(getPath(), doc.getWeb().replace('.','/'));
        return new File(webfile, doc.getName() + ".txt,v");
    }

    public File getAttachmentPath(XWikiAttachment attachment, XWikiContext context) {
        File webdir = new File(getAttachmentPath(), attachment.getDoc().getWeb().replace('.','/')
                + "/" + attachment.getDoc().getName());
        webdir.mkdirs();
        return new File(webdir, attachment.getFilename());
    }

    public File getVersionedAttachmentPath(XWikiAttachment attachment, XWikiContext context) {
        File webdir = new File(getAttachmentPath(), attachment.getDoc().getWeb().replace('.','/')
                + "/" + attachment.getDoc().getName());
        webdir.mkdirs();
        return new File(webdir, attachment.getFilename() + ",v");
    }

    public void saveXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        try {
            doc.setStore(this);
            // Handle the latest text file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                doc.setDate(new Date());
                doc.incrementVersion();
            }
            File file = getFilePath(doc, context);
            FileWriter wr = new FileWriter(file);
            wr.write(getFullContent(doc));
            wr.flush();
            wr.close();

            // Now handle the versioned file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                File vfile = getVersionedFilePath(doc, context);
                Archive archive;
                Lines lines = new Lines(getFullContent(doc));
                try {
                    // Let's try to read the archive
                    archive = new Archive(vfile.toString());
                    archive.addRevision(lines.toArray(),"");
                } catch (FileNotFoundException e) {
                    // If we cannot find the file let's create a new archive
                    archive = new Archive(lines.toArray(),doc.getFullName(),doc.getVersion());
                }
                // Save back the archive
                doc.setRCSArchive(archive);
                doc.getRCSArchive().save(vfile.toString());
            }
            doc.setMostRecent(true);
            doc.setNew(false);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SAVING_FILE,
                    "Exception while saving document {0}", e, args);
        }
    }

    public void saveXWikiDoc(XWikiDocInterface doc, XWikiContext context, boolean bTransaction) throws XWikiException {
        saveXWikiDoc(doc, context);
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        BufferedReader fr = null;
        try {
            doc.setStore(this);
            File file = getFilePath(doc, context);

            if (!file.exists()) {
                doc.setNew(true);
                return doc;
            }

            doc.setNew(false);
            StringBuffer content = new StringBuffer();
            fr = new BufferedReader(new FileReader(file));
            String line;
            boolean bMetaDataDone = false;
            boolean bisXML = false;
            line = fr.readLine();
            if (line.startsWith("<"))
                bisXML = true;

            if (bisXML) {
                while (true) {
                    if (line==null) {
                        fr.close();
                        doc.fromXML(content.toString());
                        break;
                    }
                    content.append(line);
                    content.append("\n");
                    line = fr.readLine();
                }

            } else
            {
                while (true) {
                    if (line==null) {
                        fr.close();
                        doc.setContent(content.toString());
                        doc.setMostRecent(true);
                        break;
                    }
                    if (bMetaDataDone||(parseMetaData(doc,line)==false)) {
                        content.append(line);
                        content.append("\n");
                    }
                    line = fr.readLine();
                }
            }
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_READING_FILE,
                    "Exception while reading document {0}", e, args);
        }
        return doc;
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface basedoc,String version, XWikiContext context) throws XWikiException {
        XWikiDocInterface doc = new XWikiSimpleDoc(basedoc.getWeb(), basedoc.getName());
        try {
            doc.setStore(this);
            Archive archive = basedoc.getRCSArchive();

            if (archive==null) {
                File file = getVersionedFilePath(doc, context);
                String path = file.toString();
                synchronized (path) {
                    archive = new Archive(path);
                }
            }
            basedoc.setRCSArchive(archive);

            Object[] text = (Object[]) archive.getRevision(version);
            if (text[0].toString().startsWith("<")) {
                StringBuffer content = new StringBuffer();
                for (int i=0;i<text.length;i++) {
                    String line = text[i].toString();
                    content.append(line);
                    content.append("\n");
                }
                doc.fromXML(content.toString());
            } else {
                StringBuffer content = new StringBuffer();
                boolean bMetaDataDone = false;
                for (int i=0;i<text.length;i++) {
                    String line = text[i].toString();
                    if (bMetaDataDone||(parseMetaData(doc,line)==false)) {
                        content.append(line);
                        content.append("\n");
                    }
                    doc.setContent(content.toString());
                }

                // Make sure the document has the same name
                // as the new document (in case there was a name change
                doc.setName(basedoc.getName());
                doc.setWeb(basedoc.getWeb());
            }
        } catch (Exception e) {
            Object[] args = { doc.getFullName(), version.toString() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        }
        return doc;
    }

    public void deleteXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        try {
            doc.setStore(this);
            File file = getFilePath(doc, context);
            List attachlist = doc.getAttachmentList();
            for (int i=0;i<attachlist.size();i++) {
                XWikiAttachment attachment = (XWikiAttachment) attachlist.get(i);
                deleteXWikiAttachment(attachment, context, false);
            }
            file.delete();
        } catch (Exception e) {
            Object[] args = { doc.getFullName()};
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_DELETING_FILE,
                    "Exception while deleting document {0} ", e, args);
        }
    }

    public Version[] getXWikiDocVersions(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        try {
            doc.setStore(this);
            File vfile = getVersionedFilePath(doc, context);
            String path = vfile.toString();
            synchronized (path) {
                Archive archive;
                try {
                    archive= new Archive(path);
                } catch (Exception e) {
                    File file = getFilePath(doc, context);
                    if (file.exists()) {
                        Version[] versions = new Version[1];
                        versions[0] = new Version("1.1");
                        return versions;
                    }
                    else {
                        Object[] args = { doc.getFullName() };
                        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_FILENOTFOUND,
                                "File {0} does not exist", e, args);
                    }
                }
                Node[] nodes = archive.changeLog();
                Version[] versions = new Version[nodes.length];
                for (int i=0;i<nodes.length;i++) {
                    versions[i] = nodes[i].getVersion();
                }
                return versions;
            }
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_READING_REVISIONS,
                    "Exception while reading document {0} revisions", e, args);
        }
    }

    public String getFullContent(XWikiDocInterface doc) {
        return doc.toXML();
    }

    public void getFullContent(XWikiDocInterface doc, StringBuffer buf) {
        getMetaData(doc, buf);
        getContent(doc, buf);
    }

    public void getContent(XWikiDocInterface doc, StringBuffer buf) {
        buf.append(doc.getContent());
    }

    public void getMetaData(XWikiDocInterface doc, StringBuffer buf) {
        buf.append("%META:TOPICINFO{");
        addField(buf, "author", doc.getAuthor());
        addField(buf, "date", "" + doc.getDate().getTime());
        addField(buf, "version", doc.getVersion().toString());
        addField(buf, "format", doc.getFormat());
        buf.append("}%\n");
        buf.append("%META:TOPICPARENT{");
        addField(buf, "name", doc.getParent());
        buf.append("}%\n");
        String meta = doc.getMeta();
        if (meta!=null)
            buf.append(doc.getMeta());
    }

    public void addField(StringBuffer buf,String name, String value) {
        buf.append(name);
        buf.append("=\"");
        value = (value==null) ? "" : value;
        buf.append(Util.cleanValue(value));
        buf.append("\" ");
    }

    public static boolean parseMetaData(XWikiDocInterface doc, String line) throws IOException {
        if (!line.startsWith("%META:"))
            return false;

        if (line.startsWith("%META:TOPICINFO{")) {
            String line2 = line.substring(16, line.length() - 2);
            Hashtable params = Util.keyValueToHashtable(line2);
            Object author = params.get("author");
            if (author!=null)
                doc.setAuthor((String)author);
            Object format = params.get("format");
            if (format!=null)
                doc.setFormat((String)format);
            Object version = params.get("version");
            if (version!=null)
                doc.setVersion((String) version);
            Object date = params.get("date");
            if (date!=null) {
                long l = Long.parseLong((String)date);
                doc.setDate(new Date(l));
            }
        } else if (line.startsWith("%META:TOPICPARENT{")) {
            String line2 = line.substring(18, line.length() - 2);
            Hashtable params = Util.keyValueToHashtable(line2);
            Object parent = params.get("name");
            if (parent!=null)
                doc.setParent((String)parent);
        } else {
            doc.appendMeta(line);
        }

        return true;
    }

    public List getClassList(XWikiContext context) throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Not implemented");
    }

    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException {
        return searchDocuments(wheresql,0,0, context);
    }

    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Not implemented");
    }

    public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            File file = getAttachmentPath(attachment, context);
            if (file.exists())
                file.delete();
            file = getVersionedAttachmentPath(attachment, context);
            if (file.exists())
                file.delete();
            if (bTransaction) {
                List list = attachment.getDoc().getAttachmentList();
                for (int i=0;i<list.size();i++) {
                    XWikiAttachment attach = (XWikiAttachment) list.get(i);
                    if (attachment.getFilename().equals(attach.getFilename())) {
                        list.remove(i);
                        break;
                    }
                }
                saveXWikiDoc(attachment.getDoc(), context, false);
            }
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_DELETING_ATTACHMENT,
                    "Exception while deleting attachment {0} from document {1}", e, args);
        }
    }

    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            File file = getAttachmentPath(attachment, context);
            FileOutputStream os = new FileOutputStream(file);
            os.write(attachment.getContent(context));
            os.flush();
            os.close();

            // Now handle the versioned file
            if (attachment.isContentDirty()) {
                File vfile = getVersionedAttachmentPath(attachment, context);
                attachment.updateContentArchive(context);
                Archive archive = attachment.getArchive();
                archive.save(vfile.toString());
            }

            // We need to save the attachment info
            saveXWikiDoc(attachment.getDoc(), context, false);
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SAVING_ATTACHMENT,
                    "Exception while saving attachment {0} from document {1}", e, args);
        }
    }

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            byte[] content = new byte[attachment.getFilesize()];
            File file = getAttachmentPath(attachment, context);
            FileInputStream is = new FileInputStream(file);
            is.read(content);
            attachment.setContent(content);
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_LOADING_ATTACHMENT,
                    "Exception while reading document {0} version {1}", e, args);
        }
    }

    public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            Archive archive = attachment.getArchive();

            if (archive==null) {
                File file = getVersionedAttachmentPath(attachment, context);
                String path = file.toString();
                try {
                    synchronized (path) {
                        archive = new Archive(path);
                        attachment.setArchive(archive);
                    }
                } catch (FileNotFoundException e) {
                }
            }
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_LOADING_ATTACHMENT,
                    "Exception while reading document {0} version {1}", e, args);
        }
    }

    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Not implemented");
    }
}
