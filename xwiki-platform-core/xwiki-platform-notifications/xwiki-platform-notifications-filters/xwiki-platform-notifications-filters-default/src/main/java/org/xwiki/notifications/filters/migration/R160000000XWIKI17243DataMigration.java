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
package org.xwiki.notifications.filters.migration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.notifications.filters.internal.UserProfileNotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

/**
 * Migrate old WatchListClass xobjects to save them as proper notification filters. The migration doesn't directly
 * remove the xobjects but asks {@link WatchListObjectsRemovalTaskConsumer} to do it.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@Component
@Named("R160000000XWIKI17243")
@Singleton
public class R160000000XWIKI17243DataMigration extends AbstractHibernateDataMigration
{
    private static final String WATCHLIST_CLASSNAME = "WatchListClass";

    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", WATCHLIST_CLASSNAME);

    private static final String FIELD_WIKIS = "wikis";

    private static final String FIELD_SPACES = "spaces";

    private static final String FIELD_DOCUMENTS = "documents";

    private static final String WATCHLIST_FILTER_PREFERENCES_NAME = "watchlist_%s_%s";

    private static final int BATCH_SIZE = 100;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("unique")
    private QueryFilter uniqueFilter;

    @Inject
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private TaskManager taskManager;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Migrate old WatchListClass xobjects to proper filters.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(160000000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        String statement = "from doc.object(XWiki.WatchListClass) as watchListDoc";
        int offset = 0;
        List<String> results;
        do {
            try {
                results = this.queryManager.createQuery(statement, Query.XWQL)
                    .addFilter(uniqueFilter)
                    .setOffset(offset)
                    .setLimit(BATCH_SIZE).execute();

                this.logger.info("Found [{}] users with WatchListClass objects... migrating them.", results.size());
                for (String result : results) {
                    DocumentReference watchListDoc = this.documentReferenceResolver.resolve(result);
                    this.migrateWatchListDoc(watchListDoc);
                }
                offset += BATCH_SIZE;
            } catch (QueryException e) {
                throw new DataMigrationException("Error while performing query to find users with WatchList objects",
                    e);
            }
        } while (results.size() == BATCH_SIZE);
    }

    private void migrateWatchListDoc(DocumentReference watchListDocReference)
        throws DataMigrationException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        WikiReference wikiReference = watchListDocReference.getWikiReference();
        XWikiDocument document = context.getWiki().getDocument(watchListDocReference, context);

        // If the document does contain a user xobject, we perform the migration, else we only remove the xobject.
        if (document.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE) != null) {
            EntityReference classReference = CLASS_REFERENCE.appendParent(wikiReference);
            List<BaseObject> objects = document.getXObjects(classReference);

            Set<NotificationFilterPreference> results = new HashSet<>();
            for (BaseObject obj : objects) {
                if (obj == null) {
                    continue;
                }
                getValues(obj, FIELD_WIKIS, NotificationFilterProperty.WIKI, results);
                getValues(obj, FIELD_SPACES, NotificationFilterProperty.SPACE, results);
                getValues(obj, FIELD_DOCUMENTS, NotificationFilterProperty.PAGE, results);
            }

            try {
                this.notificationFilterPreferenceStore.saveFilterPreferences(watchListDocReference, results);
            } catch (NotificationException e) {
                throw new DataMigrationException(String.format("Error while trying to save [%s] filter preferences",
                    results.size()), e);
            }
        } else {
            this.logger.info("[{}] contained a watchlist object but is not a user profile, the object will be removed "
                + "without migration", watchListDocReference);
        }

        this.taskManager.addTask(wikiReference.getName(), document.getId(),
            WatchListObjectsRemovalTaskConsumer.TASK_NAME);
    }

    private void getValues(BaseObject obj, String fieldName, NotificationFilterProperty property,
        Set<NotificationFilterPreference> results)
    {
        List<String> values;
        PropertyInterface objProperty = obj.safeget(fieldName);
        // Support both pre and post 7.0 type of watchlist objects
        if (objProperty instanceof ListProperty) {
            values = ((ListProperty) objProperty).getList();
        } else if (objProperty instanceof LargeStringProperty) {
            values = ListClass.getListFromString(((LargeStringProperty) objProperty).getValue(), ",", false);
        } else {
            values = Collections.emptyList();
        }

        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                if (value != null) {
                    DefaultNotificationFilterPreference pref = createNotificationFilterPreference(
                        String.format(WATCHLIST_FILTER_PREFERENCES_NAME, property.name(), sha256Hex(value)));
                    switch (property) {
                        case PAGE:
                            pref.setPageOnly(value);
                            break;
                        case SPACE:
                            pref.setPage(value);
                            break;
                        case WIKI:
                            pref.setWiki(value);
                            break;
                        default:
                            break;
                    }
                    results.add(pref);
                }
            }
        }
    }

    private DefaultNotificationFilterPreference createNotificationFilterPreference(String id)
    {
        DefaultNotificationFilterPreference pref = new DefaultNotificationFilterPreference();
        pref.setId(id);
        pref.setEnabled(true);
        pref.setNotificationFormats(Set.of(NotificationFormat.values()));
        pref.setProviderHint(UserProfileNotificationFilterPreferenceProvider.HINT);
        pref.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        pref.setFilterType(NotificationFilterType.INCLUSIVE);
        return pref;
    }
}
