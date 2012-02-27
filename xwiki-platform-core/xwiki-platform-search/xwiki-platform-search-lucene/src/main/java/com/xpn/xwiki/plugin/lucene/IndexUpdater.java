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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AbstractAttachmentEvent;
import com.xpn.xwiki.internal.event.AttachmentAddedEvent;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xpn.xwiki.internal.event.AttachmentUpdatedEvent;
import com.xpn.xwiki.util.AbstractXWikiRunnable;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 */
public class IndexUpdater extends AbstractXWikiRunnable implements EventListener
{
    /**
     * Logging helper.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexUpdater.class);

    private static final String NAME = "lucene";

    /**
     * The maximum number of milliseconds we have to wait before this thread is safely closed.
     */
    private static final int EXIT_INTERVAL = 3000;

    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentUpdatedEvent(),
        new DocumentCreatedEvent(), new DocumentDeletedEvent(), new AttachmentAddedEvent(),
        new AttachmentDeletedEvent());

    /**
     * Collecting all the fields for using up in search
     */
    static final List<String> fields = new ArrayList<String>();

    private final LucenePlugin plugin;

    /**
     * Milliseconds of sleep between checks for changed documents.
     */
    private final int indexingInterval;

    private final Directory directory;

    private final XWikiDocumentQueue queue = new XWikiDocumentQueue();

    /**
     * Milliseconds left till the next check for changed documents.
     */
    private int indexingTimer = 0;

    /**
     * Soft threshold after which no more documents will be added to the indexing queue. When the queue size gets larger
     * than this value, the index rebuilding thread will sleep chunks of {@code IndexRebuilder#retryInterval}
     * milliseconds until the queue size will get back bellow this threshold. This does not affect normal indexing
     * through wiki updates.
     */
    private final int maxQueueSize;

    /**
     * volatile forces the VM to check for changes every time the variable is accessed since it is not otherwise changed
     * in the main loop the VM could "optimize" the check out and possibly never exit
     */
    private volatile boolean exit = false;

    private Analyzer analyzer;

    IndexUpdater(Directory directory, int indexingInterval, int maxQueueSize, LucenePlugin plugin, XWikiContext context)
    {
        super(XWikiContext.EXECUTIONCONTEXT_KEY, context.clone());

        this.plugin = plugin;

        this.directory = directory;

        this.indexingInterval = indexingInterval;
        this.maxQueueSize = maxQueueSize;
    }

    private XWikiContext getContext()
    {
        return (XWikiContext) Utils.getComponent(Execution.class).getContext()
            .getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    public void doExit()
    {
        this.exit = true;
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
    @Override
    protected void runInternal()
    {
        getContext().setDatabase(getContext().getMainXWiki());
        runMainLoop();
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
                LOGGER.warn("Error while sleeping", e);
            }
        }
    }

    /**
     * Polls the queue for documents to be indexed.
     */
    private void updateIndex()
    {
        if (this.queue.isEmpty()) {
            LOGGER.debug("IndexUpdater: queue empty, nothing to do");
        } else {
            LOGGER.debug("IndexUpdater: documents in queue, start indexing");

            XWikiContext context = getContext();
            context.getWiki().getStore().cleanUp(context);

            IndexWriter writer;
            RETRY: while (true) {
                // We will retry after repairing if the index was
                // corrupt
                try {
                    try {
                        writer = openWriter(false);
                        break RETRY;
                    } catch (CorruptIndexException e) {
                        this.plugin.handleCorruptIndex(context);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to open index", e);

                    throw new RuntimeException(e);
                }
            }

            try {
                int nb = 0;
                while (!this.queue.isEmpty()) {
                    AbstractIndexData data = this.queue.remove();

                    try {
                        if (data.isDeleted()) {
                            removeFromIndex(writer, data, context);
                        } else {
                            addToIndex(writer, data, context);
                        }

                        ++nb;
                    } catch (Throwable e) {
                        LOGGER.error("error indexing document [{}]", data, e);
                    }
                }

                LOGGER.info("indexed [{}] docs to lucene index", nb);
            } catch (Exception e) {
                LOGGER.error("error indexing documents", e);
            } finally {
                context.getWiki().getStore().cleanUp(context);

                try {
                    writer.optimize();
                    writer.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close writer.", e);
                }
            }

            this.plugin.openSearchers(context);
        }
    }

    protected IndexWriter openWriter(boolean create) throws IOException
    {
        while (true) {
            try {
                IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_34, this.analyzer);
                if (create) {
                    cfg.setOpenMode(OpenMode.CREATE);
                }
                IndexWriter w = new IndexWriter(this.directory, cfg);
                return w;
            } catch (LockObtainFailedException e) {
                try {
                    int s = new Random().nextInt(1000);

                    LOGGER.debug("failed to acquire lock, retrying in {}ms ...", s);

                    Thread.sleep(s);
                } catch (InterruptedException e0) {
                }
            }
        }
    }

    private void addToIndex(IndexWriter writer, AbstractIndexData data, XWikiContext context) throws IOException,
        XWikiException
    {
        LOGGER.debug("addToIndex: [{}]", data);

        Document luceneDoc = new Document();
        data.addDataToLuceneDocument(luceneDoc, context);

        // collecting all the fields for using up in search
        for (Fieldable field : luceneDoc.getFields()) {
            if (!fields.contains(field.name())) {
                fields.add(field.name());
            }
        }

        writer.updateDocument(data.getTerm(), luceneDoc);
    }

    private void removeFromIndex(IndexWriter writer, AbstractIndexData data, XWikiContext context)
        throws CorruptIndexException, IOException
    {
        LOGGER.debug("removeFromIndex: [{}]", data);

        writer.deleteDocuments(data.getTerm());
    }

    /**
     * @param analyzer The analyzer to set.
     */
    public void setAnalyzer(Analyzer analyzer)
    {
        this.analyzer = analyzer;
    }

    public void cleanIndex()
    {
        LOGGER.info("trying to clear index for rebuilding");

        try {
            openWriter(true).close();
        } catch (IOException e) {
            LOGGER.error("Failed to clean index", e);
        }
    }

    public void queueDocument(XWikiDocument document, XWikiContext context, boolean deleted)
    {
        this.queue.add(new DocumentData(document, context, deleted));
    }

    public void queueAttachment(XWikiAttachment attachment, XWikiContext context, boolean deleted)
    {
        if (attachment != null && context != null) {
            this.queue.add(new AttachmentData(attachment, context, deleted));
        } else {
            LOGGER.error("Invalid parameters given to {} attachment [{}] of document [{}]", new Object[] {
                deleted ? "deleted" : "added",
                attachment == null ? null : attachment.getFilename(),
                attachment == null || attachment.getDoc() == null ? null : attachment.getDoc().getDocumentReference()});
        }
    }

    public void addAttachment(XWikiDocument document, String attachmentName, XWikiContext context, boolean deleted)
    {
        if (document != null && attachmentName != null && context != null) {
            this.queue.add(new AttachmentData(document, attachmentName, context, deleted));
        } else {
            LOGGER.error("Invalid parameters given to {} attachment [{}] of document [{}]",
                new Object[] {(deleted ? "deleted" : "added"), attachmentName, document});
        }
    }

    public void addWiki(String wikiId, boolean deleted)
    {
        if (wikiId != null) {
            this.queue.add(new WikiData(new WikiReference(wikiId), deleted));
        } else {
            LOGGER.error("Invalid parameters given to {} wiki [{}]", (deleted ? "deleted" : "added"), wikiId);
        }
    }

    public int queueAttachments(XWikiDocument document, XWikiContext context)
    {
        int retval = 0;

        final List<XWikiAttachment> attachmentList = document.getAttachmentList();
        retval += attachmentList.size();
        for (XWikiAttachment attachment : attachmentList) {
            try {
                queueAttachment(attachment, context, false);
            } catch (Exception e) {
                LOGGER.error("Failed to retrieve attachment [{}] of document [{}]",
                    new Object[] {attachment.getFilename(), document, e});
            }
        }

        return retval;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;

        try {
            if (event instanceof DocumentUpdatedEvent || event instanceof DocumentCreatedEvent) {
                queueDocument((XWikiDocument) source, context, false);
            } else if (event instanceof DocumentDeletedEvent) {
                queueDocument((XWikiDocument) source, context, true);
            } else if (event instanceof AttachmentUpdatedEvent || event instanceof AttachmentAddedEvent) {
                queueAttachment(((XWikiDocument) source).getAttachment(((AbstractAttachmentEvent) event).getName()),
                    context, false);
            } else if (event instanceof AttachmentDeletedEvent) {
                addAttachment((XWikiDocument) source, ((AbstractAttachmentEvent) event).getName(), context, true);
            } else if (event instanceof WikiDeletedEvent) {
                addWiki((String) source, true);
            }
        } catch (Exception e) {
            LOGGER.error("error in notify", e);
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
            LOGGER.error("Failed to get the number of documents in Lucene index writer", e);
        }

        return n;
    }

    public int getMaxQueueSize()
    {
        return this.maxQueueSize;
    }
}
