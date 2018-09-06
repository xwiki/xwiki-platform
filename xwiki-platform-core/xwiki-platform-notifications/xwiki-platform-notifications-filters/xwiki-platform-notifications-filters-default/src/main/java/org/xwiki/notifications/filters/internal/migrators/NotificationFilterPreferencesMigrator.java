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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.ModelBridge;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * @version $Id$
 * @since 10.8RC1
 */
@Component
@Named("R108000NotificationFilterPreferenceMigration")
@Singleton
public class NotificationFilterPreferencesMigrator extends AbstractHibernateDataMigration
{
    private static final SpaceReference NOTIFICATION_CODE_SPACE = new SpaceReference("Code",
            new SpaceReference("Notifications",
                    new SpaceReference("XWiki", new WikiReference("xwiki"))
            )
    );

    private static final DocumentReference NOTIFICATION_FILTER_PREFERENCE_CLASS = new DocumentReference(
            "NotificationFilterPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final String FIELD_FILTER_NAME = "filterName";

    private static final String FIELD_IS_ENABLED = "isEnabled";

    private static final String FIELD_IS_ACTIVE = "isActive";

    private static final String FIELD_APPLICATIONS = "applications";

    private static final String FIELD_EVENT_TYPES = "eventTypes";

    private static final String FIELD_PAGES = "pages";

    private static final String FIELD_SPACES = "spaces";

    private static final String FIELD_WIKIS = "wikis";

    private static final String FIELD_USERS = "users";

    private static final String FIELD_FILTER_TYPE = "filterType";

    private static final String FIELD_FILTER_FORMATS = "filterFormats";

    private static final String FIELD_STARTING_DATE = "startingDate";

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Logger logger;

    private void migrateUser(DocumentReference user) throws NotificationException
    {
        logger.info("Migrating the notification filter preferences of user [{}].", user);

        XWikiContext context = this.getXWikiContext();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationFilterPreferenceClass
                = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());

        try {
            logger.info("Loading the current notification filter preferences of user [{}].", user);
            XWikiDocument doc = xwiki.getDocument(user, context);

            // Get the old preferences
            List<NotificationFilterPreference> preferencesToSave = convertXObjectsToPreferences(doc,
                    notificationFilterPreferenceClass);

            // Make sure we have not already saved in the database the migrated preferences (it would mean the migration
            // has already been executed but stopped while the user's page was saving)
            Set<NotificationFilterPreference> preferencesInTheNewStore = modelBridge.getFilterPreferences(user);
            if (!modelBridge.getFilterPreferences(user).isEmpty()
                    && preferencesInTheNewStore.size() == preferencesToSave.size()) {
                logger.info("It seems the notification filter preferences of user [{}] has already been migrated,"
                        + " but the old ones have not been removed from the user's page yet. Probably a previous"
                        + " migration has been run but stopped in the middle of the process.", user);
            } else {
                // Save to the new store
                logger.info("Saving the migrated notification filter preferences of user [{}] in the new store.", user);
                modelBridge.saveFilterPreferences(user, preferencesToSave);
            }

            // Remove the old xobjects
            logger.info("Removing the old notification filter preferences in the page of the user [{}] "
                    + "(please wait, it could be long).", user);
            doc.removeXObjects(notificationFilterPreferenceClass);
            xwiki.saveDocument(doc, "Migrate notification filter preferences to the new store.", context);

        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to migrate the notification preferences for the user [%s].",
                            user), e);
        }
    }

    private List<NotificationFilterPreference> convertXObjectsToPreferences(XWikiDocument document,
            DocumentReference notificationFilterPreferenceClass)
    {
        List<NotificationFilterPreference> preferencesToConvert = new ArrayList<>();

        List<BaseObject> preferencesObj = document.getXObjects(notificationFilterPreferenceClass);
        if (preferencesObj != null) {
            for (BaseObject obj : preferencesObj) {
                if (obj != null) {
                    handleObject(preferencesToConvert, obj);
                }
            }
        }

        return preferencesToConvert;
    }

    private void handleObject(List<NotificationFilterPreference> preferencesToConvert, BaseObject obj)
    {
        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference();

        NotificationFilterType filterType = NotificationFilterType.valueOf(
                obj.getStringValue(FIELD_FILTER_TYPE).toUpperCase());

        Set<NotificationFormat> filterFormats = new HashSet<>();
        for (String format : (List<String>) obj.getListValue(FIELD_FILTER_FORMATS)) {
            filterFormats.add(NotificationFormat.valueOf(format.toUpperCase()));
        }

        preference.setProviderHint("userProfile");
        preference.setFilterName(obj.getStringValue(FIELD_FILTER_NAME));
        preference.setEnabled(obj.getIntValue(FIELD_IS_ENABLED, 1) == 1);
        preference.setActive(obj.getIntValue(FIELD_IS_ACTIVE, 1) == 1);
        preference.setFilterType(filterType);
        preference.setNotificationFormats(filterFormats);
        preference.setStartingDate(obj.getDateValue(FIELD_STARTING_DATE));

        handleProperties(preferencesToConvert, obj, preference);
    }

    private void handleProperties(List<NotificationFilterPreference> preferencesToConvert,
            BaseObject obj, DefaultNotificationFilterPreference preference)
    {
        Map<NotificationFilterProperty, List<String>> filterPreferenceProperties =
                createNotificationFilterPropertiesMap(obj);

        if (!filterPreferenceProperties.get(NotificationFilterProperty.EVENT_TYPE).isEmpty()) {
            preference.setEventTypes(
                    new HashSet<>(filterPreferenceProperties.get(NotificationFilterProperty.EVENT_TYPE)));
        }

        for (String page : filterPreferenceProperties.get(NotificationFilterProperty.PAGE)) {
            DefaultNotificationFilterPreference pref
                    = new DefaultNotificationFilterPreference(preference);
            pref.setPageOnly(page);
            preferencesToConvert.add(pref);
        }
        for (String space : filterPreferenceProperties.get(NotificationFilterProperty.SPACE)) {
            DefaultNotificationFilterPreference pref
                    = new DefaultNotificationFilterPreference(preference);
            pref.setPage(space);
            preferencesToConvert.add(pref);
        }
        for (String wiki : filterPreferenceProperties.get(NotificationFilterProperty.WIKI)) {
            DefaultNotificationFilterPreference pref
                    = new DefaultNotificationFilterPreference(preference);
            pref.setWiki(wiki);
            preferencesToConvert.add(pref);
        }
        for (String user : filterPreferenceProperties.get(NotificationFilterProperty.USER)) {
            DefaultNotificationFilterPreference pref
                    = new DefaultNotificationFilterPreference(preference);
            pref.setUser(user);
            preferencesToConvert.add(pref);
        }
        // We don't handle the property APPLICATIONS that is not here anymore
    }

    private Map<NotificationFilterProperty, List<String>> createNotificationFilterPropertiesMap(BaseObject obj)
    {
        Map<NotificationFilterProperty, List<String>> filterPreferenceProperties = new HashMap<>();

        filterPreferenceProperties.put(NotificationFilterProperty.APPLICATION,
                obj.getListValue(FIELD_APPLICATIONS));
        filterPreferenceProperties.put(NotificationFilterProperty.EVENT_TYPE,
                obj.getListValue(FIELD_EVENT_TYPES));
        filterPreferenceProperties.put(NotificationFilterProperty.PAGE,
                obj.getListValue(FIELD_PAGES));
        filterPreferenceProperties.put(NotificationFilterProperty.SPACE,
                obj.getListValue(FIELD_SPACES));
        filterPreferenceProperties.put(NotificationFilterProperty.WIKI,
                obj.getListValue(FIELD_WIKIS));
        filterPreferenceProperties.put(NotificationFilterProperty.USER,
                obj.getListValue(FIELD_USERS));
        return filterPreferenceProperties;
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        WikiReference currentWiki = getXWikiContext().getWikiReference();

        try {
            logger.info("Getting the list of the users having notification filter preferences to migrate.");
            Query query = queryManager.createQuery(
                    "select distinct doc.fullName from Document doc, "
                            + "doc.object(XWiki.Notifications.Code.NotificationFilterPreferenceClass) obj", Query.XWQL);
            for (String fullName : query.<String>execute()) {
                migrateUser(referenceResolver.resolve(fullName, currentWiki));
            }

            // Remove the useless class when all user have been migrated (not to trash because the trash might have not
            // been initialized yet since we are in a migrator).
            XWikiContext context = this.getXWikiContext();
            XWiki xwiki = context.getWiki();
            final DocumentReference notificationFilterPreferenceClass
                    = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(currentWiki);
            XWikiDocument oldClassDoc = xwiki.getDocument(notificationFilterPreferenceClass, context);
            if (!oldClassDoc.isNew()) {
                logger.info("Removing the old notification filter preference class.");
                xwiki.deleteDocument(oldClassDoc, false, context);
            }

        } catch (Exception e) {
            throw new DataMigrationException("Failed to migrate notification filter preferences.", e);
        }
    }

    @Override
    public String getDescription()
    {
        return "Move NotificationFilterPreferenceClass XObjects to the new store";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // Migrator introduced in XWiki 9.11.8 but also in 10.8RC1... Hope it's gonna be ok
        return new XWikiDBVersion(1008000);
    }
}
