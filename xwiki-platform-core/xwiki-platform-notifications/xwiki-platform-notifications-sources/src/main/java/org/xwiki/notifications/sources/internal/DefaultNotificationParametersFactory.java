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
package org.xwiki.notifications.sources.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.SystemUserNotificationFilter;
import org.xwiki.notifications.filters.internal.minor.MinorEventAlertNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.status.EventReadAlertFilter;
import org.xwiki.notifications.filters.internal.status.ForUserEventFilter;
import org.xwiki.notifications.filters.internal.user.OwnEventFilter;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * This component aims at producing {@link org.xwiki.notifications.sources.NotificationParameters} instances
 * based on some given criteria.
 * It has been introduced to help dealing with an huge amount of parameters available in old API such as the
 * notification REST API.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component(roles = DefaultNotificationParametersFactory.class)
@Singleton
public class DefaultNotificationParametersFactory
{
    private static final String FIELD_SEPARATOR = ",";

    @Inject
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private NotificationConfiguration configuration;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private UsersParameterHandler usersParameterHandler;

    @Inject
    private Logger logger;

    /**
     * Define the parameters that will be taken into account when creating a {@link NotificationParameters} in
     * {@link #createNotificationParameters(Map)}.
     */
    public enum ParametersKey {
        /**
         * See {@link NotificationParameters#format}: accepted values are the {@link NotificationFormat} names.
         */
        FORMAT(true),

        /**
         * See {@link NotificationParameters#user}: accepted values are serialized references of a user document.
         */
        USER_ID(true),

        /**
         * This parameter only accept boolean values.
         * If {@code true} then the filters and preferences of the {@link NotificationParameters} will be only
         * retrieved from the given user preferences (a {@link #USER_ID} must be set in that case).
         * If {@code false} the following parameters are taken into account for the filters of
         * {@link NotificationParameters}:
         * PAGES, SPACES, WIKIS, USERS, TAGS, DISPLAY_OWN_EVENTS, DISPLAY_MINOR_EVENTS, DISPLAY_SYSTEM_EVENTS,
         * DISPLAY_READ_EVENTS, CURRENT_WIKI.
         */
        USE_USER_PREFERENCES(false),

        /**
         * See {@link NotificationParameters#endDate}: accepted values are date serialized as a long.
         */
        UNTIL_DATE(true),

        /**
         * See {@link NotificationParameters#blackList}: accepted values are list represented as a String with commas
         * used as separators. .
         */
        BLACKLIST(true),

        /**
         * See {@link NotificationParameters#expectedCount}: accepted values are integer.
         */
        MAX_COUNT(true),

        /**
         * See {@link NotificationParameters#onlyUnread}: accepted values are boolean.
         */
        ONLY_UNREAD(true),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are list of serialized document references joined with commas.
         * It will be used to create filter to include those pages.
         */
        PAGES(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are list of serialized space references joined with commas.
         * It will be used to create filter to include those spaces.
         */
        SPACES(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are list of serialized wiki references joined with commas.
         * It will be used to create filter to include those wikis.
         */
        WIKIS(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are list of usernames joined with commas.
         * It will be used to create filter to include the events from those users.
         */
        USERS(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are boolean, if {@code true} the own events of {@link #USER_ID} will be included.
         */
        DISPLAY_OWN_EVENTS(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are boolean, if {@code true} minor events will be included.
         */
        DISPLAY_MINOR_EVENTS(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are boolean, if {@code true} system events will be included.
         */
        DISPLAY_SYSTEM_EVENTS(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are boolean, if {@code true} read events will be included.
         */
        DISPLAY_READ_EVENTS(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are list of string corresponding to document tags.
         * This will be used to include events concerning documents with those tags.
         */
        TAGS(false),

        /**
         * This parameter will be only taken into account if {@link #USE_USER_PREFERENCES} parameter value is false.
         * Accepted values are identifier of the current wiki.
         * This is used in case of multiple wikis.
         */
        // TODO: not sure we should keep it, I put it since it was already in the REST API.
        CURRENT_WIKI(false);

        private boolean isDirectlyUsed;

        /**
         * Default constructor.
         * @param isDirectlyUsed {@code true} if we have a method which handle directly the parameter with its value.
         *                       {@code false} if the parameters is evaluated in conjunction with others.
         */
        ParametersKey(boolean isDirectlyUsed)
        {
            this.isDirectlyUsed = isDirectlyUsed;
        }

        /**
         * @return {@code true} if we have a method which handle directly the parameter with its value.
         *         {@code false} if the parameters is evaluated in conjunction with others.
         */
        protected boolean isDirectlyUsed()
        {
            return this.isDirectlyUsed;
        }

        /**
         * Retrieve the {@link ParametersKey} based on a name, but accept camel cases version of the name.
         * (e.g. calling this method with userId should return {@link #USER_ID}).
         *
         * @param name a string name of a {@link ParametersKey} in any case.
         * @return an instance corresponding to the name.
         */
        static ParametersKey valueOfIgnoreCase(String name)
        {
            for (ParametersKey key : ParametersKey.values()) {
                if (CaseUtils.toCamelCase(key.name(), false, '_')
                    .equalsIgnoreCase(CaseUtils.toCamelCase(name, false, '_'))) {
                    return key;
                }
            }
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * Create a notification parameters with a map of parameters indexed by strings.
     * The keys of the map should be any variant of {@link ParametersKey} names: camel case is accepted.
     * The {@link NotificationParameters} is created then using {@link #createNotificationParameters(Map)} i
     * mplementation. This method is mainly provided as a helper in order to be allowed to use this factory with
     * velocity scripts easily.
     *
     * @param parameters the map of parameters with String as indexes.
     * @return a {@link NotificationParameters} that comply with the given parameters.
     * @throws NotificationException in case of error during the creation of the filters.
     */
    public NotificationParameters createNotificationParametersWithStringMap(Map<String, String> parameters)
        throws NotificationException
    {
        Map<ParametersKey, String> keyStringMap = new HashMap<>();

        for (Map.Entry<String, String> parameterPair : parameters.entrySet()) {
            String parameterKey = parameterPair.getKey();
            String parameterValue = parameterPair.getValue();

            ParametersKey actualKey = ParametersKey.valueOfIgnoreCase(parameterKey);
            if (actualKey != null) {
                keyStringMap.put(actualKey, parameterValue);
            } else {
                logger.error("Cannot find the right parameter key for name [{}].", parameterKey);
            }
        }
        return createNotificationParameters(keyStringMap);
    }

    /**
     * Create a {@link NotificationParameters} object based on the values of the {@link ParametersKey} given in the map.
     * The values are provided as String. The various accepted parameters are documented in {@link ParametersKey}.
     *
     * @param parameters the map of parameters value to use for building the {@link NotificationParameters} object.
     * @return a {@link NotificationParameters} that can be used to query notifications.
     * @throws NotificationException in case of error when retrieving the filters.
     */
    public NotificationParameters createNotificationParameters(Map<ParametersKey, String> parameters)
        throws NotificationException
    {
        NotificationParameters notificationParameters = new NotificationParameters();

        for (Map.Entry<ParametersKey, String> parameterPair : parameters.entrySet()) {
            ParametersKey actualKey = parameterPair.getKey();
            String parameterValue = parameterPair.getValue();

            if (actualKey.isDirectlyUsed()) {
                switch (actualKey) {
                    case FORMAT:
                        this.handleFormat(notificationParameters, parameterValue);
                        break;

                    case USER_ID:
                        this.handleUserId(notificationParameters, parameterValue);
                        break;

                    case UNTIL_DATE:
                        this.handleUntilDate(notificationParameters, parameterValue);
                        break;

                    case BLACKLIST:
                        this.handleBlacklist(notificationParameters, parameterValue);
                        break;

                    case MAX_COUNT:
                        this.handleMaxCount(notificationParameters, parameterValue);
                        break;

                    case ONLY_UNREAD:
                        this.handleOnlyUnread(notificationParameters, parameterValue);
                        break;

                    default:
                        logger.error("The notification parameters key [{}] exist but has not been implemented.",
                            actualKey.name());
                }
            }
        }

        if (notificationParameters.format == null) {
            notificationParameters.format = NotificationFormat.ALERT;
        }

        try {
            if (Boolean.parseBoolean(parameters.get(ParametersKey.USE_USER_PREFERENCES))) {
                this.useUserPreferences(notificationParameters);
            } else {
                this.dontUseUserPreferences(notificationParameters, parameters);
            }
        } catch (EventStreamException e) {
            throw new NotificationException("Error while adding the filters to the notification parameters.", e);
        }

        return notificationParameters;
    }

    private void useUserPreferences(NotificationParameters parameters)
        throws EventStreamException, NotificationException
    {
        if (parameters.user != null) {
            // Check if we should pre or post filter events
            if (parameters.format == NotificationFormat.ALERT && this.configuration.isEventPreFilteringEnabled()) {
                enableAllEventTypes(parameters);

                parameters.filters.add(new ForUserEventFilter(NotificationFormat.ALERT, null));
            } else {
                parameters.preferences =
                    notificationPreferenceManager.getPreferences(parameters.user, true, parameters.format);
                parameters.filters = notificationFilterManager.getAllFilters(parameters.user, true);
                parameters.filterPreferences =
                    notificationFilterPreferenceManager.getFilterPreferences(parameters.user);
            }
        }
    }

    private void dontUseUserPreferences(NotificationParameters notificationParameters,
        Map<ParametersKey, String> parameters)
        throws NotificationException, EventStreamException
    {
        List<String> excludedFilters = new ArrayList<>();
        if (Boolean.parseBoolean(parameters.get(ParametersKey.DISPLAY_OWN_EVENTS))) {
            excludedFilters.add(OwnEventFilter.FILTER_NAME);
        }
        if (Boolean.parseBoolean(parameters.get(ParametersKey.DISPLAY_MINOR_EVENTS))
            && notificationParameters.format == NotificationFormat.ALERT) {
            excludedFilters.add(MinorEventAlertNotificationFilter.FILTER_NAME);
        }
        if (Boolean.parseBoolean(parameters.get(ParametersKey.DISPLAY_SYSTEM_EVENTS))) {
            excludedFilters.add(SystemUserNotificationFilter.FILTER_NAME);
        }
        if (Boolean.parseBoolean(parameters.get(ParametersKey.DISPLAY_READ_EVENTS))
            && notificationParameters.format == NotificationFormat.ALERT) {
            excludedFilters.add(EventReadAlertFilter.FILTER_NAME);
        }
        notificationParameters.filters = notificationFilterManager.getAllFilters(true).stream()
            .filter(filter -> !excludedFilters.contains(filter.getName())).collect(Collectors.toList());

        enableAllEventTypes(notificationParameters);
        handleLocationParameter(parameters.get(ParametersKey.PAGES),
            notificationParameters, NotificationFilterProperty.PAGE);
        handleLocationParameter(parameters.get(ParametersKey.SPACES),
            notificationParameters, NotificationFilterProperty.SPACE);

        String wikis = parameters.get(ParametersKey.WIKIS);
        String currentWiki = parameters.get(ParametersKey.CURRENT_WIKI);
        handleLocationParameter(wikis, notificationParameters, NotificationFilterProperty.WIKI);
        // When the notifications are displayed in a macro in a subwiki, we assume they should not contain events from
        // other wikis (except if the "wikis" parameter is set).
        // The concept of the subwiki is to restrict a given domain of interest into a given wiki, this is why it does
        // not make sense to show events from other wikis in a "timeline" such as the notifications macro.
        // TODO: add a "handleAllWikis" parameter to disable this behaviour
        // Note that on the main wiki, which is often a "portal" for all the others wikis, we assure it's OK to display
        // events from other wikis.
        if (StringUtils.isBlank(wikis) && !StringUtils.equals(currentWiki, wikiDescriptorManager.getMainWikiId())) {
            handleLocationParameter(currentWiki, notificationParameters, NotificationFilterProperty.WIKI);
        }

        usersParameterHandler.handleUsersParameter(parameters.get(ParametersKey.USERS), notificationParameters);

        handleTagsParameter(notificationParameters, parameters.get(ParametersKey.TAGS), currentWiki);
    }

    private void handleTagsParameter(NotificationParameters parameters, String tags, String currentWiki)
    {
        if (StringUtils.isNotBlank(tags)) {
            String[] tagArray = tags.split(",");
            for (int i = 0; i < tagArray.length; ++i) {
                parameters.filterPreferences.add(new TagNotificationFilterPreference(tagArray[i], currentWiki));
            }
        }
    }

    private void handleLocationParameter(String locations, NotificationParameters parameters,
        NotificationFilterProperty property)
    {
        if (StringUtils.isNotBlank(locations)) {
            Set<NotificationFormat> formats = new HashSet<>();
            formats.add(parameters.format);

            String[] locationArray = locations.split(FIELD_SEPARATOR);
            for (int i = 0; i < locationArray.length; ++i) {
                DefaultNotificationFilterPreference pref = new DefaultNotificationFilterPreference();
                pref.setId(String.format("%s_%s_%s", ScopeNotificationFilter.FILTER_NAME, property, i));
                pref.setEnabled(true);
                pref.setFilterName(ScopeNotificationFilter.FILTER_NAME);
                pref.setFilterType(NotificationFilterType.INCLUSIVE);
                pref.setNotificationFormats(formats);
                pref.setProviderHint("FACTORY");
                switch (property) {
                    case WIKI:
                        pref.setWiki(locationArray[i]);
                        break;
                    case SPACE:
                        pref.setPage(locationArray[i]);
                        break;
                    case PAGE:
                        pref.setPageOnly(locationArray[i]);
                        break;
                    default:
                        break;
                }
                parameters.filterPreferences.add(new ScopeNotificationFilterPreference(pref, entityReferenceResolver));
            }
        }
    }

    private void enableAllEventTypes(NotificationParameters parameters) throws EventStreamException
    {
        parameters.preferences.clear();
        for (RecordableEventDescriptor descriptor : recordableEventDescriptorManager
            .getRecordableEventDescriptors(true)) {
            parameters.preferences.add(new InternalNotificationPreference(descriptor));
        }
    }

    private void handleOnlyUnread(NotificationParameters notificationParameters, String onlyUnread)
    {
        notificationParameters.onlyUnread = Boolean.parseBoolean(onlyUnread);
    }

    private void handleMaxCount(NotificationParameters notificationParameters, String maxCount)
    {
        if (StringUtils.isNotBlank(maxCount)) {
            try {
                notificationParameters.expectedCount = Integer.parseInt(maxCount);
            } catch (NumberFormatException e) {
                logger.error("Error while parsing maxCount number [{}] the parameter won't be taken into account.",
                    maxCount, e);
            }
        }
    }

    private void handleFormat(NotificationParameters notificationParameters, String format)
    {
        if (StringUtils.isNotBlank(format)) {
            notificationParameters.format = NotificationFormat.valueOf(format);
        }
    }

    private void handleUserId(NotificationParameters notificationParameters, String userId)
    {
        if (StringUtils.isNotBlank(userId)) {
            notificationParameters.user = this.stringDocumentReferenceResolver.resolve(userId);
        }
    }

    private void handleBlacklist(NotificationParameters notificationParameters, String blackList)
    {
        if (StringUtils.isNotBlank(blackList)) {
            notificationParameters.blackList.addAll(Arrays.asList(blackList.split(FIELD_SEPARATOR)));
        }
    }

    private void handleUntilDate(NotificationParameters notificationParameters, String untilDate)
    {
        if (StringUtils.isNotBlank(untilDate)) {
            notificationParameters.endDate = new Date(Long.parseLong(untilDate));
        }
    }
}
