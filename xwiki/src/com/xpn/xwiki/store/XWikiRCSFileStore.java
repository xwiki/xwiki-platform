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

import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.util.Util;
import org.apache.commons.jrcs.rcs.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Date;
import java.util.List;

public class XWikiRCSFileStore extends XWikiDefaultStore {
    private File rscpath;

    public XWikiRCSFileStore() {
    }


    public XWikiRCSFileStore(XWiki xwiki, XWikiContext context) {
        String rcspath = xwiki.ParamAsRealPath("xwiki.store.rcs.path", context);
        setPath(rcspath);
    }

    public XWikiRCSFileStore(String rcspath) {
        setPath(rcspath);
    }

    public void setPath(String rcspath) {
        this.rscpath = new File(rcspath);
    }

    public String getPath() {
        return rscpath.toString();
    }

    public File getFilePath(XWikiDocInterface doc) {
        File webdir = new File(getPath(), doc.getWeb().replace('.','/'));
        webdir.mkdirs();
        return new File(webdir, doc.getName() + ".txt");
    }

    public File getVersionedFilePath(XWikiDocInterface doc) {
        File webfile = new File(getPath(), doc.getWeb().replace('.','/'));
        return new File(webfile, doc.getName() + ".txt,v");
    }

    public void saveXWikiDoc(XWikiDocInterface doc) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        try {
            doc.setStore(this);
            // Handle the latest text file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                doc.setDate(new Date());
                doc.incrementVersion();
            }
            File file = getFilePath(doc);
            FileWriter wr = new FileWriter(file);
            wr.write(getFullContent(doc));
            wr.flush();
            wr.close();

            // Now handle the versioned file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                File vfile = getVersionedFilePath(doc);
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

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        BufferedReader fr = null;
        try {
            doc.setStore(this);
            File file = getFilePath(doc);
            StringBuffer content = new StringBuffer();
            fr = new BufferedReader(new FileReader(file));
            String line;
            boolean bMetaDataDone = false;
            while (true) {
                line = fr.readLine();
                if (line==null) {
                    doc.setContent(content.toString());
                    doc.setMostRecent(true);
                    fr.close();
                    break;
                }
                if (bMetaDataDone||(parseMetaData(doc,line)==false)) {
                    content.append(line);
                    content.append("\n");
                }
            }
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_READING_FILE,
                    "Exception while reading document {0}", e, args);
        }
        return doc;
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc,String version) throws XWikiException {
        //To change body of implemented methods use Options | File Templates.
        try {
            doc.setStore(this);
            Archive archive = doc.getRCSArchive();

            if (archive==null) {
                File file = getVersionedFilePath(doc);
                String path = file.toString();
                synchronized (path) {
                    archive = new Archive(path);
                }
            }

            Object[] text = (Object[]) archive.getRevision(version);
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
        } catch (Exception e) {
            Object[] args = { doc.getFullName(), version.toString() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        }
        return doc;
    }

    public Version[] getXWikiDocVersions(XWikiDocInterface doc) throws XWikiException {
        try {
            doc.setStore(this);
            File vfile = getVersionedFilePath(doc);
            String path = vfile.toString();
            synchronized (path) {
                Archive archive;
                try {
                    archive= new Archive(path);
                } catch (Exception e) {
                    File file = getFilePath(doc);
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
        StringBuffer buf = new StringBuffer();
        getMetaData(doc, buf);
        buf.append(doc.getContent());
        return buf.toString();
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

    public boolean parseMetaData(XWikiDocInterface doc, String line) throws IOException {
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

    public List getClassList() throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching: not implemented");
    }

    public List searchDocuments(String wheresql) throws XWikiException {
        return searchDocuments(wheresql,0,0);
    }

    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching: not implemented");
    }

    public void saveXWikiObject(BaseObject object, boolean bTransaction) throws XWikiException {
           throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                   "Exception while searching: not implemented");
    }

    public void loadXWikiObject(BaseObject object, boolean bTransaction) throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching: not implemented");
    }

    public void saveXWikiClass(BaseClass bclass, boolean bTransaction) throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching: not implemented");
    }

    public void loadXWikiClass(BaseClass bclass, boolean bTransaction) throws XWikiException {
        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_RCS_SEARCH,
                "Exception while searching: not implemented");
    }
}
