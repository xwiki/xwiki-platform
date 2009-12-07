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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ActionExecutionEvent;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class IndexUpdater extends AbstractXWikiRunnable implements EventListener
{
    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(IndexUpdater.class);

    private static final String NAME = "lucene";

    /** The maximum number of milliseconds we have to wait before this thread is safely closed. */
    private static final int EXIT_INTERVAL = 3000;

    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentUpdateEvent());
            add(new DocumentSaveEvent());
            add(new DocumentDeleteEvent());
            add(new ActionExecutionEvent("upload"));
        }
    };

    /** Milliseconds of sleep between checks for changed documents. */
    private final int indexingInterval;

    /** Milliseconds left till the next check for changed documents. */
    private int indexingTimer = 0;

    /**
     * volatile forces the VM to check for changes every time the variable is accessed since it is not otherwise changed
     * in the main loop the VM could "optimize" the check out and possibly never exit
     */
    private volatile boolean exit = false;

    private final Directory directory;

    private final XWikiDocumentQueue queue = new XWikiDocumentQueue();

    /**
     * Soft threshold after which no more documents will be added to the indexing queue. When the queue size gets larger
     * than this value, the index rebuilding thread will sleep chuks of {@link IndexRebuilder#retryInterval}
     * milliseconds until the queue size will get back bellow this threshold. This does not affect normal indexing
     * through wiki updates.
     */
    private final int maxQueueSize;

    private Analyzer analyzer;

    private final LucenePlugin plugin;

    private XWikiContext context;

    private XWiki xwiki;

    static List<String> fields = new ArrayList<String>();

    IndexUpdater(Directory directory, int indexingInterval, int maxQueueSize, LucenePlugin plugin, XWikiContext context)
    {
        this.xwiki = context.getWiki();
        this.context = (XWikiContext) context.clone();
        this.context.setDatabase(this.context.getMainXWiki());
        this.plugin = plugin;
        this.directory = directory;

        this.indexingInterval = indexingInterval;
        this.maxQueueSize = maxQueueSize;
    }

    public void doExit()
    {
        exit = true;
    }

    /**
     * Return a reference to the directory that this updater is currently working with.
     */

    public Directory getDirectory()
    {
        return this.directory;
    }

    /**
     * Main loop. Polls the queue for documents to be indexed.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        MDC.put("url", "Lucene index updating thread");

        // Since this is where a new thread is created this is where we need to initialize the Container
        // ThreadLocal variables and not in the init() method. Otherwise we would simply overwrite the
        // Container values for the main thread...
        try {
            initXWikiContainer(this.context);
            runMainLoop();
        } finally {
            // Cleanup Container component (it has ThreadLocal variables)
            cleanupXWikiContainer(this.context);
            this.xwiki.getStore().cleanUp(this.context);
            MDC.remove("url");
        }
    }

    /**
     * Main loop. Polls the queue for documents to be indexed.
     */
    private void runMainLoop()
    {
        while (!this.exit) {
            // Check if the indexing interval elapsed.
            if (this.indexingTimer == 0) {
                // Reset the indexing timer.
                this.indexingTimer = this.indexingInterval;
                // Poll the queue for documents to be indexed.
                updateIndex();
            }
            // Remove the exit interval from the indexing timer.
            int sleepInterval = Math.min(EXIT_INTERVAL, this.indexingTimer);
            this.indexingTimer -= sleepInterval;
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                LOG.warn("Error while sleeping", e);
            }
        }
    }

    /** Polls the queue for documents to be indexed. */
    private void updateIndex()
    {
        if (this.queue.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("IndexUpdater: queue empty, nothing to do");
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("IndexUpdater: documents in queue, start indexing");
            }

            XWikiContext context = (XWikiContext) this.context.clone();
            context.getWiki().getStore().cleanUp(context);

            IndexSearcher searcher;
            IndexWriter writer;
            RETRY: while (true) {
                // We will retry after repairing if the index was
                // corrupt
                try {
                    try {
                        searcher = new IndexSearcher(this.directory, true);
                        writer = openWriter(false);
                        break RETRY;
                    } catch (CorruptIndexException e) {
                        this.plugin.handleCorruptIndex(context);
                    }
                } catch (IOException e) {
                    LOG.error("Failed to open index");

                    throw new RuntimeException(e);
                }
            }

            try {
                int nb = 0;
                while (!this.queue.isEmpty()) {
                    IndexData data = this.queue.remove();

                    String id = data.getId();

                    try {
                        /*
                         * XXX Is it not possible to obtain the right translation directly?
                         */
                        XWikiDocument doc = this.xwiki.getDocument(data.getFullName(), context);

                        if (data.getLanguage() != null && !data.getLanguage().equals("")) {
                            doc = doc.getTranslatedDocument(data.getLanguage(), context);
                        }

                        addToIndex(writer, searcher, data, doc, context);
                        ++nb;
                    } catch (Exception e) {
                        LOG.error("error indexing document " + id, e);
                    }
                }

                if (LOG.isInfoEnabled()) {
                    LOG.info("indexed " + nb + " docs to lucene index");
                }

                writer.commit();
            } catch (Exception e) {
                LOG.error("error indexing documents", e);
            } finally {
                this.context.getWiki().getStore().cleanUp(this.context);
                try {
                    searcher.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close searcher.");
                }
                try {
                    writer.optimize();
                    writer.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close writer.");
                }
            }

            this.plugin.openSearchers(context);
        }
    }

    protected IndexWriter openWriter(boolean create) throws IOException
    {
        while (true) {
            try {
                IndexWriter w =
                        new IndexWriter(this.directory, this.analyzer, create, IndexWriter.MaxFieldLength.LIMITED);
                w.setUseCompoundFile(true);

                return w;
            } catch (LockObtainFailedException e) {
                try {
                    int s = new Random().nextInt(1000);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("failed to acquire lock, retrying in " + s + "ms ...");
                    }

                    Thread.sleep(s);
                } catch (InterruptedException e0) {
                }
            }
        }
    }

    private void addToIndex(IndexWriter writer, IndexSearcher searcher, IndexData data, XWikiDocument doc,
        XWikiContext context) throws IOException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("addToIndex: " + data);
        }

        Document luceneDoc = new Document();
        data.addDataToLuceneDocument(luceneDoc, doc, context);

        // collecting all the fields for using up in search
        for (Field fld : (List<Field>) luceneDoc.getFields()) {
            if (!fields.contains(fld.name())) {
                fields.add(fld.name());
            }
        }

        writer.updateDocument(new Term(IndexFields.DOCUMENT_ID, data.getId()), luceneDoc);
    }

    /**
     * @param analyzer The analyzer to set.
     */
    public void setAnalyzer(Analyzer analyzer)
    {
        this.analyzer = analyzer;
    }

    public void cleanIndex() throws IOException
    {
        if (LOG.isInfoEnabled()) {
            LOG.info("trying to clear index for rebuilding");
        }

        try {
            openWriter(true).close();
        } catch (IOException e) {
            LOG.error("Failed to clean index: " + e);
        }
    }

    public void add(XWikiDocument document, XWikiContext context)
    {
        this.queue.add(new DocumentData(document, context));
    }

    public void add(XWikiDocument document, XWikiAttachment attachment, XWikiContext context)
    {
        if (document != null && attachment != null && context != null) {
            this.queue.add(new AttachmentData(document, attachment, context));
        } else {
            LOG.error("invalid parameters given to add: " + document + ", " + attachment + ", " + context);
        }
    }

    public int addAttachmentsOfDocument(XWikiDocument document, XWikiContext context)
    {
        int retval = 0;

        final List<XWikiAttachment> attachmentList = document.getAttachmentList();
        retval += attachmentList.size();
        for (XWikiAttachment attachment : attachmentList) {
            try {
                add(document, attachment, context);
            } catch (Exception e) {
                LOG.error("error retrieving attachment of document " + document.getFullName(), e);
            }
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        try {
            if (event instanceof ActionExecutionEvent) {
                // Modified attachement
                if (LOG.isDebugEnabled()) {
                    LOG.debug("upload action notification for doc " + document.getName());
                }

                // Retrieve the latest version (with the file just attached)
                XWikiDocument basedoc = context.getWiki().getDocument(document.getFullName(), context);
                List<XWikiAttachment> attachments = basedoc.getAttachmentList();
                /*
                 * XXX Race condition: if two or more attachments are added before we find the "newest attachment"
                 * below, only the last one added will be indexed.
                 */
                XWikiAttachment newestAttachment = null;
                for (XWikiAttachment attachment : attachments) {
                    if ((newestAttachment == null) || attachment.getDate().after(newestAttachment.getDate())) {
                        newestAttachment = attachment;
                    }
                }

                add(basedoc, newestAttachment, context);
            } else {
                // Modified document
                if (LOG.isDebugEnabled()) {
                    LOG.debug("notify from XWikiDocChangeNotificationInterface, event=" + event + ", doc=" + document);
                }

                add(document, context);
            }
        } catch (Exception e) {
            LOG.error("error in notify", e);
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
     * @return the number of documents in Lucene index writer.
     */
    public long getLuceneDocCount()
    {
        int n = -1;

        try {
            IndexWriter w = openWriter(false);
            n = w.numDocs();
            w.close();
        } catch (IOException e) {
        }

        return n;
    }

    public int getMaxQueueSize()
    {
        return maxQueueSize;
    }

}
