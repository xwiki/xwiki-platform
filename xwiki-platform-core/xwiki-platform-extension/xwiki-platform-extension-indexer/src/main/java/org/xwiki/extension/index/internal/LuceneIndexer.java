package org.xwiki.extension.index.internal;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionCollectException;
import org.xwiki.extension.index.ExtensionIndexer;
import org.xwiki.extension.repository.ExtensionCollector;
import org.xwiki.extension.repository.ExtensionRepository;

@Component
@Singleton
public class LuceneIndexer implements ExtensionIndexer
{
    @Inject
    private LuceneIndex index;

    public void clear() throws IOException
    {
        this.index.clean();
    }

    public void addRepository(ExtensionRepository repository) throws ExtensionCollectException
    {
        repository.collectExtensions(new ExtensionCollector()
        {
            public void addExtension(Extension extension) throws ExtensionCollectException
            {
                try {
                    LuceneIndexer.this.addExtension(extension);
                } catch (Exception e) {
                    throw new ExtensionCollectException("Failed to index extension [" + extension + "]", e);
                }
            }
        });
    }

    private Document createDocument(Extension extension)
    {
        Document document = new Document();

        
        
        return document;
    }

    private void addExtension(Extension extension) throws CorruptIndexException, IOException
    {
        Document document = createDocument(extension);

        this.index.writeLock().lock();

        try {
            this.index.getIndexWriter().addDocument(document);
        } finally {
            this.index.writeLock().unlock();
        }
    }

    public TopDocs search(Query query, int n) throws IOException
    {
        return this.index.getIndexSearcher().search(query, n);
    }
}
