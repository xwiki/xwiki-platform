package org.xwiki.extension.index.internal;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFileNameFilter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionManagerConfiguration;

// TODO: use NoLockFactory
@Component
@Singleton
public class DefaultLuceneIndex implements LuceneIndex, Initializable
{
    @Inject
    private ExtensionManagerConfiguration configuration;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private Directory indexDirectory;

    private IndexReader indexReader;

    private IndexWriter indexWriter;

    private IndexSearcher indexSearcher;

    public void initialize() throws InitializationException
    {
        try {
            open();
        } catch (IOException e) {
            throw new InitializationException("Failed to initialize lucene based extension index", e);
        }
    }

    private void open() throws IOException
    {
        this.indexDirectory =
            FSDirectory.open(new File(configuration.getHome(), "index"), NoLockFactory.getNoLockFactory());

        final boolean create = !IndexReader.indexExists(getIndexDirectory());

        this.indexWriter =
            new IndexWriter(getIndexDirectory(), new ExtensionAnalyzer(), create, MaxFieldLength.UNLIMITED);

        this.indexReader = IndexReader.open(getIndexDirectory(), true);

        this.indexSearcher = new IndexSearcher(this.indexReader);
    }

    private void close() throws IOException
    {
        this.indexReader.close();
        this.indexWriter.close();

    }

    public Lock readLock()
    {
        return lock.readLock();
    }

    public Lock writeLock()
    {
        return lock.writeLock();
    }

    public void clean() throws IOException
    {
        close();

        deleteIndex();

        open();
    }

    private void deleteIndex() throws IOException
    {
        String[] names = this.indexDirectory.listAll();

        if (names != null) {
            IndexFileNameFilter filter = IndexFileNameFilter.getFilter();

            for (int i = 0; i < names.length; i++) {
                if (filter.accept(null, names[i])) {
                    this.indexDirectory.deleteFile(names[i]);
                }
            }
        }
    }

    public Directory getIndexDirectory()
    {
        return this.indexDirectory;
    }

    public IndexReader getIndexReader()
    {
        return this.indexReader;
    }

    public IndexSearcher getIndexSearcher()
    {
        return this.indexSearcher;
    }

    public IndexWriter getIndexWriter()
    {
        return this.indexWriter;
    }

    public void optimize() throws CorruptIndexException, IOException
    {
        writeLock().lock();

        try {
            getIndexWriter().optimize(true);
        } finally {
            writeLock().unlock();
        }
    }
}
