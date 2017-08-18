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
package org.xwiki.notifications.filters.internal.migrators;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * This migrator is designed to move old NotificationPreferenceScopeClass XObjects contained in user profiles
 * to new (and more generic) NotificationFilterPreferenceClass XObjects.
 *
 * @since 9.8RC1
 * @version $Id$
 */
@Component
@Named("R98000NotificationPreferenceScopeMigration")
@Singleton
public class ScopeNotificationFilterClassMigrator extends AbstractHibernateDataMigration
{
    private static final LocalDocumentReference OLD_XCLASS_REFERENCE =
            new LocalDocumentReference(Arrays.asList("XWiki", "Notifications", "Code"),
                    "NotificationPreferenceScopeClass");

    private static final LocalDocumentReference NEW_XCLASS_REFERENCE =
            new LocalDocumentReference(Arrays.asList("XWiki", "Notifications", "Code"),
                    "NotificationFilterPreferenceClass");

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Move old NotificationPreferenceScopeClass XObjects to the more generic "
                + "NotificationFilterPreferenceClass XObjects.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(98000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return (startupVersion.getVersion() < 98000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        XWikiContext xWikiContext = xcontextProvider.get();

        try {
            // Get every document having at least one NotificationPreferenceScopeClass XObject attached
            Query query = queryManager.createQuery(
                            "select distinct doc.fullName from Document doc, "
                                + "doc.object(XWiki.Notifications.Code.NotificationPreferenceScopeClass) as document",
                            Query.XWQL);

            List<String> results = query.execute();

            // Migrate each document to use the new XObject
            DocumentReference currentDocumentReference;
            for (String result : results) {
                currentDocumentReference = documentReferenceResolver.resolve(result);
                XWikiDocument document = xWikiContext.getWiki().getDocument(currentDocumentReference, xWikiContext);

                migrateDocument(document);
            }

            // When every document has been migrated, we can then delete the old XClass
            xWikiContext.getWiki().deleteDocument(
                    xWikiContext.getWiki().getDocument(OLD_XCLASS_REFERENCE, xWikiContext), xWikiContext);

        } catch (QueryException e) {
            // TODO: Improve error log.
            logger.error("Failed to perform a query on the current wiki.", e);
        }
    }

    /**
     * Given a {@link XWikiDocument}, update the document XObjects to the new NotificationPreferenceFilterClass.
     *
     * @param document the document to migrate
     */
    private void migrateDocument(XWikiDocument document)
    {
        List<BaseObject> oldXObjects = document.getXObjects(OLD_XCLASS_REFERENCE);

        for (BaseObject oldXObject : oldXObjects) {
            document.addXObject(generateNewXObject(oldXObject));
        }

        document.removeXObjects(OLD_XCLASS_REFERENCE);
    }

    private BaseObject generateNewXObject(BaseObject oldXObject)
    {
        BaseObject newXObject = new BaseObject();

        newXObject.setXClassReference(NEW_XCLASS_REFERENCE);

        String oldEventType = oldXObject.getStringValue("eventType");
        String oldFilterFormat = oldXObject.getStringValue("format");
        String oldFilterType = oldXObject.getStringValue("scopeFilterType");
        String oldScopeReferenceValue = oldXObject.getStringValue("scopeReference");
        String oldScopeValue = oldXObject.getStringValue("scope");

        // We have to generate a filter preference name that should be unique.
        newXObject.setStringValue("filterPreferenceName",
                String.format("scopeNotificationFilter-%s-%s-%s-%s-%s", oldEventType, oldFilterFormat, oldFilterType,
                        oldScopeValue, oldScopeReferenceValue));
        newXObject.setStringValue("filterName", "scopeNotificationFilter");
        newXObject.setStringValue("filterType", oldFilterType);
        newXObject.setStringListValue("filterFormats", Collections.singletonList(oldFilterFormat));

        List<String> oldXObjectScopeReference = Collections.singletonList(oldScopeReferenceValue);

        // Regarding the value of the old XObject scope, insert the scope reference in a particular field of the new
        // XObject
        switch (oldScopeValue) {
            case "pageOnly":
                newXObject.setStringListValue("pages", oldXObjectScopeReference);
                break;
            case "pageAndChildren":
                newXObject.setStringListValue("spaces", oldXObjectScopeReference);
                break;
            case "wiki":
                newXObject.setStringListValue("wikis", oldXObjectScopeReference);
                break;
        }

        return newXObject;
    }
}
