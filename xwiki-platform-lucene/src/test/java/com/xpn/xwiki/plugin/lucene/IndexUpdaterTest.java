package com.xpn.xwiki.plugin.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Semaphore;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

public class IndexUpdaterTest extends AbstractBridgedXWikiComponentTestCase
{
    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite(IndexUpdaterTest.class);
    }

    public class TestXWiki extends XWiki
    {
        public XWikiStoreInterface getStore()
        {
            return new TestStore();
        }

        public XWikiDocument getDocument(String fullname, XWikiContext context)
        {
            return new LoremIpsum();
        }

        @Override
        public XWikiConfig getConfig()
        {
            XWikiConfig p = new XWikiConfig();
            p.setProperty(LucenePlugin.PROP_INDEX_DIR, IndexUpdaterTest.INDEXDIR);

            return p;
        }

        @Override
        public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
        {
            return true;
        }
    }

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

    private class LoremIpsum extends XWikiDocument
    {
        public LoremIpsum()
        {

        }

        @Override
        public String getAuthor()
        {
            return "User";
        }

        @Override
        public String getCreator()
        {
            return "User";
        }

        @Override
        public Date getDate()
        {
            return new Date(0);
        }

        @Override
        public Date getCreationDate()
        {
            return new Date(0);
        }

        @Override
        public String getName()
        {
            return "Ipsum";
        }

        @Override
        public String getDisplayTitle(XWikiContext context)
        {
            return "Lorem Ipsum";
        }

        @Override
        public String getSpace()
        {
            return "Lorem";
        }

        @Override
        public String getFullName()
        {
            return "Lorem.Ipsum";
        }

        @Override
        public String getContent()
        {
            return "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        }

    }

    private final static String INDEXDIR = "target/lucenetest";

    private final Semaphore rebuildDone = new Semaphore(0);

    private final Semaphore writeBlockerWait = new Semaphore(0);

    private final Semaphore writeBlockerAcquiresLock = new Semaphore(0);

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        getContext().setWiki(new TestXWiki());
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

        indexUpdater.add(new LoremIpsum(), getContext());
        indexUpdater.add(new LoremIpsum(), getContext());

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
                new IndexWriter(indexUpdater.getDirectory(), new StandardAnalyzer(Version.LUCENE_29));
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

        IndexWriter w = new IndexWriter(indexUpdater.getDirectory(), new StandardAnalyzer(Version.LUCENE_29));
        w.close();
    }
}
