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
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Semaphore;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link IndexUpdater}.
 * 
 * @version $Id$
 */
public class IndexUpdaterTest extends AbstractBridgedXWikiComponentTestCase
{
    private final static String INDEXDIR = "target/lucenetest";

    private final Semaphore rebuildDone = new Semaphore(0);

    private final Semaphore writeBlockerWait = new Semaphore(0);

    private final Semaphore writeBlockerAcquiresLock = new Semaphore(0);

    private Mock mockXWiki;

    private Mock mockXWikiStoreInterface;

    private XWikiDocument loremIpsum;

    private class TestIndexRebuilder extends IndexRebuilder
    {
        TestIndexRebuilder(IndexUpdater indexUpdater, XWikiContext context)
        {
            super(indexUpdater, context);
        }

        @Override
        protected void runInternal()
        {
            super.runInternal();

            IndexUpdaterTest.this.rebuildDone.release();
        }
    }

    private class TestIndexUpdater extends IndexUpdater
    {
        TestIndexUpdater(Directory directory, int indexingInterval, int maxQueueSize, LucenePlugin plugin,
            XWikiContext context)
        {
            super(directory, indexingInterval, maxQueueSize, plugin, context);
        }

        @Override
        protected void runInternal()
        {
            if (Thread.currentThread().getName().equals("writerBlocker")) {
                try {
                    IndexWriter writer = openWriter(true);
                    Thread.sleep(5000);
                    writer.close();
                } catch (Exception e) {
                }
            } else if (Thread.currentThread().getName().equals("permanentBlocker")) {
                try {
                    IndexWriter writer = openWriter(false);
                    IndexUpdaterTest.this.writeBlockerAcquiresLock.release();
                    IndexUpdaterTest.this.writeBlockerWait.acquireUninterruptibly();
                    writer.close();
                } catch (Exception e) {
                }
            } else {
                super.runInternal();
            }
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.loremIpsum = new TestXWikiDocument(new DocumentReference("wiki", "Lorem", "Ipsum"));
        this.loremIpsum.setAuthor("User");
        this.loremIpsum.setCreator("User");
        this.loremIpsum.setDate(new Date(0));
        this.loremIpsum.setCreationDate(new Date(0));
        this.loremIpsum.setTitle("Lorem Ipsum");
        this.loremIpsum
            .setContent("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                + " Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
                + " Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
                + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");

        this.mockXWikiStoreInterface = mock(XWikiStoreInterface.class);
        this.mockXWikiStoreInterface.stubs().method("cleanUp");

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("getDocument").with(eq(this.loremIpsum.getDocumentReference()), ANYTHING)
            .will(returnValue(this.loremIpsum));
        this.mockXWiki.stubs().method("Param").with(ANYTHING, ANYTHING).will(new CustomStub("Implements XWiki.Param")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                return invocation.parameterValues.get(1);
            }
        });
        this.mockXWiki.stubs().method("Param").with(eq(LucenePlugin.PROP_INDEX_DIR))
            .will(returnValue(IndexUpdaterTest.INDEXDIR));
        this.mockXWiki.stubs().method("checkAccess").will(returnValue(true));
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(false));
        this.mockXWiki.stubs().method("getStore").will(returnValue(this.mockXWikiStoreInterface.proxy()));
        this.mockXWiki.stubs().method("search").will(returnValue(Collections.EMPTY_LIST));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());
        getContext().setDatabase("wiki");
    }

    public void testCreateIndex() throws IOException
    {
        File f = new File(INDEXDIR);

        if (!f.exists()) {
            f.mkdirs();
        }

        Directory directory = FSDirectory.open(f);

        LucenePlugin plugin = new LucenePlugin("Monkey", "Monkey", getContext());
        IndexUpdater indexUpdater = new TestIndexUpdater(directory, 100, 1000, plugin, getContext());
        IndexRebuilder indexRebuilder = new TestIndexRebuilder(indexUpdater, getContext());
        indexRebuilder.startRebuildIndex(getContext());

        this.rebuildDone.acquireUninterruptibly();

        assertTrue(IndexReader.indexExists(directory));
    }

    public void testIndexUpdater() throws Exception
    {
        File f = new File(INDEXDIR);
        Directory directory;
        if (!f.exists()) {
            f.mkdirs();
        }
        directory = FSDirectory.open(f);

        int indexingInterval;
        indexingInterval = 100;
        int maxQueueSize;
        maxQueueSize = 1000;

        LucenePlugin plugin = new LucenePlugin("Monkey", "Monkey", getContext());
        IndexUpdater indexUpdater =
            new TestIndexUpdater(directory, indexingInterval, maxQueueSize, plugin, getContext());
        IndexRebuilder indexRebuilder = new TestIndexRebuilder(indexUpdater, getContext());
        Thread writerBlocker = new Thread(indexUpdater, "writerBlocker");
        writerBlocker.start();
        plugin.init(indexUpdater, indexRebuilder, getContext());

        indexUpdater.cleanIndex();

        Thread indexUpdaterThread = new Thread(indexUpdater, "Lucene Index Updater");
        indexUpdaterThread.start();

        indexUpdater.queueDocument(this.loremIpsum.clone(), getContext(), false);
        indexUpdater.queueDocument(this.loremIpsum.clone(), getContext(), false);

        try {
            Thread.sleep(1000);
            indexUpdater.doExit();
        } catch (InterruptedException e) {
        }
        while (true) {
            try {
                indexUpdaterThread.join();
                break;
            } catch (InterruptedException e) {
            }
        }

        Query q = new TermQuery(new Term(IndexFields.DOCUMENT_ID, "wiki:Lorem.Ipsum.default"));
        IndexSearcher searcher = new IndexSearcher(directory, true);
        TopDocs t = searcher.search(q, null, 10);

        assertEquals(1, t.totalHits);

        SearchResults results = plugin.getSearchResultsFromIndexes("Ipsum", "target/lucenetest", null, getContext());

        assertEquals(1, results.getTotalHitcount());
    }

    public void testLock() throws IOException
    {
        Directory directory;
        File f = new File(INDEXDIR);
        int indexingInterval;
        indexingInterval = 100;
        int maxQueueSize;
        maxQueueSize = 1000;

        if (!f.exists()) {
            f.mkdirs();
        }
        directory = FSDirectory.open(f);

        LucenePlugin plugin = new LucenePlugin("Monkey", "Monkey", getContext());

        final IndexUpdater indexUpdater =
            new TestIndexUpdater(directory, indexingInterval, maxQueueSize, plugin, getContext());

        plugin.init(indexUpdater, getContext());

        Thread permanentBlocker = new Thread(indexUpdater, "permanentBlocker");
        permanentBlocker.start();
        this.writeBlockerAcquiresLock.acquireUninterruptibly();

        assertTrue(IndexWriter.isLocked(indexUpdater.getDirectory()));

        final boolean[] doneCleaningIndex = {false};

        Thread indexCleaner = new Thread(new Runnable()
        {
            public void run()
            {
                indexUpdater.cleanIndex();

                doneCleaningIndex[0] = true;
            }
        }, "indexCleaner");

        indexCleaner.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        assertFalse(doneCleaningIndex[0]);

        boolean wasActuallyLocked = false;

        try {
            if (!IndexWriter.isLocked(indexUpdater.getDirectory())) {
                new IndexWriter(indexUpdater.getDirectory(), new StandardAnalyzer(Version.LUCENE_29),
                    MaxFieldLength.LIMITED);
            } else {
                wasActuallyLocked = true;
            }
            // assert(IndexWriter.isLocked(indexUpdater.getDirectory()));
        } catch (LockObtainFailedException e) {
            /*
             * Strange, the isLocked method appears to be unreliable.
             */
            wasActuallyLocked = true;
        }

        assertTrue(wasActuallyLocked);

        this.writeBlockerWait.release();

        while (true) {
            try {
                indexCleaner.join();
                break;
            } catch (InterruptedException e) {
            }
        }

        assertTrue(doneCleaningIndex[0]);

        assertFalse(IndexWriter.isLocked(indexUpdater.getDirectory()));

        IndexWriter w =
            new IndexWriter(indexUpdater.getDirectory(), new StandardAnalyzer(Version.LUCENE_29),
                MaxFieldLength.LIMITED);
        w.close();
    }
}
