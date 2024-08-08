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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
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
import org.xwiki.notifications.sources.internal.DefaultNotificationParametersFactory.ParametersKey;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    private EntityReferenceResolver<String> entityReferenceResolver;

    @MockComponent
    @Named("relative")
    private EntityReferenceResolver<String> relativeEntityReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private UsersParameterHandler usersParameterHandler;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    private List<RecordableEventDescriptor> recordableEventDescriptors;

    private List<NotificationPreference> mailPreferenceList;

    private List<NotificationPreference> alertPreferenceList;

    private List<NotificationFilterPreference> filterPreferenceList;

    private OwnEventFilter ownEventFilter;
    private MinorEventAlertNotificationFilter minorEventFilter;
    private SystemUserNotificationFilter systemEventFilter;
    private EventReadAlertFilter readEventFilter;
    private ScopeNotificationFilter scopeFilter;

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        when(this.stringDocumentReferenceResolver.resolve(USER_SERIALIZED_REFERENCE)).thenReturn(USER_REFERENCE);

        this.ownEventFilter = componentManager.registerMockComponent(OwnEventFilter.class);
        when(ownEventFilter.getName()).thenReturn(OwnEventFilter.FILTER_NAME);

        this.minorEventFilter =
            componentManager.registerMockComponent(MinorEventAlertNotificationFilter.class);
        when(minorEventFilter.getName()).thenReturn(MinorEventAlertNotificationFilter.FILTER_NAME);

        this.systemEventFilter =
            componentManager.registerMockComponent(SystemUserNotificationFilter.class);
        when(systemEventFilter.getName()).thenReturn(SystemUserNotificationFilter.FILTER_NAME);

        this.readEventFilter = componentManager.registerMockComponent(EventReadAlertFilter.class);
        when(readEventFilter.getName()).thenReturn(EventReadAlertFilter.FILTER_NAME);

        this.scopeFilter = componentManager.registerMockComponent(ScopeNotificationFilter.class);
        when(scopeFilter.getName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);

        this.mailPreferenceList = List.of(mock(NotificationPreference.class), mock(NotificationPreference.class),
            mock(NotificationPreference.class));
        this.alertPreferenceList = List.of(mock(NotificationPreference.class), mock(NotificationPreference.class),
            mock(NotificationPreference.class));

        this.filterPreferenceList = List.of(mock(NotificationFilterPreference.class));

        this.recordableEventDescriptors =
            List.of(mock(RecordableEventDescriptor.class), mock(RecordableEventDescriptor.class));
        when(notificationFilterManager.getAllFilters(true)).thenReturn(List.of(
            ownEventFilter,
            minorEventFilter,
            systemEventFilter,
            readEventFilter,
            scopeFilter
        ));
        when(notificationFilterManager.getAllFilters(USER_REFERENCE, true,
            NotificationFilter.FilteringPhase.POST_FILTERING))
            .thenReturn(List.of(this.minorEventFilter));

        when(notificationPreferenceManager.getPreferences(eq(USER_REFERENCE), eq(true), same(NotificationFormat.EMAIL)))
            .thenReturn(this.mailPreferenceList);
        when(notificationPreferenceManager.getPreferences(eq(USER_REFERENCE), eq(true), same(NotificationFormat.ALERT)))
            .thenReturn(this.alertPreferenceList);

        when(notificationFilterPreferenceManager.getFilterPreferences(USER_REFERENCE))
            .thenReturn(this.filterPreferenceList);

        when(recordableEventDescriptorManager.getRecordableEventDescriptors(true))
            .thenReturn(recordableEventDescriptors);

        when(relativeEntityReferenceResolver.resolve(isNotNull(), isNotNull())).thenAnswer(new Answer<EntityReference>()
        {
            public EntityReference answer(InvocationOnMock invocation) throws Throwable
            {
                String pageName = invocation.getArgument(0, String.class);
                EntityType type = invocation.getArgument(1, EntityType.class);
                return new EntityReference(pageName, type);
            }
        });

        when(entityReferenceResolver.resolve(isNotNull(), isNotNull(), isNotNull()))
            .thenAnswer(new Answer<EntityReference>()
            {
                public EntityReference answer(InvocationOnMock invocation) throws Throwable
                {
                    String pageName = invocation.getArgument(0, String.class);
                    EntityType type = invocation.getArgument(1, EntityType.class);
                    EntityReference parent = invocation.getArgument(2, EntityReference.class);
                    return new EntityReference(pageName, type, parent);
                }
            });

        when(entityReferenceSerializer.serialize(isNotNull())).thenAnswer(new Answer<String>()
        {
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                EntityReference param = invocation.getArgument(0, EntityReference.class);
                EntityReference parent = param.getParent();
                if (parent != null) {
                    assertEquals(EntityType.WIKI, parent.getType());
                    return parent.getName() + "@@" + param.getName();
                }
                return param.getName();
            }
        });
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
    public void createNotificationParameters() throws Exception
    {
        NotificationParameters notificationParameters = new NotificationParameters();
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.filters = Set.of(
            ownEventFilter,
            minorEventFilter,
            systemEventFilter,
            readEventFilter
        );
        notificationParameters.preferences =
            List.of(new InternalNotificationPreference(this.recordableEventDescriptors.get(0)),
                new InternalNotificationPreference(this.recordableEventDescriptors.get(1)));
        assertEquals(notificationParameters,
            this.parametersFactory.createNotificationParameters(Collections.emptyMap()));
        verify(wikiDescriptorManager).getCurrentWikiId();

        Map<ParametersKey, String> parametersMap = new HashMap<>();
        parametersMap.put(ParametersKey.FORMAT, "EMAIL");
        parametersMap.put(ParametersKey.USER_ID, USER_SERIALIZED_REFERENCE);
        parametersMap.put(ParametersKey.USE_USER_PREFERENCES, "true");
        parametersMap.put(ParametersKey.UNTIL_DATE, "4242");
        parametersMap.put(ParametersKey.UNTIL_DATE_INCLUDED, "true");
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
        notificationParameters.blackList = List.of("foo", "bar", "baz");
        notificationParameters.preferences = this.mailPreferenceList;
        notificationParameters.filterPreferences = this.filterPreferenceList;
        when(notificationFilterManager.getAllFilters(USER_REFERENCE, true,
            NotificationFilter.FilteringPhase.POST_FILTERING)).thenReturn(Collections.emptyList());

        notificationParameters.filters =
            Collections.singleton(new ForUserEventFilter(NotificationFormat.EMAIL, null));
        notificationParameters.preferences = List.of(
            new InternalNotificationPreference(this.recordableEventDescriptors.get(0)),
            new InternalNotificationPreference(this.recordableEventDescriptors.get(1))
        );
        notificationParameters.filterPreferences = Collections.emptyList();

        assertEquals(notificationParameters, this.parametersFactory.createNotificationParameters(parametersMap));

        parametersMap.put(ParametersKey.FORMAT, NotificationFormat.ALERT.name());
        notificationParameters.format = NotificationFormat.ALERT;
        notificationParameters.filters =
            Collections.singleton(new ForUserEventFilter(NotificationFormat.ALERT, null));
        assertEquals(notificationParameters, this.parametersFactory.createNotificationParameters(parametersMap));

        parametersMap.put(ParametersKey.USE_USER_PREFERENCES, "false");

        // Don't forget that "false" in DISPLAY_XXX_EVENTS means that the filter is applied to discard those events.
        notificationParameters.filters = Set.of(
            this.minorEventFilter,
            this.systemEventFilter,
            this.scopeFilter
        );

        List<NotificationFilterPreference> notificationFilterPreferences = new ArrayList<>();
        DefaultNotificationFilterPreference filterPref = getFilterPreference("PAGE", 0);
        filterPref.setPageOnly("mywiki@@a");
        notificationFilterPreferences
            .add(new ScopeNotificationFilterPreference(filterPref, this.entityReferenceResolver));

        filterPref = getFilterPreference("PAGE", 1);
        filterPref.setPageOnly("mywiki@@b");
        notificationFilterPreferences
            .add(new ScopeNotificationFilterPreference(filterPref, this.entityReferenceResolver));

        filterPref = getFilterPreference("PAGE", 2);
        filterPref.setPageOnly("mywiki@@c");
        notificationFilterPreferences
            .add(new ScopeNotificationFilterPreference(filterPref, this.entityReferenceResolver));

        filterPref = getFilterPreference("SPACE", 0);
        filterPref.setPage("mywiki@@space1");
        notificationFilterPreferences
            .add(new ScopeNotificationFilterPreference(filterPref, this.entityReferenceResolver));

        filterPref = getFilterPreference("SPACE", 1);
        filterPref.setPage("mywiki@@space2");
        notificationFilterPreferences
            .add(new ScopeNotificationFilterPreference(filterPref, this.entityReferenceResolver));

        filterPref = getFilterPreference("WIKI", 0);
        filterPref.setWiki("wiki1");
        notificationFilterPreferences
            .add(new ScopeNotificationFilterPreference(filterPref, this.entityReferenceResolver));

        filterPref = getFilterPreference("WIKI", 1);
        filterPref.setWiki("wiki2");
        notificationFilterPreferences
            .add(new ScopeNotificationFilterPreference(filterPref, this.entityReferenceResolver));

        notificationFilterPreferences.add(new TagNotificationFilterPreference("d", "mywiki"));
        notificationFilterPreferences.add(new TagNotificationFilterPreference("e", "mywiki"));
        notificationFilterPreferences.add(new TagNotificationFilterPreference("f", "mywiki"));

        notificationParameters.filterPreferences = notificationFilterPreferences;
        assertEquals(notificationParameters, this.parametersFactory.createNotificationParameters(parametersMap));
        verify(this.usersParameterHandler).handleUsersParameter("foobar,barbar", notificationParameters);

        parametersMap.remove(ParametersKey.CURRENT_WIKI);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");
        assertEquals(notificationParameters, this.parametersFactory.createNotificationParameters(parametersMap));
        verify(this.wikiDescriptorManager, times(3)).getCurrentWikiId();
    }

    @Test
    void createNotificationParametersDontUseUserPreferences() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mywiki");
        NotificationParameters expectedNotificationParameters = new NotificationParameters();
        expectedNotificationParameters.format = NotificationFormat.ALERT;
        expectedNotificationParameters.preferences = List.of(
            new InternalNotificationPreference(this.recordableEventDescriptors.get(0)),
            new InternalNotificationPreference(this.recordableEventDescriptors.get(1))
        );
        expectedNotificationParameters.filters = Set.of(
            ownEventFilter,
            minorEventFilter,
            systemEventFilter,
            readEventFilter
        );

        assertEquals(expectedNotificationParameters, this.parametersFactory.createNotificationParameters(Map.of(
            ParametersKey.USE_USER_PREFERENCES, "false"
        )));
    }

    @Test
    void createNotificationParametersDontUseUserPreferencesWithSpace() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");
        NotificationParameters expectedNotificationParameters = new NotificationParameters();
        expectedNotificationParameters.format = NotificationFormat.ALERT;
        expectedNotificationParameters.preferences = List.of(
            new InternalNotificationPreference(this.recordableEventDescriptors.get(0)),
            new InternalNotificationPreference(this.recordableEventDescriptors.get(1))
        );
        expectedNotificationParameters.filters = Set.of(
            ownEventFilter,
            minorEventFilter,
            systemEventFilter,
            readEventFilter,
            scopeFilter
        );

        DefaultNotificationFilterPreference filterPreference0 = getFilterPreference("SPACE", 0);
        filterPreference0.setPage("mywiki@@s1");
        expectedNotificationParameters.filterPreferences = List.of(
            new ScopeNotificationFilterPreference(filterPreference0, this.entityReferenceResolver)
        );

        assertEquals(expectedNotificationParameters, this.parametersFactory.createNotificationParameters(Map.of(
            ParametersKey.USE_USER_PREFERENCES, "false",
            ParametersKey.SPACES, "s1"
        )));
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
