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
package org.xwiki.notifications.filters.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link ModelBridge}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    protected static final String FIELD_PAGES = "pages";

    protected static final String FIELD_SPACES = "spaces";

    protected static final String FIELD_WIKIS = "wikis";

    private static final SpaceReference NOTIFICATION_CODE_SPACE = new SpaceReference("Code",
            new SpaceReference("Notifications",
                    new SpaceReference("XWiki", new WikiReference("xwiki"))
            )
    );

    private static final DocumentReference NOTIFICATION_FILTER_PREFERENCE_CLASS = new DocumentReference(
            "NotificationFilterPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final DocumentReference TOGGLEABLE_FILTER_PREFERENCE_CLASS = new DocumentReference(
            "ToggleableFilterPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final String FIELD_FILTER_NAME = "filterName";

    private static final String FIELD_IS_ENABLED = "isEnabled";

    private static final String FIELD_IS_ACTIVE = "isActive";

    private static final String FILTER_PREFERENCE_NAME = "filterPreferenceName";

    private static final String FIELD_APPLICATIONS = "applications";

    private static final String FIELD_EVENT_TYPES = "eventTypes";

    private static final String FIELD_USERS = "users";

    private static final String FIELD_FILTER_TYPE = "filterType";

    private static final String FIELD_FILTER_FORMATS = "filterFormats";

    private static final String FIELD_STARTING_DATE = "startingDate";

    private static final String EXPLODED_PREFERENCES_PREFIX = "EXPLODED_";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ComponentManager componentManager;

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationFilterPreferenceClass
                = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());

        Set<NotificationFilterPreference> preferences = new HashSet<>();

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);
            List<BaseObject> preferencesObj = doc.getXObjects(notificationFilterPreferenceClass);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj != null) {
                        // IDEA: to avoid having too much xobjects, we decide to have only 1 xobject for all
                        // common properties, and using the ability of the relational storage to store several
                        // pages, spaces, wikis, etc...

                        Map<NotificationFilterProperty, List<String>> filterPreferenceProperties =
                                createNotificationFilterPropertiesMap(obj);

                        NotificationFilterType filterType = NotificationFilterType.valueOf(
                                obj.getStringValue(FIELD_FILTER_TYPE).toUpperCase());

                        Set<NotificationFormat> filterFormats = new HashSet<>();
                        for (String format : (List<String>) obj.getListValue(FIELD_FILTER_FORMATS)) {
                            filterFormats.add(NotificationFormat.valueOf(format.toUpperCase()));
                        }

                        // Create the new filter preference and add it to the list of preferences
                        DefaultNotificationFilterPreference notificationFilterPreference
                                = new DefaultNotificationFilterPreference(
                                        obj.getStringValue(FILTER_PREFERENCE_NAME));

                        notificationFilterPreference.setProviderHint("userProfile");
                        notificationFilterPreference.setFilterName(obj.getStringValue(FIELD_FILTER_NAME));
                        notificationFilterPreference.setEnabled(obj.getIntValue(FIELD_IS_ENABLED, 1) == 1);
                        notificationFilterPreference.setActive(obj.getIntValue(FIELD_IS_ACTIVE, 1) == 1);
                        notificationFilterPreference.setFilterType(filterType);
                        notificationFilterPreference.setNotificationFormats(filterFormats);
                        notificationFilterPreference.setPreferenceProperties(filterPreferenceProperties);
                        notificationFilterPreference.setStartingDate(obj.getDateValue(FIELD_STARTING_DATE));

                        addOrExplodePreference(preferences, notificationFilterPreference);
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences scope for the user [%s].",
                            user), e);
        }

        return preferences;
    }

    private void addOrExplodePreference(Set<NotificationFilterPreference> preferences,
            DefaultNotificationFilterPreference notificationFilterPreference)
    {
        boolean hasBeenExploded;

        hasBeenExploded = explodedFilterPreference(notificationFilterPreference, NotificationFilterProperty.PAGE,
                preferences);
        hasBeenExploded |= explodedFilterPreference(notificationFilterPreference, NotificationFilterProperty.SPACE,
                preferences);
        hasBeenExploded |= explodedFilterPreference(notificationFilterPreference, NotificationFilterProperty.WIKI,
                preferences);

        if (!hasBeenExploded) {
            preferences.add(notificationFilterPreference);
        }
    }

    private boolean explodedFilterPreference(DefaultNotificationFilterPreference notificationFilterPreference,
            NotificationFilterProperty property, Set<NotificationFilterPreference> preferences)
    {
        List<String> values = notificationFilterPreference.getProperties(property);
        int numberOfValues = values.size();
        if (numberOfValues > 0) {
            for (int i = 0; i < numberOfValues; ++i) {
                preferences.add(new ExplodedFilterPreference(notificationFilterPreference, property, i));
            }
            return true;
        }
        return false;
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
    public Map<String, Boolean> getToggeableFilterActivations(DocumentReference user) throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationPreferencesScopeClass
                = TOGGLEABLE_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());

        Map<String, Boolean> filterStatus = new HashMap<>();

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);
            for (NotificationFilter filter : componentManager.<NotificationFilter>getInstanceList(
                    NotificationFilter.class)) {
                if (filter instanceof ToggleableNotificationFilter) {
                    ToggleableNotificationFilter toggleableFilter = (ToggleableNotificationFilter) filter;
                    boolean status = toggleableFilter.isEnabledByDefault();
                    BaseObject obj = doc.getXObject(notificationPreferencesScopeClass, FIELD_FILTER_NAME,
                            filter.getName());
                    if (obj != null) {
                        status = obj.getIntValue(FIELD_IS_ENABLED, status ? 1 : 0) != 0;
                    }
                    filterStatus.put(filter.getName(), status);
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the toggleable filters preferences for the user [%s].",
                            user), e);
        }

        return filterStatus;
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceName) throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationFilterPreferenceClass
                = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());
        boolean shouldSave = false;

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);

            BaseObject obj = doc.getXObject(notificationFilterPreferenceClass, FILTER_PREFERENCE_NAME,
                    filterPreferenceName);
            if (obj != null) {
                doc.removeXObject(obj);
                shouldSave = true;
            } else if (filterPreferenceName.startsWith(EXPLODED_PREFERENCES_PREFIX)) {
                ExplodedFilterPreferenceReference reference
                        = new ExplodedFilterPreferenceReference(filterPreferenceName);
                obj = doc.getXObject(notificationFilterPreferenceClass, FILTER_PREFERENCE_NAME,
                        reference.getObjectName());
                if (obj != null) {
                    if (obj.getListValue(reference.getPropertyName()).remove(reference.getLocation())) {
                        shouldSave = true;

                        // Remove the whole object if it is now totally empty
                        if (obj.getListValue(FIELD_PAGES).isEmpty() && obj.getListValue(FIELD_SPACES).isEmpty()
                                && obj.getListValue(FIELD_WIKIS).isEmpty()) {
                            doc.removeXObject(obj);
                        }
                    }
                }
            }
            if (shouldSave) {
                xwiki.saveDocument(doc, String.format("Remove filter preference [%s].", filterPreferenceName), context);
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to delete filters [%s] for user [%s].", filterPreferenceName, user), e);
        }

    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceName, boolean enabled)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationFilterPreferenceClass
                = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());
        boolean shouldSave = false;

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);
            BaseObject obj = doc.getXObject(notificationFilterPreferenceClass, FILTER_PREFERENCE_NAME,
                    filterPreferenceName);
            if (obj != null) {
                obj.setIntValue(FIELD_IS_ENABLED, enabled ? 1 : 0);
                shouldSave = true;
            } else if (filterPreferenceName.startsWith(EXPLODED_PREFERENCES_PREFIX)) {

                Set<NotificationFilterPreference> existingPreferences = this.getFilterPreferences(user);
                for (NotificationFilterPreference existingPref : existingPreferences) {
                    if (filterPreferenceName.equals(existingPref.getFilterPreferenceName())) {
                        if (existingPref.isEnabled() != enabled) {
                            deleteFilterPreference(user, filterPreferenceName);
                            saveFilterPreferences(user,
                                    Arrays.asList(new NotificationFilterPreferenceWrapper(existingPref, enabled)));
                        }
                    }
                }
            }

            if (shouldSave) {
                // Make this change a minor edit so it's not displayed, by default, in notifications
                xwiki.saveDocument(doc, String.format("%s filter preference [%s].",
                        enabled ? "Enable" : "Disable", filterPreferenceName), true, context);
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to update enabled state filters [%s] for user [%s].",
                            filterPreferenceName, user), e);
        }
    }

    @Override
    public void saveFilterPreferences(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        if (user == null) {
            return;
        }

        // Usual XWiki objects
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationFilterPreferenceClass
                = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);

            for (NotificationFilterPreference prefToSave : filterPreferences) {
                saveFilterPreferences(context, notificationFilterPreferenceClass, doc, prefToSave);
            }

            // Make this change a minor edit so it's not displayed, by default, in notifications
            xwiki.saveDocument(doc, "Save notification filter preferences.", true, context);
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to save the notification preferences scope for the user [%s].", user),
                    e
            );
        }
    }

    private void saveFilterPreferences(XWikiContext context, DocumentReference notificationFilterPreferenceClass,
            XWikiDocument doc, NotificationFilterPreference prefToSave) throws XWikiException
    {
        // Get existing objects
        List<BaseObject> preferencesObj = doc.getXObjects(notificationFilterPreferenceClass);
        if (preferencesObj != null) {
            for (BaseObject obj : preferencesObj) {
                if (obj == null) {
                    continue;
                }

                // Get the filter preference corresponding to this base object
                // Since we now have the notion of ExplodedFilterPreference, we need to do some tricks here
                if (isObjectSimilarToPref(obj, prefToSave)) {
                    // Then update the base object
                    fillBaseObjectWithProperty(obj, prefToSave, FIELD_PAGES, NotificationFilterProperty.PAGE);
                    fillBaseObjectWithProperty(obj, prefToSave, FIELD_SPACES, NotificationFilterProperty.SPACE);
                    fillBaseObjectWithProperty(obj, prefToSave, FIELD_WIKIS, NotificationFilterProperty.WIKI);
                    // Object is saved, nothing to do more
                    return;
                }
            }
        }

        // Create a new one if no one matches
        int objNumber = doc.createXObject(notificationFilterPreferenceClass, context);
        BaseObject obj = doc.getXObject(notificationFilterPreferenceClass, objNumber);
        convertFilterPreferenceToBaseObject(prefToSave, obj);
    }

    private void fillBaseObjectWithProperty(BaseObject obj, NotificationFilterPreference prefToSave,
            String fieldName, NotificationFilterProperty property)
    {
        Set<String> pages = new HashSet<>(obj.getListValue(fieldName));
        if (pages.addAll(prefToSave.getProperties(property))) {
            obj.setDBStringListValue(fieldName, new ArrayList(pages));
        }
    }

    private boolean isObjectSimilarToPref(BaseObject obj, NotificationFilterPreference prefToSave)
    {
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(obj.getStringValue(FIELD_FILTER_NAME), prefToSave.getFilterName());
        equalsBuilder.append(obj.getIntValue(FIELD_IS_ENABLED) == 1, prefToSave.isEnabled());
        equalsBuilder.append(obj.getIntValue(FIELD_IS_ACTIVE) == 1, prefToSave.isActive());
        equalsBuilder.append(obj.getStringValue(FIELD_FILTER_TYPE), prefToSave.getFilterType().name().toLowerCase());
        equalsBuilder.append(obj.getListValue(FIELD_FILTER_FORMATS),
                toCollectionOfStrings(prefToSave.getFilterFormats()));
        equalsBuilder.append(obj.getListValue(FIELD_APPLICATIONS),
                prefToSave.getProperties(NotificationFilterProperty.APPLICATION));
        equalsBuilder.append(obj.getListValue(FIELD_EVENT_TYPES),
                prefToSave.getProperties(NotificationFilterProperty.EVENT_TYPE));
        equalsBuilder.append(obj.getListValue(FIELD_USERS),
                prefToSave.getProperties(NotificationFilterProperty.USER));
        // All other fields are ignored
        return equalsBuilder.isEquals();
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        // Usual XWiki objects
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationFilterPreferenceClass
                = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);

            List<BaseObject> preferencesObj = doc.getXObjects(notificationFilterPreferenceClass);
            if (preferencesObj == null) {
                return;
            }
            boolean needSave = false;
            for (BaseObject object : preferencesObj) {
                if (object == null) {
                    continue;
                }
                object.setDateValue(FIELD_STARTING_DATE, startDate);
                needSave = true;
            }
            if (needSave) {
                // Make this change a minor edit so it's not displayed, by default, in notifications
                xwiki.saveDocument(doc, "Save notification filter preferences starting date.", true,
                        context);
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to save the notification preferences starting date for the user [%s].", user),
                    e
            );
        }
    }

    /**
     * Fill the values of the given XObject with the values of the given filter preference.
     * @param filterPreference the filter preference to save
     * @param obj the object to fill
     */
    private void convertFilterPreferenceToBaseObject(NotificationFilterPreference filterPreference, BaseObject obj)
    {
        obj.setStringValue(FILTER_PREFERENCE_NAME, filterPreference.getFilterPreferenceName());
        obj.setStringValue(FIELD_FILTER_NAME, filterPreference.getFilterName());
        obj.setIntValue(FIELD_IS_ENABLED, filterPreference.isEnabled() ? 1 : 0);
        obj.setIntValue(FIELD_IS_ACTIVE, filterPreference.isActive() ? 1 : 0);
        obj.setStringValue(FIELD_FILTER_TYPE, filterPreference.getFilterType().name().toLowerCase());
        obj.setDBStringListValue(FIELD_FILTER_FORMATS,
                toCollectionOfStrings(filterPreference.getFilterFormats()));
        obj.setDBStringListValue(FIELD_APPLICATIONS,
                filterPreference.getProperties(NotificationFilterProperty.APPLICATION));
        obj.setDBStringListValue(FIELD_EVENT_TYPES,
                filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE));
        obj.setDBStringListValue(FIELD_PAGES,
                filterPreference.getProperties(NotificationFilterProperty.PAGE));
        obj.setDBStringListValue(FIELD_SPACES,
                filterPreference.getProperties(NotificationFilterProperty.SPACE));
        obj.setDBStringListValue(FIELD_WIKIS,
                filterPreference.getProperties(NotificationFilterProperty.WIKI));
        obj.setDBStringListValue(FIELD_USERS,
                filterPreference.getProperties(NotificationFilterProperty.USER));
        obj.setDateValue(FIELD_STARTING_DATE, filterPreference.getStartingDate());
    }

    private List<String> toCollectionOfStrings(Collection<NotificationFormat> formats)
    {
        // TODO: improve this
        List<String> results = new ArrayList<>();
        for (NotificationFormat format : formats) {
            results.add(format.name().toLowerCase());
        }
        return results;
    }
}
