package org.xwiki.extension.index.internal;

import org.apache.lucene.document.Document;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.extension.Extension;

@ComponentRole
public interface DocumentSerializer
{
    String FKEY_UID = "uid";

    String FKEY_ID = "id";

    String FKEY_VERSION = "version";

    String FKEY_TYPE = "type";

    String FKEY_NAME = "name";

    String FKEY_DESCRIPTION = "description";

    String FKEY_WEBSITE = "website";

    String FKEY_AUTHOR = "author";

    String FKEY_REPOSITORYID = "repositoryid";

    String getUid(Extension extension);

    Document createDocument(Extension extension);

    Extension createExtension(Document document);
}
