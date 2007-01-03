/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 * Created on 21.01.2005
 *
 */

package com.xpn.xwiki.plugin.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiActionNotificationInterface;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class IndexUpdater implements Runnable, XWikiDocChangeNotificationInterface,
        XWikiActionNotificationInterface {

    private static final Logger LOG = Logger.getLogger(IndexUpdater.class);

    /**
     * Milliseconds of sleep between checks for changed documents
     */
    private int indexingInterval = 300000;
    private boolean exit = false;
    private IndexWriter writer;
    private String indexDir;
    private XWikiDocumentQueue queue = new XWikiDocumentQueue();
    private Analyzer analyzer;
    private LucenePlugin plugin;
    private IndexSearcher searcher;
    private IndexReader reader;

    private XWikiContext context;
    private XWiki xwiki;

    static List fields = new ArrayList();
    

    public void doExit() {
        exit = true;
    }

    /**
     * Main loop. Polls the queue for documents to be indexed.
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        MDC.put("url", "index updating thread");

        while (!exit) {
            if (queue.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IndexUpdater: queue empty, nothing to do");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IndexUpdater: documents in queue, start indexing");
                }
                // we need a searcher to find old versions of documents
                openSearcher();
                openWriter(false);
                List oldDocs = new ArrayList();

                while (!queue.isEmpty()) {
                    XWikiContext context = (XWikiContext) this.context.clone();
                    context.getWiki().getStore().cleanUp(context);
                    IndexData data = queue.remove();

                    try {
                        oldDocs.addAll(getOldIndexDocIds(data));
                        XWikiDocument doc = xwiki.getDocument(data.getFullName(), context);
                        addToIndex(data, doc, context);
                    } catch (Exception e) {
                        LOG.error("error retrieving doc from own context: " + e.getMessage(), e);
                        e.printStackTrace();
                    }
                    context.getWiki().getStore().cleanUp(context);
                }
                closeWriter();
                // the following searcher close/open cycle is necessary because
                // the old reader is not valid for document deletion anymore
                // after
                // updating the index
                closeSearcher();
                openSearcher();
                deleteOldDocs(oldDocs);
                closeSearcher();
                // readers and searchers should be reopened after index update
                plugin.openSearchers();
            }
            try {
                Thread.sleep(indexingInterval);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        xwiki.getStore().cleanUp(context);
        MDC.remove("url");
    }

    private synchronized void closeSearcher() {
        try {
            if (searcher != null) searcher.close();
            if (reader != null) reader.close();
        } catch (IOException e) {
            LOG.error("error closing index searcher", e);
            e.printStackTrace();
        } finally {
            searcher = null;
            reader = null;
        }
    }

    /**
     * Opens the index reader and searcher used for finding and deleting old
     * versions of indexed documents.
     */
    private synchronized void openSearcher() {
        try {
            reader = IndexReader.open(indexDir);
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            LOG.error("error opening index searcher", e);
            e.printStackTrace();
        }
    }

    /**
     * Deletes the documents with the given ids from the index.
     *
     * @param oldDocs
     */
    private void deleteOldDocs(List oldDocs) {
        for (Iterator iter = oldDocs.iterator(); iter.hasNext();) {
            Integer id = (Integer) iter.next();
            if (LOG.isDebugEnabled()) {
                LOG.debug("delete doc " + id);
            }
            try {
                reader.deleteDocument(id.intValue());
            } catch (IOException e1) {
                LOG.error("error deleting doc " + id, e1);
                e1.printStackTrace();
            }
        }
    }

    /**
     * @param data
     * @return
     */
    private Collection getOldIndexDocIds(IndexData data) {
        List retval = new ArrayList(3);
        Query query = data.buildQuery();
        try {
            Hits hits = searcher.search(query);
            for (int i = 0; i < hits.length(); i++) {
                retval.add(new Integer(hits.id(i)));
            }
        } catch (IOException e) {
            LOG.error("error looking for old versions of document " + data + " with query " + query, e);
            e.printStackTrace();
        }
        return retval;
    }

    /**
     *
     */
    private void openWriter(boolean create) {
        if (writer != null) {
            LOG.error("Writer already open and createWriter called");
            return;
        }
        try {
            // fix for windows by Daniel Cortes:
            FSDirectory f = FSDirectory.getDirectory(indexDir, false);
            writer = new IndexWriter(f, analyzer, create);
            //writer = new IndexWriter (indexDir, analyzer, create);
            writer.setUseCompoundFile(true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("successfully opened index writer : " + indexDir);
            }
        } catch (IOException e) {
            LOG.error("IOException when opening Lucene Index for writing at " + indexDir, e);
        }
    }

    /**
     *
     */
    private void closeWriter() {
        if (writer == null) {
            LOG.error("Writer not open and closeWriter called");
            return;
        }
        try {
            writer.optimize();
        } catch (IOException e1) {
            LOG.error("Exception caught when optimizing Index", e1);
        }
        try {
            writer.close();
        } catch (Exception e) {
            LOG.error("Exception caught when closing IndexWriter", e);
        }
        writer = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("closed writer.");
        }

    }

    /**
     * @param doc
     * @throws IOException
     */
    private void addToIndex(IndexData data, XWikiDocument doc, XWikiContext context) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("addToIndex: " + data);
        }
        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
        data.addDataToLuceneDocument(luceneDoc, doc, context);
        Field fld = null;
        // collecting all the fields for using up in search
        for (Enumeration e = luceneDoc.fields(); e.hasMoreElements();) {
            fld = (Field) e.nextElement();
            if (!fields.contains(fld.name())) {
                fields.add(fld.name());
            }
        }
        writer.addDocument(luceneDoc);
    }

    /**
     * @param indexDir The indexDir to set.
     */
    public void setIndexDir(String indexDir) {
        this.indexDir = indexDir;
    }

    /**
     * @param analyzer The analyzer to set.
     */
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * @param config
     */
    public synchronized void init(Properties config, LucenePlugin plugin, XWiki wiki) {
        this.xwiki = wiki;
        this.context = new XWikiContext();
        this.context.setWiki(xwiki);
        this.context.setDatabase(xwiki.getDatabase());
        this.plugin = plugin;
        // take the first configured index dir as the one for writing
        String[] indexDirs = StringUtils.split(config.getProperty(LucenePlugin.PROP_INDEX_DIR), " ,");
        if (indexDirs != null && indexDirs.length > 0) {
            this.indexDir = indexDirs[0];
            File f = new File(indexDir);
            if (!f.isDirectory()) {
                f.mkdirs();
                cleanIndex();
            }
        }
        indexingInterval = 1000 * Integer.parseInt(config.getProperty(LucenePlugin.PROP_INDEXING_INTERVAL,
                "300"));
        openSearcher();
    }

    /**
     *
     */
    public void cleanIndex() {
        LOG.info("trying to clear index for rebuilding");
        while (writer != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("waiting for existing index writer to close");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        synchronized (this) {
            openWriter(true);
            closeWriter();
        }
    }

    /**
     * @param document
     */
    public void add(XWikiDocument document, XWikiContext context) {
        queue.add(new DocumentData(document, context));
        if (document.hasElement(XWikiDocument.HAS_OBJECTS)) {
            addObject(document, context);
        }
    }

    /**
     * @param document
     * @param context
     */
    public void addObject(XWikiDocument document, XWikiContext context) {
        queue.add(new ObjectData(document, context));
    }

    /**
     * @param attachment
     */
    public void add(XWikiDocument document, XWikiAttachment attachment, XWikiContext context) {
        if (document != null && attachment != null && context != null)
            queue.add(new AttachmentData(document, attachment, context));
        else
            LOG.error("invalid parameters given to add: " + document + ", " + attachment + ", " + context);
    }


    public int addAttachmentsOfDocument(XWikiDocument document, XWikiContext context) {
        int retval = 0;
        final List attachmentList = document.getAttachmentList();
        retval += attachmentList.size();
        for (Iterator attachmentIter = attachmentList.iterator(); attachmentIter.hasNext();) {
            try {
                XWikiAttachment attachment = (XWikiAttachment) attachmentIter.next();
                add(document, attachment, context);
            } catch (Exception e) {
                LOG.error("error retrieving attachment of document " + document.getFullName(), e);
            }
        }
        return retval;
    }


    /**
     * Notification of changes in document content
     *
     * @see com.xpn.xwiki.notify.XWikiNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *com.xpn.xwiki.doc.XWikiDocument,com.xpn.xwiki.doc.XWikiDocument,
     *int,com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument newDoc, XWikiDocument oldDoc, int event,
                       XWikiContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("notify from XWikiDocChangeNotificationInterface, event=" + event + ", newDoc="
                    + newDoc + " oldDoc=" + oldDoc);
        }
        try {
            add(newDoc, context);
        } catch (Exception e) {
            LOG.error("error in notify", e);
        }
    }

    /**
     * Notification of attachment uploads.
     *
     * @see com.xpn.xwiki.notify.XWikiActionNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *com.xpn.xwiki.doc.XWikiDocument,java.lang.String,
     *com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule arg0, XWikiDocument doc, String action, XWikiContext context) {
        if ("upload".equals(action)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("upload action notification for doc " + doc.getName());
            }
            try {
                List attachments = doc.getAttachmentList();
                // find out the most recently changed attachment
                XWikiAttachment newestAttachment = null;
                for (Iterator iter = attachments.iterator(); iter.hasNext();) {
                    XWikiAttachment attachment = (XWikiAttachment) iter.next();
                    if (newestAttachment != null
                            && attachment.getDate().before(newestAttachment.getDate()))
                        newestAttachment = attachment;
                    else
                        newestAttachment = attachment;
                }
                add(doc, newestAttachment, context);
            } catch (Exception e) {
                LOG.error("error in notify", e);
            }
        }
    }

    public long getQueueSize() {
        return queue.getSize();
    }
}
