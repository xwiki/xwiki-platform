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

package org.xwiki.notifications.filters.internal.livedata.system;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilterActivation;
import org.xwiki.notifications.filters.internal.livedata.NotificationFilterLiveDataTranslationHelper;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NotificationSystemFiltersLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 16.2.0RC1
 */
@ComponentTest
@ReferenceComponentList
class NotificationSystemFiltersLiveDataEntryStoreTest
{
    @InjectMockComponents
    private NotificationSystemFiltersLiveDataEntryStore entryStore;

    @MockComponent
    private NotificationFilterManager notificationFilterManager;

    @MockComponent
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private NotificationFilterLiveDataTranslationHelper translationHelper;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void getMissingTarget()
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        when(source.getParameters()).thenReturn(Map.of());
        LiveDataException liveDataException = assertThrows(LiveDataException.class, () -> this.entryStore.get(query));
        assertEquals("The target source parameter is mandatory.", liveDataException.getMessage());
    }

    @Test
    void getBadAuthorization() throws LiveDataException
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        when(source.getParameters()).thenReturn(Map.of(
            "target", "wiki",
            "wiki", "foo"
        ));
        DocumentReference userDoc = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(context.getUserReference()).thenReturn(userDoc);
        LiveDataException liveDataException = assertThrows(LiveDataException.class, () -> this.entryStore.get(query));
        assertEquals("You don't have rights to access those information.", liveDataException.getMessage());

        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(true);
        LiveData emptyLiveData = new LiveData();
        assertEquals(emptyLiveData, this.entryStore.get(query));

        when(source.getParameters()).thenReturn(Map.of(
            "target", "user",
            "user", "xwiki:XWiki.Bar"
        ));
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        liveDataException = assertThrows(LiveDataException.class, () -> this.entryStore.get(query));
        assertEquals("You don't have rights to access those information.", liveDataException.getMessage());

        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(true);
        assertEquals(emptyLiveData, this.entryStore.get(query));

        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(source.getParameters()).thenReturn(Map.of(
            "target", "user",
            "user", "xwiki:XWiki.Foo"
        ));
        assertEquals(emptyLiveData, this.entryStore.get(query));
    }

    @Test
    void getFromWiki() throws NotificationException, LiveDataException
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        when(source.getParameters()).thenReturn(Map.of(
            "target", "wiki",
            "wiki", "foo"
        ));
        DocumentReference userDoc = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(true);
        when(context.getUserReference()).thenReturn(userDoc);

        String filter1Name = "filter1";
        String filter2Name = "filter2";
        String filter3Name = "filter3";
        String filter4Name = "filter4";
        String filter5Name = "filter5";
        String filter6Name = "filter6";
        String filter7Name = "filter7";

        NotificationFilter filter1 = mock(NotificationFilter.class, filter1Name);
        ToggleableNotificationFilter filter2 = mock(ToggleableNotificationFilter.class, filter2Name);
        NotificationFilter filter3 = mock(NotificationFilter.class, filter3Name);
        ToggleableNotificationFilter filter4 = mock(ToggleableNotificationFilter.class, filter4Name);
        ToggleableNotificationFilter filter5 = mock(ToggleableNotificationFilter.class, filter5Name);
        ToggleableNotificationFilter filter6 = mock(ToggleableNotificationFilter.class, filter6Name);
        ToggleableNotificationFilter filter7 = mock(ToggleableNotificationFilter.class, filter7Name);

        when(filter1.getName()).thenReturn(filter1Name);
        when(filter2.getName()).thenReturn(filter2Name);
        when(filter3.getName()).thenReturn(filter3Name);
        when(filter4.getName()).thenReturn(filter4Name);
        when(filter5.getName()).thenReturn(filter5Name);
        when(filter6.getName()).thenReturn(filter6Name);
        when(filter7.getName()).thenReturn(filter7Name);

        WikiReference wikiReference = new WikiReference("foo");
        DocumentReference prefReference = new DocumentReference("foo", List.of("XWiki", "Notifications", "Code"),
            "NotificationAdministration");
        when(this.notificationFilterManager.getAllFilters(wikiReference)).thenReturn(List.of(
            // They are shuffled to test ordering
            filter3,
            filter4,
            filter6,
            filter2,
            filter5,
            filter1,
            filter7
        ));

        when(filter2.getFormats()).thenReturn(List.of(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(filter4.getFormats()).thenReturn(List.of());
        when(filter5.getFormats()).thenReturn(List.of(NotificationFormat.ALERT));
        when(filter6.getFormats()).thenReturn(List.of(NotificationFormat.EMAIL));
        when(filter7.getFormats()).thenReturn(List.of(NotificationFormat.EMAIL, NotificationFormat.ALERT));

        // There's explicitely no activation data for filter2 and filter6
        ToggleableNotificationFilterActivation activationFilter4 = mock(ToggleableNotificationFilterActivation.class,
            filter4Name);
        ToggleableNotificationFilterActivation activationFilter5 = mock(ToggleableNotificationFilterActivation.class,
            filter5Name);
        ToggleableNotificationFilterActivation activationFilter7 = mock(ToggleableNotificationFilterActivation.class,
            filter7Name);
        when(this.filterPreferencesModelBridge.getToggleableFilterActivations(prefReference)).thenReturn(Map.of(
            filter4Name, activationFilter4,
            filter5Name, activationFilter5,
            filter7Name, activationFilter7
        ));

        when(filter2.isEnabledByDefault()).thenReturn(true);
        when(filter6.isEnabledByDefault()).thenReturn(false);

        when(filter4.isEnabledByDefault()).thenReturn(true);
        when(filter5.isEnabledByDefault()).thenReturn(false);
        when(filter7.isEnabledByDefault()).thenReturn(false);

        when(activationFilter4.isEnabled()).thenReturn(false);
        when(activationFilter5.isEnabled()).thenReturn(true);
        when(activationFilter7.isEnabled()).thenReturn(false);

        when(activationFilter4.getObjectNumber()).thenReturn(-1);
        when(activationFilter5.getObjectNumber()).thenReturn(2);
        when(activationFilter7.getObjectNumber()).thenReturn(14);

        // Get all info to start
        when(query.getOffset()).thenReturn(0L);
        when(query.getLimit()).thenReturn(10);

        when(this.translationHelper.getTranslationWithPrefix(eq("notifications.filters.name."), anyString()))
            .thenAnswer(invocationOnMock -> "Name:" + invocationOnMock.getArgument(1));
        when(this.translationHelper.getTranslationWithPrefix(eq("notifications.filters.description."), anyString()))
            .thenAnswer(invocationOnMock -> "Description " + invocationOnMock.getArgument(1));
        when(this.translationHelper.getFormatTranslation(any()))
            .thenAnswer(invocationOnMock -> "Format " + invocationOnMock.getArgument(0));

        Map<String, Object> dataFilter2 = Map.of(
            "name", "Name:" + filter2Name,
            "filterDescription", "Description " + filter2Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format ALERT", "Format EMAIL")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "",
                "filterName", filter2Name
            ),
            "isEnabled_checked", true
        );

        Map<String, Object> dataFilter4 = Map.of(
            "name", "Name:" + filter4Name,
            "filterDescription", "Description " + filter4Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of()
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "",
                "filterName", filter4Name
            ),
            "isEnabled_checked", false
        );

        Map<String, Object> dataFilter5 = Map.of(
            "name", "Name:" + filter5Name,
            "filterDescription", "Description " + filter5Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format ALERT")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "2",
                "filterName", filter5Name
            ),
            "isEnabled_checked", true
        );

        Map<String, Object> dataFilter6 = Map.of(
            "name", "Name:" + filter6Name,
            "filterDescription", "Description " + filter6Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format EMAIL")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "",
                "filterName", filter6Name
            ),
            "isEnabled_checked", false
        );

        Map<String, Object> dataFilter7 = Map.of(
            "name", "Name:" + filter7Name,
            "filterDescription", "Description " + filter7Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format ALERT", "Format EMAIL")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "14",
                "filterName", filter7Name
            ),
            "isEnabled_checked", false
        );

        LiveData liveData = new LiveData();
        liveData.setCount(5L);
        liveData.getEntries().addAll(List.of(dataFilter2, dataFilter4, dataFilter5, dataFilter6, dataFilter7));
        assertEquals(liveData, this.entryStore.get(query));

        when(query.getOffset()).thenReturn(2L);
        when(query.getLimit()).thenReturn(2);

        liveData = new LiveData();
        liveData.setCount(5L);
        liveData.getEntries().addAll(List.of(dataFilter5, dataFilter6));
        assertEquals(liveData, this.entryStore.get(query));
    }

    @Test
    void getFromUser() throws NotificationException, LiveDataException
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        when(source.getParameters()).thenReturn(Map.of(
            "target", "user",
            "user", "xwiki:XWiki.Foo"
        ));
        DocumentReference userDoc = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(context.getUserReference()).thenReturn(userDoc);

        String filter1Name = "filter1";
        String filter2Name = "filter2";
        String filter3Name = "filter3";
        String filter4Name = "filter4";
        String filter5Name = "filter5";
        String filter6Name = "filter6";
        String filter7Name = "filter7";

        NotificationFilter filter1 = mock(NotificationFilter.class, filter1Name);
        ToggleableNotificationFilter filter2 = mock(ToggleableNotificationFilter.class, filter2Name);
        NotificationFilter filter3 = mock(NotificationFilter.class, filter3Name);
        ToggleableNotificationFilter filter4 = mock(ToggleableNotificationFilter.class, filter4Name);
        ToggleableNotificationFilter filter5 = mock(ToggleableNotificationFilter.class, filter5Name);
        ToggleableNotificationFilter filter6 = mock(ToggleableNotificationFilter.class, filter6Name);
        ToggleableNotificationFilter filter7 = mock(ToggleableNotificationFilter.class, filter7Name);

        when(filter1.getName()).thenReturn(filter1Name);
        when(filter2.getName()).thenReturn(filter2Name);
        when(filter3.getName()).thenReturn(filter3Name);
        when(filter4.getName()).thenReturn(filter4Name);
        when(filter5.getName()).thenReturn(filter5Name);
        when(filter6.getName()).thenReturn(filter6Name);
        when(filter7.getName()).thenReturn(filter7Name);

        when(this.notificationFilterManager.getAllFilters(userDoc, false)).thenReturn(List.of(
            // They are shuffled to test ordering
            filter3,
            filter4,
            filter6,
            filter2,
            filter5,
            filter1,
            filter7
        ));

        when(filter2.getFormats()).thenReturn(List.of(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(filter4.getFormats()).thenReturn(List.of());
        when(filter5.getFormats()).thenReturn(List.of(NotificationFormat.ALERT));
        when(filter6.getFormats()).thenReturn(List.of(NotificationFormat.EMAIL));
        when(filter7.getFormats()).thenReturn(List.of(NotificationFormat.EMAIL, NotificationFormat.ALERT));

        // There's explicitely no activation data for filter2 and filter6
        ToggleableNotificationFilterActivation activationFilter4 = mock(ToggleableNotificationFilterActivation.class,
            filter4Name);
        ToggleableNotificationFilterActivation activationFilter5 = mock(ToggleableNotificationFilterActivation.class,
            filter5Name);
        ToggleableNotificationFilterActivation activationFilter7 = mock(ToggleableNotificationFilterActivation.class,
            filter7Name);
        when(this.filterPreferencesModelBridge.getToggleableFilterActivations(userDoc)).thenReturn(Map.of(
            filter4Name, activationFilter4,
            filter5Name, activationFilter5,
            filter7Name, activationFilter7
        ));

        when(filter2.isEnabledByDefault()).thenReturn(true);
        when(filter6.isEnabledByDefault()).thenReturn(false);

        when(filter4.isEnabledByDefault()).thenReturn(true);
        when(filter5.isEnabledByDefault()).thenReturn(false);
        when(filter7.isEnabledByDefault()).thenReturn(false);

        when(activationFilter4.isEnabled()).thenReturn(false);
        when(activationFilter5.isEnabled()).thenReturn(true);
        when(activationFilter7.isEnabled()).thenReturn(false);

        when(activationFilter4.getObjectNumber()).thenReturn(-1);
        when(activationFilter5.getObjectNumber()).thenReturn(2);
        when(activationFilter7.getObjectNumber()).thenReturn(14);

        // Get all info to start
        when(query.getOffset()).thenReturn(0L);
        when(query.getLimit()).thenReturn(10);

        when(this.translationHelper.getTranslationWithPrefix(eq("notifications.filters.name."), anyString()))
            .thenAnswer(invocationOnMock -> "Name:" + invocationOnMock.getArgument(1));
        when(this.translationHelper.getTranslationWithPrefix(eq("notifications.filters.description."), anyString()))
            .thenAnswer(invocationOnMock -> "Description " + invocationOnMock.getArgument(1));
        when(this.translationHelper.getFormatTranslation(any()))
            .thenAnswer(invocationOnMock -> "Format " + invocationOnMock.getArgument(0));

        Map<String, Object> dataFilter2 = Map.of(
            "name", "Name:" + filter2Name,
            "filterDescription", "Description " + filter2Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format ALERT", "Format EMAIL")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "",
                "filterName", filter2Name
            ),
            "isEnabled_checked", true
        );

        Map<String, Object> dataFilter4 = Map.of(
            "name", "Name:" + filter4Name,
            "filterDescription", "Description " + filter4Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of()
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "",
                "filterName", filter4Name
            ),
            "isEnabled_checked", false
        );

        Map<String, Object> dataFilter5 = Map.of(
            "name", "Name:" + filter5Name,
            "filterDescription", "Description " + filter5Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format ALERT")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "2",
                "filterName", filter5Name
            ),
            "isEnabled_checked", true
        );

        Map<String, Object> dataFilter6 = Map.of(
            "name", "Name:" + filter6Name,
            "filterDescription", "Description " + filter6Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format EMAIL")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "",
                "filterName", filter6Name
            ),
            "isEnabled_checked", false
        );

        Map<String, Object> dataFilter7 = Map.of(
            "name", "Name:" + filter7Name,
            "filterDescription", "Description " + filter7Name,
            "notificationFormats", Map.of(
                "extraClass", "list-unstyled",
                "items", List.of("Format ALERT", "Format EMAIL")
            ),
            "isEnabled_data", Map.of(
                "objectNumber", "14",
                "filterName", filter7Name
            ),
            "isEnabled_checked", false
        );

        LiveData liveData = new LiveData();
        liveData.setCount(5L);
        liveData.getEntries().addAll(List.of(dataFilter2, dataFilter4, dataFilter5, dataFilter6, dataFilter7));
        assertEquals(liveData, this.entryStore.get(query));

        when(query.getOffset()).thenReturn(2L);
        when(query.getLimit()).thenReturn(2);

        liveData = new LiveData();
        liveData.setCount(5L);
        liveData.getEntries().addAll(List.of(dataFilter5, dataFilter6));
        assertEquals(liveData, this.entryStore.get(query));
    }
}