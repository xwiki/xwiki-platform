package org.xwiki.extension.index.internal;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface LuceneIndex
{
    Lock readLock();

    Lock writeLock();

    void clean() throws IOException;

    void optimize() throws CorruptIndexException, IOException;

    IndexReader getIndexReader();

    IndexSearcher getIndexSearcher();

    IndexWriter getIndexWriter();

    Directory getIndexDirectory();
}
