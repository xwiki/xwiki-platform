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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link R160401000XWIKI22271DataMigration}.
 *
 * @version $Id$
 * @since 16.5.0
 * @since 16.4.1
 */
@ComponentTest
class R160401000XWIKI22271DataMigrationTest
{
    private static final String CURRENT_WIKI = "current_Wiki";
    private static final String CURRENT_WIKI_ESCAPED = "current!_Wiki";

    @InjectMockComponents(role = HibernateDataMigration.class)
    private R160401000XWIKI22271DataMigration dataMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Execution execution;

    private XWikiContext context;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private HibernateStore hibernateStore;
    private XWikiHibernateStore hibernateBaseStore;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        this.hibernateStore = componentManager.registerMockComponent(HibernateStore.class);
        this.hibernateBaseStore = componentManager.registerMockComponent(XWikiStoreInterface.class,
            XWikiHibernateBaseStore.HINT, XWikiHibernateStore.class, false);
    }

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of(
            "wikiA", CURRENT_WIKI, "wikiB"
        ));

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);

        this.context = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        when(this.context.getWikiId()).thenReturn(CURRENT_WIKI);
    }

    @Test
    void hibernateMigrate() throws QueryException, XWikiException, DataMigrationException
    {
        // We simulate 2 loops (3rd is just getting empty value)
        Query query1 = mock(Query.class, "query1");
        Query query2 = mock(Query.class, "query2");
        Query query3 = mock(Query.class, "query3");

        when(this.queryManager.createQuery("select nfp "
            + "from DefaultNotificationFilterPreference nfp "
            + "where nfp.id not in ("
            + "select nfp2.id from DefaultNotificationFilterPreference nfp2 "
            + "where nfp2.page like :wikiPrefix or "
            + "nfp2.pageOnly like :wikiPrefix or "
            + "nfp2.user like :wikiPrefix or "
            + "nfp2.wiki = :wikiId"
            + ")", Query.HQL))
            .thenReturn(query1)
            .thenReturn(query2)
            .thenReturn(query3);

        when(query1.bindValue("wikiPrefix", CURRENT_WIKI_ESCAPED + ":%")).thenReturn(query1);
        when(query1.bindValue("wikiId", CURRENT_WIKI)).thenReturn(query1);

        when(query1.setOffset(0)).thenReturn(query1);
        when(query1.setLimit(100)).thenReturn(query1);

        List resultQuery1 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            DefaultNotificationFilterPreference pref = mock(DefaultNotificationFilterPreference.class, "pref_" + i);
            when(pref.getInternalId()).thenReturn(Long.valueOf(i));
            if (List.of(11, 15, 32).contains(i)) {
                when(pref.getWikiId()).thenReturn(Optional.of("unknown"));
            } else if (i % 2 == 0) {
                when(pref.getWikiId()).thenReturn(Optional.of("wikiA"));
            } else {
                when(pref.getWikiId()).thenReturn(Optional.of("wikiB"));
            }
            resultQuery1.add(pref);
        }
        when(query1.execute()).thenReturn(resultQuery1);

        // query 2
        when(query2.bindValue("wikiPrefix", CURRENT_WIKI_ESCAPED + ":%")).thenReturn(query2);
        when(query2.bindValue("wikiId", CURRENT_WIKI)).thenReturn(query2);

        when(query2.setOffset(100)).thenReturn(query2);
        when(query2.setLimit(100)).thenReturn(query2);

        List resultQuery2 = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            DefaultNotificationFilterPreference pref = mock(DefaultNotificationFilterPreference.class, "pref2_" + i);
            when(pref.getInternalId()).thenReturn(100L + i);
            if (i % 5 == 0) {
                when(pref.getWikiId()).thenReturn(Optional.of("Subwiki"));
            } else if (i == 12) {
                when(pref.getWikiId()).thenReturn(Optional.empty());
            } else {
                when(pref.getWikiId()).thenReturn(Optional.of(CURRENT_WIKI));
            }
            resultQuery2.add(pref);
        }
        when(query2.execute()).thenReturn(resultQuery2);

        // query 3
        when(query3.bindValue("wikiPrefix", CURRENT_WIKI_ESCAPED + ":%")).thenReturn(query3);
        when(query3.bindValue("wikiId", CURRENT_WIKI)).thenReturn(query3);

        when(query3.setOffset(136)).thenReturn(query3);
        when(query3.setLimit(100)).thenReturn(query3);

        when(query3.execute()).thenReturn(List.of());

        Set<Long> expectedSet = Set.of(
            11L,
            15L,
            32L,
            100L,
            105L,
            110L,
            112L,
            115L,
            120L,
            125L,
            130L,
            135L
        );
        Session session = mock(Session.class, "session");
        when(this.hibernateBaseStore.executeWrite(eq(context), any())).then(invocationOnMock -> {
            XWikiHibernateBaseStore.HibernateCallback callback = invocationOnMock.getArgument(1);
            return callback.doInHibernate(session);
        });
        org.hibernate.query.Query sessionQuery = mock(org.hibernate.query.Query.class, "sessionQuery");
        when(session.createQuery("delete from DefaultNotificationFilterPreference "
            + "where internalId in (:filterIds)")).thenReturn(sessionQuery);
        when(sessionQuery.setParameter("filterIds", expectedSet)).thenReturn(sessionQuery);

        this.dataMigration.hibernateMigrate();
        assertEquals(3, logCapture.size());

        assertEquals("Performing analysis of [100] filters to find those related to removed wikis",
            logCapture.getMessage(0));
        assertEquals("Performing analysis of [36] filters to find those related to removed wikis",
            logCapture.getMessage(1));
        assertEquals("Removing [12] filters related to deleted wikis",
            logCapture.getMessage(2));

        verify(query1).execute();
        verify(query2).execute();
        verify(query3).execute();
        verify(query1).bindValue("wikiPrefix", CURRENT_WIKI_ESCAPED + ":%");
        verify(query1).bindValue("wikiId", CURRENT_WIKI);
        verify(query2).bindValue("wikiPrefix", CURRENT_WIKI_ESCAPED + ":%");
        verify(query2).bindValue("wikiId", CURRENT_WIKI);
        verify(query3).bindValue("wikiPrefix", CURRENT_WIKI_ESCAPED + ":%");
        verify(query3).bindValue("wikiId", CURRENT_WIKI);
        verify(sessionQuery).executeUpdate();
    }
}
