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
package org.xwiki.wiki.workspacesmigrator.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {$link SearchSuggestCustomConfigDeleter}.
 *
 * @since 5.3RC1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultSearchSuggestCustomConfigDeleter implements SearchSuggestCustomConfigDeleter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void deleteSearchSuggestCustomConfig(String wikiId) throws XWikiException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        DocumentReference searchConfigDocRef = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, "SearchSuggestConfig");
        DocumentReference searchConfigClass = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE,
                "SearchSuggestSourceClass");

        XWikiDocument searchConfigDoc = xwiki.getDocument(searchConfigDocRef, xcontext);

        // Get the config objects
        List<BaseObject> objects = searchConfigDoc.getXObjects(searchConfigClass);
        if (objects != null) {
            boolean found = false;
            // Find the object to remove
            for (BaseObject object : objects) {
                if (object == null) {
                    continue;
                }
                // Look if the object is to remove
                String name = object.getStringValue("name");
                if (name.equals("platform.workspace.searchSuggestSourceWorkspaces")) {
                    String query = object.getStringValue("query");
                    String engine = object.getStringValue("engine");
                    String url = object.getStringValue("url");

                    if (isSolrObject(query, engine, url) || isLuceneObject(query, engine, url)) {
                        searchConfigDoc.removeXObject(object);
                        found = true;
                    }
                }
            }

            if (found) {
                xwiki.saveDocument(searchConfigDoc, "Remove object previously introduced by WorkspaceManager.Install",
                        xcontext);
            }
        }
    }

    /**
     * Check if the object is the the config object for SolR.
     */
    private boolean isSolrObject(String query, String engine, String url)
    {
        return engine != null && engine.equals("solr") && query.equals(
               "class:XWiki.XWikiServerClass AND propertyname:wikiprettyname AND propertyvalue__:(__INPUT__*)")
               && url.equals("xwiki:WorkspaceManager.WorkspacesSuggestSolrService");
    }

    /**
     * Check if the object is the the config object for lucene.
     */
    private boolean isLuceneObject(String query, String engine, String url)
    {
        return (engine == null || engine.equals("lucene")) && query.equals(
               "XWiki.XWikiServerClass.wikiprettyname:__INPUT__* AND object:WorkspaceManager.WorkspaceClass")
               && url.equals("xwiki:WorkspaceManager.WorkspacesSuggestLuceneService");
    }
}
