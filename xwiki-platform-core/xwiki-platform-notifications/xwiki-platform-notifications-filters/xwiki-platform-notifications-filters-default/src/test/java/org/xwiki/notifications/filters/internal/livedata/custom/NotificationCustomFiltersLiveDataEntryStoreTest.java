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

package org.xwiki.notifications.filters.internal.livedata.custom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.notifications.filters.internal.livedata.NotificationFilterLiveDataTranslationHelper;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NotificationCustomFiltersLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@ComponentTest
@ReferenceComponentList
class NotificationCustomFiltersLiveDataEntryStoreTest
{
    @InjectMockComponents
    private NotificationCustomFiltersLiveDataEntryStore entryStore;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private NotificationFilterLiveDataTranslationHelper translationHelper;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    @MockComponent
    private NotificationFilterManager notificationFilterManager;

    @MockComponent
    @Named("html/5.0")
    private BlockRenderer blockRenderer;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private NotificationCustomFiltersQueryHelper queryHelper;

    private XWikiContext context;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        when(this.translationHelper.getFormatTranslation(any()))
            .thenAnswer(invocationOnMock -> "Format " + invocationOnMock.getArgument(0));
        when(this.translationHelper.getScopeTranslation(any()))
            .thenAnswer(invocationOnMock -> "Scope " + invocationOnMock.getArgument(0));
        when(this.translationHelper.getFilterTypeTranslation(any()))
            .thenAnswer(invocationOnMock -> "FilterType " + invocationOnMock.getArgument(0));
        when(this.translationHelper.getAllEventTypesTranslation()).thenReturn("All event types");
        when(this.translationHelper.getEventTypeTranslation(any()))
            .thenAnswer(invocationOnMock -> "EventType " + invocationOnMock.getArgument(0));
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
    void getEntry(MockitoComponentManager componentManager) throws Exception
    {
        String entryId = "entryId";
        WikiReference wikiReference = new WikiReference("foo");
        when(this.context.getWikiReference()).thenReturn(wikiReference);
        assertEquals(Optional.empty(), this.entryStore.get(entryId));
        DefaultNotificationFilterPreference notificationFilterPreference =
            mock(DefaultNotificationFilterPreference.class);
        when(this.notificationFilterPreferenceStore.getFilterPreference(entryId, wikiReference))
            .thenReturn(Optional.of(notificationFilterPreference));
        DocumentReference userDoc = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(context.getUserReference()).thenReturn(userDoc);

        LiveDataException liveDataException = assertThrows(LiveDataException.class, () -> this.entryStore.get(entryId));
        assertEquals("You don't have rights to access those information.", liveDataException.getMessage());

        String filterId = "watchlist_id432";
        when(notificationFilterPreference.getId()).thenReturn(filterId);
        when(notificationFilterPreference.getEventTypes()).thenReturn(Set.of());
        when(notificationFilterPreference.getNotificationFormats())
            .thenReturn(Set.of(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        String page = "foo:Space.Page";
        SpaceReference spaceReference = new SpaceReference("foo", List.of("Space", "Page"));
        when(notificationFilterPreference.getPage()).thenReturn(page);
        when(notificationFilterPreference.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(notificationFilterPreference.isEnabled()).thenReturn(true);

        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);

        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(true);
        String filterName = "scopeFilter";
        when(notificationFilterPreference.getFilterName()).thenReturn(filterName);

        liveDataException = assertThrows(LiveDataException.class, () -> this.entryStore.get(entryId));
        assertEquals("Cannot find NotificationFilter component for preference named [scopeFilter]",
            liveDataException.getMessage());

        NotificationFilter notificationFilter =
            componentManager.registerMockComponent(NotificationFilter.class, filterName);

        String displayLocation = "Location hierarchy";
        when(this.templateManager.renderNoException("notification/filters/livedatalocation.vm"))
            .thenReturn(displayLocation);
        Block block = mock(Block.class);
        when(this.notificationFilterManager.displayFilter(notificationFilter, notificationFilterPreference))
            .thenReturn(block);
        String displayData = "Scope and location here";
        doAnswer(invocationOnMock -> {
            DefaultWikiPrinter wikiPrinter = invocationOnMock.getArgument(1);
            wikiPrinter.print(displayData);
            return null;
        }).when(this.blockRenderer).render(eq(block), any());

        Map<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("filterPreferenceId", filterId);
        expectedResult.put("eventTypes", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("All event types")
        ));
        expectedResult.put("notificationFormats", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("Format ALERT", "Format EMAIL")
        ));
        expectedResult.put("scope", Map.of(
            "icon", "chart-organisation",
            "name", "Scope SPACE"
        ));

        expectedResult.put("location", displayLocation);
        expectedResult.put("display", displayData);
        expectedResult.put("filterType", "FilterType EXCLUSIVE");
        expectedResult.put("isEnabled_checked", true);
        expectedResult.put("isEnabled_disabled", true);
        expectedResult.put("isEnabled_data", Map.of(
            "preferenceId", filterId
        ));
        expectedResult.put("doc_hasdelete", true);

        assertEquals(Optional.of(expectedResult), this.entryStore.get(entryId));

        verify(scriptContext, times(2)).setAttribute("location", new EntityReference(spaceReference),
            ScriptContext.ENGINE_SCOPE);

        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(notificationFilterPreference.getOwner()).thenReturn("xwiki:XWiki.Foo");
        assertEquals(Optional.of(expectedResult), this.entryStore.get(entryId));
    }

    @Test
    void getEntries(MockitoComponentManager componentManager) throws Exception
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        String owner = "foo";
        when(source.getParameters()).thenReturn(Map.of(
            "target", "wiki",
            "wiki", owner
        ));
        DocumentReference userDoc = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(true);
        when(context.getUserReference()).thenReturn(userDoc);
        WikiReference wikiReference = new WikiReference(owner);
        Long count = 12L;
        when(this.queryHelper.countTotalFilters(query, owner, wikiReference)).thenReturn(count);

        NotificationFilterPreference filterPref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref3 = mock(NotificationFilterPreference.class);
        when(this.queryHelper.getFilterPreferences(query, owner, wikiReference)).thenReturn(List.of(
            filterPref1,
            filterPref2,
            filterPref3
        ));

        String filterId1 = "id432";
        when(filterPref1.getId()).thenReturn(filterId1);
        when(filterPref1.getEventTypes()).thenReturn(Set.of("event foo"));
        when(filterPref1.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.EMAIL));
        String user = "xwiki:XWiki.User1";
        //DocumentReference user1DocRef = new DocumentReference("xwiki", "XWiki", "User1");
        when(filterPref1.getUser()).thenReturn(user);
        when(filterPref1.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);
        when(filterPref1.isEnabled()).thenReturn(false);

        String filterId2 = "bka_id432";
        when(filterPref2.getId()).thenReturn(filterId2);
        when(filterPref2.getEventTypes()).thenReturn(Set.of());
        when(filterPref2.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.ALERT));
        String wikiFilter2 = "bla";
        //WikiReference wikiReferenceFilter = new WikiReference(wikiFilter2);
        when(filterPref2.getWiki()).thenReturn(wikiFilter2);
        when(filterPref2.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);
        when(filterPref2.isEnabled()).thenReturn(true);

        String filterId3 = "444564";
        when(filterPref3.getId()).thenReturn(filterId3);
        when(filterPref3.getEventTypes()).thenReturn(Set.of("event foo", "bar", "buz"));
        when(filterPref3.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.EMAIL,
            NotificationFormat.ALERT));
        String pageOnly = "xwiki:XWiki.User1";
        DocumentReference pageOnlyRef = new DocumentReference("xwiki", "XWiki", "User1");
        when(filterPref3.getPageOnly()).thenReturn(pageOnly);
        when(filterPref3.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(filterPref3.isEnabled()).thenReturn(true);

        ScriptContext scriptContext1 = mock(ScriptContext.class, "scriptContext1");
        ScriptContext scriptContext2 = mock(ScriptContext.class, "scriptContext1");
        ScriptContext scriptContext3 = mock(ScriptContext.class, "scriptContext1");
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(
            scriptContext1,
            scriptContext2,
            scriptContext3
        );

        String displayLocation1 = "Location hierarchy 1";
        String displayLocation2 = "Location hierarchy 2";
        String displayLocation3 = "Location hierarchy 3";
        when(this.templateManager.renderNoException("notification/filters/livedatalocation.vm")).thenReturn(
            displayLocation1,
            displayLocation2,
            displayLocation3
        );

        String filterName1 = "filter1";
        String filterName2 = "filter2";
        NotificationFilter notificationFilter1 =
            componentManager.registerMockComponent(NotificationFilter.class, filterName1);
        NotificationFilter notificationFilter2 =
            componentManager.registerMockComponent(NotificationFilter.class, filterName2);

        when(filterPref1.getFilterName()).thenReturn(filterName1);
        when(filterPref2.getFilterName()).thenReturn(filterName2);
        when(filterPref3.getFilterName()).thenReturn(filterName2);

        Block block1 = mock(Block.class, "block1");
        Block block2 = mock(Block.class, "block2");
        Block block3 = mock(Block.class, "block3");

        when(this.notificationFilterManager.displayFilter(notificationFilter1, filterPref1))
            .thenReturn(block1);
        when(this.notificationFilterManager.displayFilter(notificationFilter2, filterPref2))
            .thenReturn(block2);
        when(this.notificationFilterManager.displayFilter(notificationFilter2, filterPref3))
            .thenReturn(block3);

        String displayData1 = "Scope and location here 1";
        String displayData2 = "Scope and location here 2";
        String displayData3 = "Scope and location here 3";
        doAnswer(invocationOnMock -> {
            DefaultWikiPrinter wikiPrinter = invocationOnMock.getArgument(1);
            wikiPrinter.print(displayData1);
            return null;
        }).when(this.blockRenderer).render(eq(block1), any());

        doAnswer(invocationOnMock -> {
            DefaultWikiPrinter wikiPrinter = invocationOnMock.getArgument(1);
            wikiPrinter.print(displayData2);
            return null;
        }).when(this.blockRenderer).render(eq(block2), any());

        doAnswer(invocationOnMock -> {
            DefaultWikiPrinter wikiPrinter = invocationOnMock.getArgument(1);
            wikiPrinter.print(displayData3);
            return null;
        }).when(this.blockRenderer).render(eq(block3), any());

        Map<String, Object> expectedResult1 = new LinkedHashMap<>();
        expectedResult1.put("filterPreferenceId", filterId1);
        expectedResult1.put("eventTypes", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("EventType event foo")
        ));
        expectedResult1.put("notificationFormats", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("Format EMAIL")
        ));
        expectedResult1.put("scope", Map.of(
            "icon", "user",
            "name", "Scope USER"
        ));
        expectedResult1.put("location", displayLocation1);
        expectedResult1.put("display", displayData1);
        expectedResult1.put("filterType", "FilterType INCLUSIVE");
        expectedResult1.put("isEnabled_checked", false);
        expectedResult1.put("isEnabled_disabled", false);
        expectedResult1.put("isEnabled_data", Map.of(
            "preferenceId", filterId1
        ));
        expectedResult1.put("doc_hasdelete", true);

        Map<String, Object> expectedResult2 = new LinkedHashMap<>();
        expectedResult2.put("filterPreferenceId", filterId2);
        expectedResult2.put("eventTypes", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("All event types")
        ));
        expectedResult2.put("notificationFormats", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("Format ALERT")
        ));
        expectedResult2.put("scope", Map.of(
            "icon", "wiki",
            "name", "Scope WIKI"
        ));
        expectedResult2.put("location", displayLocation2);
        expectedResult2.put("display", displayData2);
        expectedResult2.put("filterType", "FilterType INCLUSIVE");
        expectedResult2.put("isEnabled_checked", true);
        expectedResult2.put("isEnabled_disabled", false);
        expectedResult2.put("isEnabled_data", Map.of(
            "preferenceId", filterId2
        ));
        expectedResult2.put("doc_hasdelete", true);

        Map<String, Object> expectedResult3 = new LinkedHashMap<>();
        expectedResult3.put("filterPreferenceId", filterId3);
        expectedResult3.put("eventTypes", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("EventType bar", "EventType buz", "EventType event foo")
        ));
        expectedResult3.put("notificationFormats", Map.of(
            "extraClass", "list-unstyled",
            "items", List.of("Format ALERT", "Format EMAIL")
        ));
        expectedResult3.put("scope", Map.of(
            "icon", "page",
            "name", "Scope PAGE"
        ));
        expectedResult3.put("location", displayLocation3);
        expectedResult3.put("display", displayData3);
        expectedResult3.put("filterType", "FilterType EXCLUSIVE");
        expectedResult3.put("isEnabled_checked", true);
        expectedResult3.put("isEnabled_disabled", false);
        expectedResult3.put("isEnabled_data", Map.of(
            "preferenceId", filterId3
        ));
        expectedResult3.put("doc_hasdelete", true);

        LiveData liveData = new LiveData();
        liveData.setCount(count);
        liveData.getEntries().addAll(List.of(expectedResult1, expectedResult2, expectedResult3));

        assertEquals(liveData, this.entryStore.get(query));
    }
}