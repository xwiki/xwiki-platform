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
package org.xwiki.wiki.user.internal.membermigration;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;

/**
 * Default implementation for {@link org.xwiki.wiki.user.internal.membermigration.MemberRightsMigrator}.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Component
public class DefaultMemberRightsMigrator implements MemberRightsMigrator
{
    private static final String PROPERTY_NAME = "groups";

    private static final String LIST_SEPARATOR = ",";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public void upgradeRights(String wikiId) throws DataMigrationException
    {
        upgradeRights("XWikiRights", wikiId);
        upgradeRights("XWikiGlobalRights", wikiId);
    }

    private void upgradeRights(String rightsClass, String wikiId) throws DataMigrationException
    {
        // Get XWiki objects
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        DocumentReference classReference = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, rightsClass);

        try {
            String queryStatement = String.format("from doc.object(XWiki.%s) objRight WHERE"
                    + " objRight.groups like '%%XWiki.XWikiAllGroup%%'", rightsClass);
            Query query = queryManager.createQuery(queryStatement, Query.XWQL);
            query.addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "unique"));
            query.setWiki(wikiId);
            List<String> documentNames = query.execute();
            WikiReference currentWikiRef = new WikiReference(wikiId);
            for (String docName : documentNames) {
                DocumentReference docRef = documentReferenceResolver.resolve(docName, currentWikiRef);
                XWikiDocument doc = xwiki.getDocument(docRef, xcontext);
                upgradeRightsForDocument(doc, classReference);
            }

        } catch (QueryException e) {
            throw new DataMigrationException(String.format("Failed to create a query to get all document containing"
                    + " rights set for XWiki.XWikiAllGroup in wiki [%s].", wikiId), e);
        } catch (ComponentLookupException e) {
            throw new DataMigrationException("Failed to get the query filter \"unique\".", e);
        } catch (XWikiException e) {
            throw new DataMigrationException(String.format("Failed to get or save documents in the wiki [%s].", wikiId),
                    e);
        }
    }

    private void upgradeRightsForDocument(XWikiDocument doc, DocumentReference classReference) throws XWikiException
    {
        // Get XWiki objects
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Get the rights objects
        List<BaseObject> objects = doc.getXObjects(classReference);
        if (objects == null) {
            // Then there is nothing to do
            return;
        }

        boolean modified = false;
        // For each object
        for (BaseObject obj : objects) {
            // The object might be null
            if (obj == null) {
                continue;
            }
            // Get the values
            String groupsValue = obj.getLargeStringValue(PROPERTY_NAME);
            String[] groups = groupsValue.split(LIST_SEPARATOR);
            for (int i = 0; i < groups.length; ++i) {
                String groupValue = groups[i];
                // Change the value if it matches XWikiAllGroup
                if (groupValue != null && groupValue.equals("XWiki.XWikiAllGroup")) {
                    groups[i] = "XWiki.XWikiMemberGroup";
                    modified = true;
                }
            }

            if (modified) {
                // Set the new values
                String newGroupsValue = groups[0];
                for (int i = 1; i < groups.length; ++i) {
                    newGroupsValue += LIST_SEPARATOR + groups[i];
                }
                obj.setLargeStringValue(PROPERTY_NAME, newGroupsValue);
            }
        }

        // Save the document
        if (modified) {
            xwiki.saveDocument(doc, "Set rights for XWikiMemberGroup", xcontext);
        }
    }
}
