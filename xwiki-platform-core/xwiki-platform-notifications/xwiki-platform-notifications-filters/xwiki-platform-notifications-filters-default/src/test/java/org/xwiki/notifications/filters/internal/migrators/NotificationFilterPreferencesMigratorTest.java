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
import java.util.Date;
import java.util.List;
import java.util.Set;

import jakarta.inject.Provider;

import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.text.StringUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.NotificationFormat.ALERT;
import static org.xwiki.notifications.NotificationFormat.EMAIL;
import static org.xwiki.notifications.filters.NotificationFilterType.EXCLUSIVE;
import static org.xwiki.notifications.filters.NotificationFilterType.INCLUSIVE;

/**
 * Test of {@link NotificationFilterPreferencesMigrator}.
 *
 * @version $Id$
 */
@ComponentTest
class NotificationFilterPreferencesMigratorTest
{
    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @InjectMockComponents
    private NotificationFilterPreferencesMigrator migrator;

    @MockComponent
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Mock
    private XWikiContext xwikicontext;

    @Mock
    private XWiki xwiki;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.xwikicontext);
        when(this.xwikicontext.getWiki()).thenReturn(this.xwiki);
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of("xwiki"));
    }

    @Test
    void migrate() throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        when(query.setWiki("xwiki")).thenReturn(query);
        when(query.execute()).thenReturn(List.of("XWiki.UserA"));

        WikiReference wikiReference = new WikiReference("xwiki");
        when(this.xwikicontext.getWikiReference()).thenReturn(wikiReference);

        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");
        when(this.referenceResolver.resolve("XWiki.UserA", wikiReference)).thenReturn(userA);

        XWikiDocument userAdoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(userA, this.xwikicontext)).thenReturn(userAdoc);
        // Arrays.asList is intentional here: the list contains a null element (simulating a missing XObject)
        List<BaseObject> objects = Arrays.asList(
            mockObject(List.of("alert"), "filter1", true, "inclusive",
                new Date(100L), List.of("update", "create"), List.of("page1", "page2"),
                List.of("space1", "space2"), List.of("wiki1", "wiki2"), List.of("user1", "user2")),
            null,
            mockObject(List.of("alert", "email"), "filter2", false, "exclusive",
                new Date(90L), List.of("comment"), List.of("page3"), List.of(), List.of(), List.of())
        );
        DocumentReference classReference = new DocumentReference("xwiki", List.of("XWiki", "Notifications", "Code"),
            "NotificationFilterPreferenceClass");
        when(userAdoc.getXObjects(classReference)).thenReturn(objects);

        DocumentReference notificationFilterPreferenceClass = new DocumentReference("xwiki",
            List.of("XWiki", "Notifications", "Code"), "NotificationFilterPreferenceClass");
        XWikiDocument oldClass = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(notificationFilterPreferenceClass, this.xwikicontext)).thenReturn(oldClass);
        when(oldClass.isNew()).thenReturn(false);
        when(this.xwiki.exists(notificationFilterPreferenceClass, this.xwikicontext)).thenReturn(true);

        // Test
        this.migrator.onEvent(new ApplicationReadyEvent(), null, null);

        // Verify log messages from the migration
        assertEquals(
            "Getting the list of the users having notification filter preferences to migrate on wiki [xwiki].",
            this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Migrating the notification filter preferences of user [xwiki:XWiki.UserA].",
            this.logCapture.getMessage(1));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(1).getLevel());
        assertEquals("Loading the current notification filter preferences of user [xwiki:XWiki.UserA].",
            this.logCapture.getMessage(2));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(2).getLevel());
        assertEquals(
            "Saving the migrated notification filter preferences of user [xwiki:XWiki.UserA] in the new store.",
            this.logCapture.getMessage(3));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(3).getLevel());
        assertEquals(
            "Removing the old notification filter preferences in the page of the user [xwiki:XWiki.UserA]"
                + " (please wait, it could be long).",
            this.logCapture.getMessage(4));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(4).getLevel());
        assertEquals("Removing the old notification filter preference class on wiki [xwiki].",
            this.logCapture.getMessage(5));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(5).getLevel());

        // Verify
        verify(this.xwiki).deleteDocument(oldClass, false, this.xwikicontext);
        verify(userAdoc).removeXObjects(classReference);
        verify(this.xwiki).saveDocument(userAdoc, "Migrate notification filter preferences to the new store.",
            this.xwikicontext);

        verify(this.filterPreferencesModelBridge).saveFilterPreferences(userA, List.of(
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "", "", "page1", ""),
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "", "", "page2", ""),
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "", "space1", "", ""),
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "", "space2", "", ""),
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "wiki1", "", "", ""),
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "wiki2", "", "", ""),
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "", "", "", "user1"),
            createExpectedPreference(SetUtils.hashSet("update", "create"), SetUtils.hashSet(ALERT), INCLUSIVE,
                "filter1", true, new Date(100L), "", "", "", "user2"),
            createExpectedPreference(SetUtils.hashSet("comment"), SetUtils.hashSet(ALERT, EMAIL), EXCLUSIVE,
                "filter2", false, new Date(90L), "", "", "page3", "")
        ));
    }

    private BaseObject mockObject(List<String> formats, String filterName, boolean isEnabled,
        String filterType, Date date, List<String> eventTypes, List<String> pages, List<String> spaces,
        List<String> wikis, List<String> users)
    {
        BaseObject obj = mock(BaseObject.class);

        when(obj.getListValue("filterFormats")).thenReturn(formats);
        when(obj.getStringValue("filterName")).thenReturn(filterName);
        when(obj.getIntValue("isEnabled", 1)).thenReturn(isEnabled ? 1 : 0);
        when(obj.getStringValue("filterType")).thenReturn(filterType);
        when(obj.getDateValue("startingDate")).thenReturn(date);

        when(obj.getListValue("eventTypes")).thenReturn(eventTypes);
        when(obj.getListValue("pages")).thenReturn(pages);
        when(obj.getListValue("spaces")).thenReturn(spaces);
        when(obj.getListValue("wikis")).thenReturn(wikis);
        when(obj.getListValue("users")).thenReturn(users);

        return obj;
    }

    private DefaultNotificationFilterPreference createExpectedPreference(Set<String> eventType,
        Set<NotificationFormat> formats, NotificationFilterType filterType, String filterName, boolean isEnabled,
        Date date, String wiki, String page, String pageOnly, String user)
    {
        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference();
        preference.setEventTypes(eventType);
        preference.setNotificationFormats(formats);
        preference.setFilterType(filterType);
        preference.setFilterName(filterName);
        preference.setEnabled(isEnabled);
        preference.setStartingDate(date);
        if (StringUtils.isNotBlank(wiki)) {
            preference.setWiki(wiki);
        }
        if (StringUtils.isNotBlank(page)) {
            preference.setPage(page);
        }
        if (StringUtils.isNotBlank(pageOnly)) {
            preference.setPageOnly(pageOnly);
        }
        if (StringUtils.isNotBlank(user)) {
            preference.setUser(user);
        }
        return preference;
    }
}
