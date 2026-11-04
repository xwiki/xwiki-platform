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
package com.xpn.xwiki.store.migration.hibernate;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractHibernateDataMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class AbstractHibernateDataMigrationTest
{
    /**
     * Concrete migration delegating the actual migration work to a callback controlled by the tests.
     */
    @Component
    @Named("TestMigration")
    @Singleton
    public static class TestHibernateDataMigration extends AbstractHibernateDataMigration
    {
        private Runnable migration;

        void setMigration(Runnable migration)
        {
            this.migration = migration;
        }

        @Override
        protected void hibernateMigrate() throws DataMigrationException, XWikiException
        {
            this.migration.run();
        }

        @Override
        public String getDescription()
        {
            return "Test migration";
        }

        @Override
        public XWikiDBVersion getVersion()
        {
            return new XWikiDBVersion(1);
        }
    }

    @InjectMockComponents(role = HibernateDataMigration.class)
    private TestHibernateDataMigration dataMigration;

    @MockComponent
    private Execution execution;

    private XWiki wiki;

    private Runnable migration;

    @BeforeEach
    void setUp()
    {
        XWikiContext context = mock(XWikiContext.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(context);
        this.wiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(this.wiki);

        this.migration = mock(Runnable.class);
        this.dataMigration.setMigration(this.migration);
    }

    @Test
    void migrateFlushesDocumentCacheAfterMigration() throws Exception
    {
        XWikiCacheStoreInterface cacheStore = mock(XWikiCacheStoreInterface.class);
        when(this.wiki.getStore()).thenReturn(cacheStore);

        this.dataMigration.migrate();

        InOrder inOrder = inOrder(this.migration, cacheStore);
        inOrder.verify(this.migration).run();
        inOrder.verify(cacheStore).flushCache();
    }

    @Test
    void migrateWithoutCacheStore() throws Exception
    {
        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        when(this.wiki.getStore()).thenReturn(store);

        this.dataMigration.migrate();

        verify(this.migration).run();
    }

    @Test
    void migrateDoesNotFlushDocumentCacheWhenMigrationFails()
    {
        XWikiCacheStoreInterface cacheStore = mock(XWikiCacheStoreInterface.class);
        when(this.wiki.getStore()).thenReturn(cacheStore);
        RuntimeException failure = new RuntimeException("Migration failure");
        doThrow(failure).when(this.migration).run();

        DataMigrationException exception =
            assertThrows(DataMigrationException.class, () -> this.dataMigration.migrate());

        assertEquals("Data migration TestMigration failed", exception.getMessage());
        assertSame(failure, exception.getCause());
        verify(cacheStore, never()).flushCache();
    }
}
