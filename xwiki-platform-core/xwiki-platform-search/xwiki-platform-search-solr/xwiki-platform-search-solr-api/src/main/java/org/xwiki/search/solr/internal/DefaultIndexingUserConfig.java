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
package org.xwiki.search.solr.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.IndexingUserConfig;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Provides the configured indexing user via information stored in a wiki page.
 *
 * @version $Id$
 * @since 14.8M1
 */
@Component
@Singleton
public class DefaultIndexingUserConfig implements IndexingUserConfig
{
    /**
     * The class storing the indexing user.
     */
    static final LocalDocumentReference CONFIG_CLASS
        = new LocalDocumentReference(XWiki.SYSTEM_SPACE, "SolrSearchAdminIndexingUserClass");

    /**
     * The wiki page storing the indexing user.
     */
    static final LocalDocumentReference CONFIG_PAGE
        = new LocalDocumentReference(XWiki.SYSTEM_SPACE, "SolrSearchAdminIndexingUser");

    /**
     * the name of the class field containing the indexing user.
     */
    static final String CONFIG_USER_ATTRIBUTE = "indexer";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public DocumentReference getIndexingUserReference() throws XWikiException
    {
        DocumentReference indexingUser = null;
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        WikiReference mainWikiRef = new WikiReference(context.getMainXWiki());

        XWikiDocument configPage = wiki.getDocument(new DocumentReference(CONFIG_PAGE, mainWikiRef),
            context);
        BaseObject configObject = configPage.getXObject(CONFIG_CLASS);

        if (configObject != null) {
            String indexers = configObject.getLargeStringValue(CONFIG_USER_ATTRIBUTE);
            for (String userRef : ListClass.getListFromString(indexers)) {
                if (StringUtils.isNotBlank(userRef)) {
                    indexingUser = documentReferenceResolver.resolve(userRef, mainWikiRef);
                    break;
                }
            }
        }

        return indexingUser;
    }
}
