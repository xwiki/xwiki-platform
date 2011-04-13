package org.xwiki.extension.index.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Fieldable;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepositoryManager;

@Component
@Singleton
public class DefaultDocumentSerializer implements DocumentSerializer
{
    private static final Set<String> STANDARDKEYS = new HashSet<String>(Arrays.asList(FKEY_UID, FKEY_ID, FKEY_VERSION,
        FKEY_TYPE, FKEY_NAME, FKEY_DESCRIPTION, FKEY_WEBSITE, FKEY_AUTHOR, FKEY_REPOSITORYID));

    @Inject
    private ExtensionRepositoryManager repositories;

    public String getUid(Extension extension)
    {
        return extension.getId().getId() + "|" + extension.getId().getVersion() + "|"
            + extension.getRepository().getId().getId();
    }

    public Document createDocument(Extension extension)
    {
        Document document = new Document();

        document.add(new Field(FKEY_UID, getUid(extension), Store.YES, Index.NOT_ANALYZED));

        document.add(new Field(FKEY_ID, extension.getId().getId(), Store.YES, Index.ANALYZED));
        document.add(new Field(FKEY_VERSION, extension.getId().getVersion(), Store.YES, Index.ANALYZED));
        document
            .add(new Field(FKEY_REPOSITORYID, extension.getRepository().getId().getId(), Store.YES, Index.ANALYZED));

        document.add(new Field(FKEY_TYPE, extension.getType(), Store.YES, Index.ANALYZED));
        document.add(new Field(FKEY_NAME, extension.getName(), Store.YES, Index.ANALYZED));
        document.add(new Field(FKEY_DESCRIPTION, extension.getDescription(), Store.YES, Index.ANALYZED));
        document.add(new Field(FKEY_AUTHOR, extension.getAuthor(), Store.YES, Index.ANALYZED));
        document.add(new Field(FKEY_WEBSITE, extension.getWebSite(), Store.YES, Index.ANALYZED));

        for (Map.Entry<String, Object> entry : extension.getProperties().entrySet()) {
            // TODO: add better support for lists values

            document.add(new Field(entry.getKey(), entry.getValue().toString(), Store.YES, Index.ANALYZED));
        }

        return document;
    }

    public Extension createExtension(Document document)
    {
        IndexedExtension extension = new IndexedExtension();

        extension.setId(new ExtensionId(document.get(FKEY_ID), document.get(FKEY_VERSION)));
        extension.setRepository(this.repositories.getRepository(document.get(FKEY_REPOSITORYID)));

        extension.setType(document.get(FKEY_TYPE));
        extension.setName(document.get(FKEY_NAME));
        extension.setDescription(document.get(FKEY_DESCRIPTION));
        extension.setAuthor(document.get(FKEY_AUTHOR));
        extension.setWebsite(document.get(FKEY_WEBSITE));

        // get custom properties
        for (Fieldable field : (List<Fieldable>) document.getFields()) {
            if (!STANDARDKEYS.contains(field.name())) {
                extension.putProperty(field.name(), field.stringValue());
            }
        }

        return extension;
    }
}
