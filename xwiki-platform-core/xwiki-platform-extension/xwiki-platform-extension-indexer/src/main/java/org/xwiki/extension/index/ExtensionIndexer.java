package org.xwiki.extension.index;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.extension.ExtensionCollectException;
import org.xwiki.extension.repository.ExtensionRepository;

@ComponentRole
public interface ExtensionIndexer
{
    public void clear() throws IOException;

    public void addRepository(ExtensionRepository repository) throws ExtensionCollectException;

    public TopDocs search(Query query, int n) throws IOException;
}
