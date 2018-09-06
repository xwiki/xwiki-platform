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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.ModelBridge;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.text.StringUtils;

import com.google.common.collect.Sets;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since
 */
public class NotificationFilterPreferencesMigratorTest
{
    @Rule
    public final MockitoComponentMockingRule<NotificationFilterPreferencesMigrator> mocker =
            new MockitoComponentMockingRule<>(NotificationFilterPreferencesMigrator.class, HibernateDataMigration.class,
                    "R108000NotificationFilterPreferenceMigration");

    private ModelBridge modelBridge;
    private QueryManager queryManager;
    private DocumentReferenceResolver<String> referenceResolver;
    private Execution execution;
    private XWikiContext xwikicontext;
    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        modelBridge = mocker.getInstance(ModelBridge.class);
        queryManager = mocker.getInstance(QueryManager.class);
        referenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        execution = mocker.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        xwikicontext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikicontext);
        xwiki = mock(XWiki.class);
        when(xwikicontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void migrate() throws Exception
    {
        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("XWiki.UserA"));

        WikiReference wikiReference = new WikiReference("xwiki");
        when(xwikicontext.getWikiReference()).thenReturn(wikiReference);

        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");
        when(referenceResolver.resolve("XWiki.UserA", wikiReference)).thenReturn(userA);

        XWikiDocument userAdoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(userA, xwikicontext)).thenReturn(userAdoc);
        List<BaseObject> objects = Arrays.asList(
                mockObject(Arrays.asList("alert"), "filter1", true, "inclusive",
                        new Date(100L), Arrays.asList("update", "create"), Arrays.asList("page1", "page2"),
                        Arrays.asList("space1", "space2"), Arrays.asList("wiki1", "wiki2"),
                        Arrays.asList("user1", "user2")),
                null,
                mockObject(Arrays.asList("alert", "email"), "filter2", false, "exclusive",
                        new Date(90L), Arrays.asList("comment"), Arrays.asList("page3"),
                        Arrays.asList(), Arrays.asList(), Arrays.asList())
        );
        DocumentReference classReference = new DocumentReference("xwiki", Arrays.asList("XWiki", "Notifications", "Code"),
                "NotificationFilterPreferenceClass");
        when(userAdoc.getXObjects(classReference)).thenReturn(objects);

        DocumentReference notificationFilterPreferenceClass = new DocumentReference("xwiki",
                Arrays.asList("XWiki", "Notifications", "Code"), "NotificationFilterPreferenceClass");
        XWikiDocument oldClass = mock(XWikiDocument.class);
        when(xwiki.getDocument(notificationFilterPreferenceClass, xwikicontext)).thenReturn(oldClass);
        when(oldClass.isNew()).thenReturn(false);

        // Test
        mocker.getComponentUnderTest().migrate();

        // Verify
        verify(xwiki).deleteDocument(oldClass, false, xwikicontext);
        verify(userAdoc).removeXObjects(classReference);
        verify(xwiki).saveDocument(userAdoc, "Migrate notification filter preferences to the new store.",
                xwikicontext);

        verify(modelBridge).saveFilterPreferences(userA, Arrays.asList(
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "", "", "page1", ""),
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "", "", "page2", ""),
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "", "space1", "", ""),
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "", "space2", "", ""),
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "wiki1", "", "", ""),
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "wiki2", "", "", ""),
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "", "", "", "user1"),
                createExpectedPreference(Sets.newHashSet("update", "create"),
                        Sets.newHashSet(NotificationFormat.ALERT), NotificationFilterType.INCLUSIVE,
                        "filter1", true, new Date(100L), "", "", "", "user2"),
                createExpectedPreference(Sets.newHashSet("comment"),
                        Sets.newHashSet(NotificationFormat.ALERT, NotificationFormat.EMAIL), NotificationFilterType.EXCLUSIVE,
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

    private DefaultNotificationFilterPreference createExpectedPreference(
            Set<String> eventType,
            Set<NotificationFormat> formats,
            NotificationFilterType filterType, String filterName, boolean isEnabled, Date date,
            String wiki, String page, String pageOnly, String user)
    {
        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference();
        preference.setEventTypes(eventType);
        preference.setNotificationFormats(formats);
        preference.setFilterType(filterType);
        preference.setProviderHint("userProfile");
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
