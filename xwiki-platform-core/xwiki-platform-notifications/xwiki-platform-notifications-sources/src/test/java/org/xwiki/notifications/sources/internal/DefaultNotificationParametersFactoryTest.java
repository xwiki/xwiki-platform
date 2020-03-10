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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.SystemUserNotificationFilter;
import org.xwiki.notifications.filters.internal.minor.MinorEventAlertNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.status.EventReadAlertFilter;
import org.xwiki.notifications.filters.internal.status.ForUserEventFilter;
import org.xwiki.notifications.filters.internal.user.OwnEventFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import org.xwiki.notifications.sources.internal.DefaultNotificationParametersFactory.ParametersKey;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultNotificationParametersFactory}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultNotificationParametersFactoryTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "Foo");
    private static final String USER_SERIALIZED_REFERENCE = "xwiki:XWiki.Foo";

    @InjectMockComponents
    private DefaultNotificationParametersFactory parametersFactory;

    @MockComponent
    private NotificationFilterManager notificationFilterManager;

    @MockComponent
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @MockComponent
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @MockComponent
    private NotificationConfiguration configuration;

    @MockComponent
    private EntityReferenceResolver<String> entityReferenceResolver;

    @MockComponent
    private UsersParameterHandler usersParameterHandler;

    private List<NotificationFilter> filterList;

    private List<RecordableEventDescriptor> recordableEventDescriptors;

    private List<NotificationPreference> preferenceList;

    private List<NotificationFilterPreference> filterPreferenceList;

    @BeforeEach
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        when(this.stringDocumentReferenceResolver.resolve(USER_SERIALIZED_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.configuration.isEventPreFilteringEnabled()).thenReturn(true);

        OwnEventFilter ownEventFilter = componentManager.registerMockComponent(OwnEventFilter.class);
        when(ownEventFilter.getName()).thenReturn(OwnEventFilter.FILTER_NAME);

        MinorEventAlertNotificationFilter minorEventFilter =
            componentManager.registerMockComponent(MinorEventAlertNotificationFilter.class);
        when(minorEventFilter.getName()).thenReturn(MinorEventAlertNotificationFilter.FILTER_NAME);

        SystemUserNotificationFilter systemEventFilter =
            componentManager.registerMockComponent(SystemUserNotificationFilter.class);
        when(systemEventFilter.getName()).thenReturn(SystemUserNotificationFilter.FILTER_NAME);

        EventReadAlertFilter readEventFilter = componentManager.registerMockComponent(EventReadAlertFilter.class);
        when(readEventFilter.getName()).thenReturn(EventReadAlertFilter.FILTER_NAME);

        this.filterList = Arrays.asList(
            ownEventFilter,
            minorEventFilter,
            systemEventFilter,
            readEventFilter
        );

        this.preferenceList = Arrays.asList(
            mock(NotificationPreference.class),
            mock(NotificationPreference.class),
            mock(NotificationPreference.class)
        );

        this.filterPreferenceList = Arrays.asList(
            mock(NotificationFilterPreference.class)
        );

        this.recordableEventDescriptors = Arrays.asList(
            mock(RecordableEventDescriptor.class),
            mock(RecordableEventDescriptor.class)
        );
        when(notificationFilterManager.getAllFilters(true)).thenReturn(this.filterList);
        when(notificationFilterManager.getAllFilters(USER_REFERENCE, true))
            .thenReturn(Collections.singletonList(filterList.get(1)));

        when(notificationPreferenceManager.getPreferences(eq(USER_REFERENCE), eq(true), any()))
            .thenReturn(this.preferenceList);

        when(notificationFilterPreferenceManager.getFilterPreferences(USER_REFERENCE))
            .thenReturn(this.filterPreferenceList);

        when(recordableEventDescriptorManager.getRecordableEventDescriptors(true))
            .thenReturn(recordableEventDescriptors);
    }

    @Test
    public void valueOfIgnoreCase()
    {
        assertEquals(ParametersKey.CURRENT_WIKI, ParametersKey.valueOfIgnoreCase("currentwiki"));
        assertEquals(ParametersKey.CURRENT_WIKI, ParametersKey.valueOfIgnoreCase("current_wiki"));
        assertEquals(ParametersKey.CURRENT_WIKI, ParametersKey.valueOfIgnoreCase("CurrentWiki"));
        assertEquals(ParametersKey.CURRENT_WIKI, ParametersKey.valueOfIgnoreCase("CURRENT_WIKI"));

        assertNull(ParametersKey.valueOfIgnoreCase("foobar"));
    }

    @Test
    public void createNotificationParameters() throws NotificationException
    {
        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.filters = this.filterList;
        notificationParameters.preferences = Arrays.asList(
            new InternalNotificationPreference(this.recordableEventDescriptors.get(0)),
            new InternalNotificationPreference(this.recordableEventDescriptors.get(1))
        );
        assertEquals(notificationParameters,
            this.parametersFactory.createNotificationParameters(Collections.emptyMap()));

        Map<ParametersKey, String> parametersMap = new HashMap<>();
        parametersMap.put(ParametersKey.FORMAT, "EMAIL");
        parametersMap.put(ParametersKey.USER_ID, USER_SERIALIZED_REFERENCE);
        parametersMap.put(ParametersKey.USE_USER_PREFERENCES, "true");
        parametersMap.put(ParametersKey.UNTIL_DATE, "4242");
        parametersMap.put(ParametersKey.MAX_COUNT, "1258");
        parametersMap.put(ParametersKey.ONLY_UNREAD, "true");
        parametersMap.put(ParametersKey.BLACKLIST, "foo,bar,baz");
        parametersMap.put(ParametersKey.PAGES, "a,b,c");
        parametersMap.put(ParametersKey.TAGS, "d,e,f");
        parametersMap.put(ParametersKey.CURRENT_WIKI, "mywiki");
        parametersMap.put(ParametersKey.DISPLAY_READ_EVENTS, "true");
        parametersMap.put(ParametersKey.DISPLAY_SYSTEM_EVENTS, "false");
        parametersMap.put(ParametersKey.DISPLAY_OWN_EVENTS, "true");
        parametersMap.put(ParametersKey.DISPLAY_MINOR_EVENTS, "false");
        parametersMap.put(ParametersKey.USERS, "foobar,barbar");
        parametersMap.put(ParametersKey.WIKIS, "wiki1,wiki2");
        parametersMap.put(ParametersKey.SPACES, "space1,space2");

        notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.EMAIL;
        notificationParameters.endDate = new Date(4242);
        notificationParameters.onlyUnread = true;
        notificationParameters.user = USER_REFERENCE;
        notificationParameters.expectedCount = 1258;
        notificationParameters.blackList = Arrays.asList("foo", "bar", "baz");
        notificationParameters.preferences = preferenceList;
        notificationParameters.filterPreferences = filterPreferenceList;
        notificationParameters.filters = Collections.singletonList(filterList.get(1));

        assertEquals(notificationParameters, this.parametersFactory.createNotificationParameters(parametersMap));

        parametersMap.put(ParametersKey.FORMAT, "ALERT");
        notificationParameters.format = NotificationFormat.ALERT;
        assertNotEquals(notificationParameters, this.parametersFactory.createNotificationParameters(parametersMap));

        notificationParameters.preferences = Arrays.asList(
            new InternalNotificationPreference(this.recordableEventDescriptors.get(0)),
            new InternalNotificationPreference(this.recordableEventDescriptors.get(1))
        );
        notificationParameters.filters =
            Collections.singletonList(new ForUserEventFilter(NotificationFormat.ALERT, null));
        notificationParameters.filterPreferences = Collections.emptyList();
        NotificationParameters obtainedParameters =
            this.parametersFactory.createNotificationParameters(parametersMap);
        assertEquals(notificationParameters, obtainedParameters);

        parametersMap.put(ParametersKey.USE_USER_PREFERENCES, "false");

        // Don't forget that "false" in DISPLAY_XXX_EVENTS means that the filter is applied to discard those events.
        notificationParameters.filters = Arrays.asList(
            filterList.get(1),
            filterList.get(2)
        );

        List<NotificationFilterPreference> notificationFilterPreferences = new ArrayList<>();
        DefaultNotificationFilterPreference filterPref = getFilterPreference("PAGE", 0);
        filterPref.setPageOnly("a");
        notificationFilterPreferences.add(new ScopeNotificationFilterPreference(filterPref,
            this.entityReferenceResolver));

        filterPref = getFilterPreference("PAGE", 1);
        filterPref.setPageOnly("b");
        notificationFilterPreferences.add(new ScopeNotificationFilterPreference(filterPref,
            this.entityReferenceResolver));

        filterPref = getFilterPreference("PAGE", 2);
        filterPref.setPageOnly("c");
        notificationFilterPreferences.add(new ScopeNotificationFilterPreference(filterPref,
            this.entityReferenceResolver));

        filterPref = getFilterPreference("SPACE", 0);
        filterPref.setPage("space1");
        notificationFilterPreferences.add(new ScopeNotificationFilterPreference(filterPref,
            this.entityReferenceResolver));

        filterPref = getFilterPreference("SPACE", 1);
        filterPref.setPage("space2");
        notificationFilterPreferences.add(new ScopeNotificationFilterPreference(filterPref,
            this.entityReferenceResolver));

        filterPref = getFilterPreference("WIKI", 0);
        filterPref.setWiki("wiki1");
        notificationFilterPreferences.add(new ScopeNotificationFilterPreference(filterPref,
            this.entityReferenceResolver));

        filterPref = getFilterPreference("WIKI", 1);
        filterPref.setWiki("wiki2");
        notificationFilterPreferences.add(new ScopeNotificationFilterPreference(filterPref,
            this.entityReferenceResolver));

        notificationFilterPreferences.add(new TagNotificationFilterPreference("d", "mywiki"));
        notificationFilterPreferences.add(new TagNotificationFilterPreference("e", "mywiki"));
        notificationFilterPreferences.add(new TagNotificationFilterPreference("f", "mywiki"));

        notificationParameters.filterPreferences = notificationFilterPreferences;
        obtainedParameters = this.parametersFactory.createNotificationParameters(parametersMap);
        assertEquals(notificationParameters, obtainedParameters);
        verify(this.usersParameterHandler).handleUsersParameter("foobar,barbar", notificationParameters);
    }

    private DefaultNotificationFilterPreference getFilterPreference(String property, int number)
    {
        DefaultNotificationFilterPreference filterPreference = new DefaultNotificationFilterPreference();
        filterPreference.setId(String.format("%s_%s_%s", ScopeNotificationFilter.FILTER_NAME, property, number));
        filterPreference.setEnabled(true);
        filterPreference.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        filterPreference.setFilterType(NotificationFilterType.INCLUSIVE);
        filterPreference.setNotificationFormats(Collections.singleton(NotificationFormat.ALERT));
        filterPreference.setProviderHint("FACTORY");
        return filterPreference;
    }
}
