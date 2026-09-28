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
package org.xwiki.internal.migration;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link R180900000XWIKI15074DataMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class R180900000XWIKI15074DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R180900000XWIKI15074DataMigration dataMigration;

    @MockComponent
    private Execution execution;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private XWikiContext context;

    private XWikiHibernateStore hibernateStore;

    private Query query;

    private org.hibernate.query.Query<?> updateQuery;

    @BeforeEach
    void setUp() throws Exception
    {
        this.context = mock(XWikiContext.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        XWiki wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(wiki);

        XWikiCacheStore store = mock(XWikiCacheStore.class);
        when(wiki.getStore()).thenReturn(store);
        QueryManager queryManager = mock(QueryManager.class);
        when(store.getQueryManager()).thenReturn(queryManager);
        this.query = mock(Query.class);
        when(queryManager.createQuery(startsWith("select translation.id, original.syntaxId "), any()))
            .thenReturn(this.query);
        when(this.query.setLimit(100)).thenReturn(this.query);

        this.hibernateStore = mock(XWikiHibernateStore.class);
        when(wiki.getHibernateStore()).thenReturn(this.hibernateStore);
        Session session = mock(Session.class);
        this.updateQuery = mock(org.hibernate.query.Query.class);
        when(session.createQuery(anyString())).thenReturn((org.hibernate.query.Query) this.updateQuery);
        when(this.updateQuery.setParameter(anyString(), any())).thenReturn((org.hibernate.query.Query) this.updateQuery);
        when(this.hibernateStore.executeWrite(any(XWikiContext.class), any(HibernateCallback.class)))
            .thenAnswer(invocation -> invocation.<HibernateCallback<?>>getArgument(1).doInHibernate(session));
    }

    @Test
    void migrate() throws Exception
    {
        List<Object[]> firstBatch = new ArrayList<>();
        firstBatch.add(new Object[] { 1L, "markdown/1.2" });
        firstBatch.add(new Object[] { 2L, "xwiki/2.0" });
        List<Object[]> secondBatch = new ArrayList<>();
        secondBatch.add(new Object[] { 3L, "plain/1.0" });
        when(this.query.execute()).thenReturn((List) firstBatch, (List) secondBatch, List.of());
        when(this.updateQuery.executeUpdate()).thenReturn(1);

        this.dataMigration.migrate();

        verify(this.updateQuery).setParameter("syntaxId", "markdown/1.2");
        verify(this.updateQuery).setParameter("id", 1L);
        verify(this.updateQuery).setParameter("syntaxId", "xwiki/2.0");
        verify(this.updateQuery).setParameter("id", 2L);
        verify(this.updateQuery).setParameter("syntaxId", "plain/1.0");
        verify(this.updateQuery).setParameter("id", 3L);
        verify(this.hibernateStore, times(2)).beginTransaction(this.context);
        verify(this.hibernateStore, times(2)).endTransaction(this.context, true);
        assertEquals("Updated the syntax of [3] document translations to match their original document.",
            this.logCapture.getMessage(0));
    }

    @Test
    void migrateWhenNoTranslationIsUpdated() throws Exception
    {
        List<Object[]> batch = new ArrayList<>();
        batch.add(new Object[] { 1L, "markdown/1.2" });
        when(this.query.execute()).thenReturn((List) batch);
        when(this.updateQuery.executeUpdate()).thenReturn(0);

        DataMigrationException exception = assertThrows(DataMigrationException.class, this.dataMigration::migrate);

        assertEquals("Failed to update the syntax of the [1] translations with id [1] and the following ones.",
            exception.getCause().getMessage());
        verify(this.hibernateStore).endTransaction(this.context, true);
    }
}
