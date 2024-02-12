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

import java.util.List;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link R160100000XWIKI21738DataMigration}.
 *
 * @version $Id$
 */
@ComponentTest
@ReferenceComponentList
class R160100000XWIKI21738DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R160100000XWIKI21738DataMigration dataMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private NotificationFilterPreferenceStore filterPreferenceStore;

    @MockComponent
    private Execution execution;

    private HibernateStore hibernateStore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private XWikiHibernateStore hibernateBaseStore;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        this.hibernateStore = componentManager.registerMockComponent(HibernateStore.class);
        this.hibernateBaseStore = componentManager.registerMockComponent(XWikiStoreInterface.class,
            XWikiHibernateBaseStore.HINT, XWikiHibernateStore.class, false);
    }

    @Test
    void getVersion()
    {
        assertEquals(160100000, dataMigration.getVersion().getVersion());
    }

    @Test
    void shouldExecute()
    {
        String currentDB = "foo";
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(currentDB);
        when(this.wikiDescriptorManager.isMainWiki(currentDB)).thenReturn(false);
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(12)));

        when(this.wikiDescriptorManager.isMainWiki(currentDB)).thenReturn(true);
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(12)));
    }

    @Test
    void hibernateMigrate()
        throws QueryException, WikiManagerException, DataMigrationException, XWikiException, NotificationException
    {
        String mainWikiId = "mainWiki";
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(mainWikiId);

        // We simulate 2 loops (3rd is just getting empty value)
        Query query1 = mock(Query.class, "query1");
        Query query2 = mock(Query.class, "query2");
        Query query3 = mock(Query.class, "query3");

        when(this.queryManager.createQuery("select nfp "
            + "from DefaultNotificationFilterPreference nfp "
            + "where nfp.owner not like :ownerLike and nfp.owner <> :mainWiki "
            + "order by nfp.owner, nfp.internalId", Query.HQL))
            .thenReturn(query1)
            .thenReturn(query2)
            .thenReturn(query3);

        QueryParameter queryParameter1 = mock(QueryParameter.class, "queryParam1");
        when(query1.bindValue("ownerLike")).thenReturn(queryParameter1);
        when(queryParameter1.literal(any())).thenReturn(queryParameter1);
        when(queryParameter1.anyChars()).thenReturn(queryParameter1);
        when(queryParameter1.query()).thenReturn(query1);

        QueryParameter queryParameter2 = mock(QueryParameter.class, "queryParam2");
        when(query2.bindValue("ownerLike")).thenReturn(queryParameter2);
        when(queryParameter2.literal(any())).thenReturn(queryParameter2);
        when(queryParameter2.anyChars()).thenReturn(queryParameter2);
        when(queryParameter2.query()).thenReturn(query2);

        QueryParameter queryParameter3 = mock(QueryParameter.class, "queryParam3");
        when(query3.bindValue("ownerLike")).thenReturn(queryParameter3);
        when(queryParameter3.literal(any())).thenReturn(queryParameter3);
        when(queryParameter3.anyChars()).thenReturn(queryParameter3);
        when(queryParameter3.query()).thenReturn(query3);

        when(query1.bindValue("mainWiki", mainWikiId)).thenReturn(query1);
        when(query2.bindValue("mainWiki", mainWikiId)).thenReturn(query2);
        when(query3.bindValue("mainWiki", mainWikiId)).thenReturn(query3);

        when(query1.setLimit(100)).thenReturn(query1);
        when(query2.setLimit(100)).thenReturn(query2);
        when(query3.setLimit(100)).thenReturn(query3);

        DefaultNotificationFilterPreference filterPref1 = mock(DefaultNotificationFilterPreference.class, "filter1");
        DefaultNotificationFilterPreference filterPref2 = mock(DefaultNotificationFilterPreference.class, "filter2");
        DefaultNotificationFilterPreference filterPref3 = mock(DefaultNotificationFilterPreference.class, "filter3");
        when(query1.execute()).thenReturn(List.of(filterPref1, filterPref2, filterPref3));

        DefaultNotificationFilterPreference filterPref4 = mock(DefaultNotificationFilterPreference.class, "filter4");
        DefaultNotificationFilterPreference filterPref5 = mock(DefaultNotificationFilterPreference.class, "filter5");
        DefaultNotificationFilterPreference filterPref6 = mock(DefaultNotificationFilterPreference.class, "filter6");
        when(query2.execute()).thenReturn(List.of(filterPref4, filterPref5, filterPref6));
        when(query3.execute()).thenReturn(List.of());

        XWikiContext context = mock(XWikiContext.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(context);

        String ownerFilter1 = "foo";
        String ownerFilter2 = "foo:Space.User1";
        String ownerFilter3 = "sub:Space.User1";
        String ownerFilter4 = "sub:Space.User2";
        String ownerFilter5 = "removed";
        String ownerFilter6 = "sub:Space.User2";

        when(filterPref1.getOwner()).thenReturn(ownerFilter1);
        when(filterPref2.getOwner()).thenReturn(ownerFilter2);
        when(filterPref3.getOwner()).thenReturn(ownerFilter3);
        when(filterPref4.getOwner()).thenReturn(ownerFilter4);
        when(filterPref5.getOwner()).thenReturn(ownerFilter5);
        when(filterPref6.getOwner()).thenReturn(ownerFilter6);

        when(this.wikiDescriptorManager.exists("foo")).thenReturn(true);
        when(this.wikiDescriptorManager.exists("sub")).thenReturn(true);
        when(this.wikiDescriptorManager.exists("removed")).thenReturn(false);

        Long filterId1 = 1L;
        Long filterId2 = 2L;
        Long filterId3 = 3L;
        Long filterId4 = 4L;
        Long filterId5 = 5L;
        Long filterId6 = 6L;

        when(filterPref1.getInternalId()).thenReturn(filterId1);
        when(filterPref2.getInternalId()).thenReturn(filterId2);
        when(filterPref3.getInternalId()).thenReturn(filterId3);
        when(filterPref4.getInternalId()).thenReturn(filterId4);
        when(filterPref5.getInternalId()).thenReturn(filterId5);
        when(filterPref6.getInternalId()).thenReturn(filterId6);

        Session session1 = mock(Session.class, "session1");
        Session session2 = mock(Session.class, "session2");

        when(this.hibernateBaseStore.executeWrite(eq(context), any())).then(invocationOnMock -> {
            XWikiHibernateBaseStore.HibernateCallback callback = invocationOnMock.getArgument(1);
            return callback.doInHibernate(session1);
        }).then(invocationOnMock -> {
            XWikiHibernateBaseStore.HibernateCallback callback = invocationOnMock.getArgument(1);
            return callback.doInHibernate(session2);
        });

        org.hibernate.query.Query sessionQuery1 = mock(org.hibernate.query.Query.class, "sessionQuery1");
        when(session1.createQuery("delete from DefaultNotificationFilterPreference nfp "
            + "where nfp.internalId IN (:listIds)")).thenReturn(sessionQuery1);
        when(sessionQuery1.setParameter("listIds", List.of(filterId1, filterId2, filterId3))).thenReturn(sessionQuery1);

        org.hibernate.query.Query sessionQuery2 = mock(org.hibernate.query.Query.class, "sessionQuery2");
        when(session2.createQuery("delete from DefaultNotificationFilterPreference nfp "
            + "where nfp.internalId IN (:listIds)")).thenReturn(sessionQuery2);
        when(sessionQuery2.setParameter("listIds", List.of(filterId4, filterId5, filterId6))).thenReturn(sessionQuery2);

        this.dataMigration.hibernateMigrate();
        assertEquals(6, logCapture.size());
        // Loop 1
        assertEquals("Found [3] filters to migrate...", logCapture.getMessage(0));
        assertEquals("Migrating filters for [3] entities", logCapture.getMessage(1));

        // Loop 2
        assertEquals("Found [3] filters to migrate...", logCapture.getMessage(2));
        assertEquals("Owner [Wiki removed] of some filter preferences belongs to a wiki that does not long exist, "
            + "preferences will be removed.", logCapture.getMessage(3));
        assertEquals("Migrating filters for [1] entities", logCapture.getMessage(4));

        // Loop 3
        assertEquals("No more filters found to migrate.", logCapture.getMessage(5));

        verify(this.filterPreferenceStore).saveFilterPreferences(new WikiReference("foo"), List.of(
            new DefaultNotificationFilterPreference(filterPref1, false)
        ));
        verify(this.filterPreferenceStore).saveFilterPreferences(new DocumentReference("foo", "Space", "User1"),
            List.of(new DefaultNotificationFilterPreference(filterPref2, false))
        );
        verify(this.filterPreferenceStore).saveFilterPreferences(new DocumentReference("sub", "Space", "User1"),
            List.of(new DefaultNotificationFilterPreference(filterPref3, false))
        );
        verify(this.filterPreferenceStore).saveFilterPreferences(new DocumentReference("sub", "Space", "User2"),
            List.of(
                new DefaultNotificationFilterPreference(filterPref4, false),
                new DefaultNotificationFilterPreference(filterPref6, false)
            )
        );
        verify(sessionQuery1).executeUpdate();
        verify(sessionQuery2).executeUpdate();
    }

    @Test
    void hibernateMigrateDeletionError()
        throws QueryException, NotificationException, XWikiException, WikiManagerException
    {
        String mainWikiId = "mainWiki";
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(mainWikiId);

        // We simulate 2 loops returning same values
        Query query1 = mock(Query.class, "query1");
        Query query2 = mock(Query.class, "query2");

        when(this.queryManager.createQuery("select nfp "
            + "from DefaultNotificationFilterPreference nfp "
            + "where nfp.owner not like :ownerLike and nfp.owner <> :mainWiki "
            + "order by nfp.owner, nfp.internalId", Query.HQL))
            .thenReturn(query1)
            .thenReturn(query2);

        QueryParameter queryParameter1 = mock(QueryParameter.class, "queryParam1");
        when(query1.bindValue("ownerLike")).thenReturn(queryParameter1);
        when(queryParameter1.literal(any())).thenReturn(queryParameter1);
        when(queryParameter1.anyChars()).thenReturn(queryParameter1);
        when(queryParameter1.query()).thenReturn(query1);

        QueryParameter queryParameter2 = mock(QueryParameter.class, "queryParam2");
        when(query2.bindValue("ownerLike")).thenReturn(queryParameter2);
        when(queryParameter2.literal(any())).thenReturn(queryParameter2);
        when(queryParameter2.anyChars()).thenReturn(queryParameter2);
        when(queryParameter2.query()).thenReturn(query2);

        when(query1.bindValue("mainWiki", mainWikiId)).thenReturn(query1);
        when(query2.bindValue("mainWiki", mainWikiId)).thenReturn(query2);

        when(query1.setLimit(100)).thenReturn(query1);
        when(query2.setLimit(100)).thenReturn(query2);

        DefaultNotificationFilterPreference filterPref1 = mock(DefaultNotificationFilterPreference.class, "filter1");
        DefaultNotificationFilterPreference filterPref2 = mock(DefaultNotificationFilterPreference.class, "filter2");
        DefaultNotificationFilterPreference filterPref3 = mock(DefaultNotificationFilterPreference.class, "filter3");
        when(query1.execute()).thenReturn(List.of(filterPref1, filterPref2, filterPref3));
        when(query2.execute()).thenReturn(List.of(filterPref1, filterPref2, filterPref3));

        XWikiContext context = mock(XWikiContext.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(context);

        String owner = "sub:XWiki.User1";
        when(filterPref1.getOwner()).thenReturn(owner);
        when(filterPref2.getOwner()).thenReturn(owner);
        when(filterPref3.getOwner()).thenReturn(owner);

        when(filterPref1.getInternalId()).thenReturn(1L);
        when(filterPref2.getInternalId()).thenReturn(2L);
        when(filterPref3.getInternalId()).thenReturn(3L);

        when(this.wikiDescriptorManager.exists("sub")).thenReturn(true);

        Session session1 = mock(Session.class, "session1");
        when(this.hibernateBaseStore.executeWrite(eq(context), any())).then(invocationOnMock -> {
            XWikiHibernateBaseStore.HibernateCallback callback = invocationOnMock.getArgument(1);
            return callback.doInHibernate(session1);
        });
        org.hibernate.query.Query sessionQuery1 = mock(org.hibernate.query.Query.class, "sessionQuery1");
        when(session1.createQuery("delete from DefaultNotificationFilterPreference nfp "
            + "where nfp.internalId IN (:listIds)")).thenReturn(sessionQuery1);
        when(sessionQuery1.setParameter("listIds", List.of(1L, 2L, 3L))).thenReturn(sessionQuery1);

        DataMigrationException dataMigrationException =
            assertThrows(DataMigrationException.class, () -> this.dataMigration.hibernateMigrate());
        assertEquals("Error while performing the migration: filters are not properly deleted.",
            dataMigrationException.getMessage());

        assertEquals(2, logCapture.size());
        // Loop 1
        assertEquals("Found [3] filters to migrate...", logCapture.getMessage(0));
        assertEquals("Migrating filters for [1] entities", logCapture.getMessage(1));

        verify(this.filterPreferenceStore).saveFilterPreferences(new DocumentReference("sub", "XWiki", "User1"),
            List.of(
                new DefaultNotificationFilterPreference(filterPref1, false),
                new DefaultNotificationFilterPreference(filterPref2, false),
                new DefaultNotificationFilterPreference(filterPref3, false)
            )
        );
        verify(sessionQuery1).executeUpdate();
    }
}