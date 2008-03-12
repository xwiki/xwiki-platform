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
 */
package com.xpn.xwiki.plugin.lucene;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
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
 * @version $Id: $
 */
public class IndexUpdater implements Runnable, XWikiDocChangeNotificationInterface,
    XWikiActionNotificationInterface
{
    private static final Log LOG = LogFactory.getLog(IndexUpdater.class);

    /**
     * Milliseconds of sleep between checks for changed documents
     */
    private int indexingInterval = 3000;

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

    private long activesIndexedDocs = 0;

    static List fields = new ArrayList();

    public boolean needInitialBuild = false;

    public void doExit()
    {
        exit = true;
    }

    /**
     * Main loop. Polls the queue for documents to be indexed.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        MDC.put("url", "index updating thread");

        while (!this.exit) {
            if (this.queue.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IndexUpdater: queue empty, nothing to do");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("IndexUpdater: documents in queue, start indexing");
                }

                Map toIndex = new HashMap();
                List toDelete = new ArrayList();
                activesIndexedDocs = 0;

                try {
                    openSearcher();
                    while (!this.queue.isEmpty()) {
                        IndexData data = this.queue.remove();
                        List oldDocs = getOldIndexDocIds(data);
                        if (oldDocs != null) {
                            for (int i = 0; i < oldDocs.size(); i++) {
                                Object id = oldDocs.get(i);

                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Adding " + id + " to remove list");
                                }

                                if (!toDelete.contains(id)) {
                                    toDelete.add(id);
                                } else {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Found " + id
                                            + " already in list while adding it to remove list");
                                    }
                                }
                            }
                        }

                        String id = data.getId();
                        LOG.debug("Adding " + id + " to index list");
                        if (toIndex.containsKey(id)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found " + id
                                    + " already in list while adding it to index list");
                            }
                            toIndex.remove(id);
                        }
                        ++activesIndexedDocs;
                        toIndex.put(id, data);
                    }
                } catch (Exception e) {
                    LOG.error("error preparing index queue", e);
                } finally {
                    closeSearcher();
                }

                // Let's delete
                try {
                    openSearcher();
                    if (LOG.isInfoEnabled()) {
                        LOG.info("deleting " + toDelete.size() + " docs from lucene index");
                    }
                    int nb = deleteOldDocs(toDelete);
                    if (LOG.isInfoEnabled()) {
                        LOG.info("deleted " + nb + " docs from lucene index");
                    }
                } catch (Exception e) {
                    LOG.error("error deleting previous documents", e);
                } finally {
                    closeSearcher();
                }

                // Let's index
                try {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("indexing " + toIndex.size() + " docs to lucene index");
                    }

                    XWikiContext context = (XWikiContext) this.context.clone();
                    context.getWiki().getStore().cleanUp(context);
                    openWriter(false);

                    int nb = 0;
                    for (Iterator entryIt = toIndex.entrySet().iterator(); entryIt.hasNext();) {
                        Map.Entry entry = (Map.Entry) entryIt.next();

                        String id = (String) entry.getKey();
                        IndexData data = (IndexData) entry.getValue();

                        try {
                            XWikiDocument doc =
                                this.xwiki.getDocument(data.getFullName(), context);

                            if (data.getLanguage() != null && !data.getLanguage().equals("")) {
                                doc = doc.getTranslatedDocument(data.getLanguage(), context);
                            }

                            addToIndex(data, doc, context);
                            ++nb;
                            --activesIndexedDocs;
                        } catch (Exception e) {
                            LOG.error("error indexing document " + id, e);
                        }
                    }

                    if (LOG.isInfoEnabled()) {
                        LOG.info("indexed " + nb + " docs to lucene index");
                    }

                    writer.flush();
                } catch (Exception e) {
                    LOG.error("error indexing documents", e);
                } finally {
                    this.context.getWiki().getStore().cleanUp(this.context);
                    closeWriter();
                }

                plugin.openSearchers();
            }
            try {
                Thread.sleep(indexingInterval);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.xwiki.getStore().cleanUp(this.context);
        MDC.remove("url");
    }

    private synchronized void closeSearcher()
    {
        try {
            if (this.searcher != null) {
                this.searcher.close();
            }
            if (this.reader != null) {
                this.reader.close();
            }
        } catch (IOException e) {
            LOG.error("error closing index searcher", e);
        } finally {
            this.searcher = null;
            this.reader = null;
        }
    }

    /**
     * Opens the index reader and searcher used for finding and deleting old versions of indexed
     * documents.
     */
    private synchronized void openSearcher()
    {
        try {
            this.reader = IndexReader.open(this.indexDir);
            this.searcher = new IndexSearcher(this.reader);
        } catch (IOException e) {
            LOG.error("error opening index searcher", e);
        }
    }

    /**
     * Deletes the documents with the given ids from the index.
     */
    private int deleteOldDocs(List oldDocs)
    {
        int nb = 0;

        for (Iterator iter = oldDocs.iterator(); iter.hasNext();) {
            Integer id = (Integer) iter.next();
            if (LOG.isDebugEnabled()) {
                LOG.debug("delete doc " + id);
            }

            try {
                this.reader.deleteDocument(id.intValue());
                nb++;
            } catch (IOException e1) {
                LOG.error("error deleting doc " + id, e1);
            }
        }

        return nb;
    }

    private List getOldIndexDocIds(IndexData data)
    {
        List retval = new ArrayList(3);
        Query query = data.buildQuery();
        try {
            Hits hits = this.searcher.search(query);
            for (int i = 0; i < hits.length(); i++) {
                retval.add(new Integer(hits.id(i)));
            }
        } catch (Exception e) {
            LOG.error("error looking for old versions of document " + data + " with query "
                + query, e);
        }

        return retval;
    }

    private void openWriter(boolean create)
    {
        if (writer != null) {
            LOG.error("Writer already open and createWriter called");
            return;
        }

        try {
            // fix for windows by Daniel Cortes:
            FSDirectory f = FSDirectory.getDirectory(indexDir);
            writer = new IndexWriter(f, analyzer, create);
            // writer = new IndexWriter (indexDir, analyzer, create);
            writer.setUseCompoundFile(true);

            if (LOG.isDebugEnabled()) {
                LOG.debug("successfully opened index writer : " + indexDir);
            }
        } catch (IOException e) {
            LOG.error("IOException when opening Lucene Index for writing at " + indexDir, e);
        }
    }

    private void closeWriter()
    {
        if (this.writer == null) {
            LOG.error("Writer not open and closeWriter called");
            return;
        }

        try {
            this.writer.optimize();
        } catch (IOException e1) {
            LOG.error("Exception caught when optimizing Index", e1);
        }

        try {
            this.writer.close();
        } catch (Exception e) {
            LOG.error("Exception caught when closing IndexWriter", e);
        }

        this.writer = null;

        if (LOG.isDebugEnabled()) {
            LOG.debug("closed writer.");
        }
    }

    private void addToIndex(IndexData data, XWikiDocument doc, XWikiContext context)
        throws IOException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("addToIndex: " + data);
        }

        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
        data.addDataToLuceneDocument(luceneDoc, doc, context);
        Field fld = null;

        // collecting all the fields for using up in search
        for (Iterator it = luceneDoc.getFields().iterator(); it.hasNext();) {
            fld = (Field) it.next();
            if (!fields.contains(fld.name())) {
                fields.add(fld.name());
            }
        }

        this.writer.addDocument(luceneDoc);
    }

    /**
     * @param indexDir The indexDir to set.
     */
    public void setIndexDir(String indexDir)
    {
        this.indexDir = indexDir;
    }

    /**
     * @param analyzer The analyzer to set.
     */
    public void setAnalyzer(Analyzer analyzer)
    {
        this.analyzer = analyzer;
    }

    public synchronized void init(Properties config, LucenePlugin plugin, XWikiContext context)
    {
        this.xwiki = context.getWiki();
        this.context = (XWikiContext) context.clone();
        this.context.setDatabase(this.context.getMainXWiki());
        this.plugin = plugin;
        // take the first configured index dir as the one for writing
        // String[] indexDirs =
        // StringUtils.split(config.getProperty(LucenePlugin.PROP_INDEX_DIR), "
        // ,");
        String[] indexDirs = StringUtils.split(plugin.getIndexDirs(), ",");
        if (indexDirs != null && indexDirs.length > 0) {
            this.indexDir = indexDirs[0];
            File f = new File(indexDir);
            if (!f.isDirectory()) {
                f.mkdirs();
                this.needInitialBuild = true;
            }
            if (!IndexReader.indexExists(f)) {
                this.needInitialBuild = true;
            }
        }

        this.indexingInterval =
            1000 * Integer.parseInt(config
                .getProperty(LucenePlugin.PROP_INDEXING_INTERVAL, "300"));

        // Note: There's no need to open the Searcher here (with a call to
        // openSearcher()) as each
        // task needing it will open it itself.
    }

    public void cleanIndex()
    {
        if (LOG.isInfoEnabled()) {
            LOG.info("trying to clear index for rebuilding");
        }

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

    public void add(XWikiDocument document, XWikiContext context)
    {
        this.queue.add(new DocumentData(document, context));

        if (document.hasElement(XWikiDocument.HAS_OBJECTS)) {
            addObject(document, context);
        }
    }

    public void addObject(XWikiDocument document, XWikiContext context)
    {
        this.queue.add(new ObjectData(document, context));
    }

    public void add(XWikiDocument document, XWikiAttachment attachment, XWikiContext context)
    {
        if (document != null && attachment != null && context != null) {
            this.queue.add(new AttachmentData(document, attachment, context));
        } else {
            LOG.error("invalid parameters given to add: " + document + ", " + attachment + ", "
                + context);
        }
    }

    public int addAttachmentsOfDocument(XWikiDocument document, XWikiContext context)
    {
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
     *      com.xpn.xwiki.doc.XWikiDocument,com.xpn.xwiki.doc.XWikiDocument,
     *      int,com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument newDoc, XWikiDocument oldDoc,
        int event, XWikiContext context)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("notify from XWikiDocChangeNotificationInterface, event=" + event
                + ", newDoc=" + newDoc + " oldDoc=" + oldDoc);
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
     *      com.xpn.xwiki.doc.XWikiDocument,java.lang.String,com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule arg0, XWikiDocument doc, String action,
        XWikiContext context)
    {
        if ("upload".equals(action)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("upload action notification for doc " + doc.getName());
            }

            try {
                // Retrieve the latest version (with the file just attached)
                XWikiDocument basedoc = context.getWiki().getDocument(doc.getFullName(), context);
                List attachments = basedoc.getAttachmentList();
                // find out the most recently changed attachment
                XWikiAttachment newestAttachment = null;
                for (Iterator iter = attachments.iterator(); iter.hasNext();) {
                    XWikiAttachment attachment = (XWikiAttachment) iter.next();
                    if ((newestAttachment == null)
                        || attachment.getDate().after(newestAttachment.getDate())) {
                        newestAttachment = attachment;
                    }
                }
                add(basedoc, newestAttachment, context);
            } catch (Exception e) {
                LOG.error("error in notify", e);
            }
        }
    }

    /**
     * @return the number of documents in the queue.
     */
    public long getQueueSize()
    {
        return this.queue.getSize();
    }

    /**
     * @return the number of documents Lucene index writer.
     */
    public long getLuceneDocCount()
    {
        if (writer != null)
            return writer.docCount();

        return -1;
    }

    /**
     * @return the number of documents in the second queue gave to Lucene.
     */
    public long getActiveQueueSize()
    {
        return this.activesIndexedDocs;
    }
}
