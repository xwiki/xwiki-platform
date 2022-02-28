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
package org.xwiki.index.migration;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.index.TaskManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link R1402000XWIKI19352DataMigration}.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@ComponentTest
class R1402000XWIKI19352DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R1402000XWIKI19352DataMigration migration;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private TaskManager taskManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWikiId()).thenReturn("wiki1");
        when(this.queryManager.createQuery("SELECT doc.id, doc.version FROM XWikiDocument doc",
            Query.HQL)).thenReturn(this.query);
        when(this.query.setWiki(any())).thenReturn(this.query);
    }

    @Test
    void migrate() throws Exception
    {
        when(this.wiki.hasBacklinks(this.context)).thenReturn(true);

        when(this.query.execute()).thenReturn(List.of(
            new Object[] { 42L, "1.4" },
            new Object[] { 43L, "1.5" }
        ));

        this.migration.migrate();

        verify(this.query).setWiki("wiki1");
        verify(this.taskManager).replaceTask("wiki1", 42L, "1.4", "links");
        verify(this.taskManager).replaceTask("wiki1", 43L, "1.5", "links");
    }

    @Test
    void migrateQueryException() throws Exception
    {
        when(this.wiki.hasBacklinks(this.context)).thenReturn(true);

        when(this.query.execute()).thenThrow(QueryException.class);

        DataMigrationException queryException =
            assertThrows(DataMigrationException.class, () -> this.migration.migrate());

        assertEquals("Failed retrieve the list of all the documents for wiki [wiki1].", queryException.getMessage());
        assertEquals(QueryException.class, queryException.getCause().getClass());

        verify(this.query).setWiki("wiki1");
        verifyNoInteractions(this.taskManager);
    }

    @Test
    void migrateNotHasBacklinks() throws Exception
    {
        when(this.wiki.hasBacklinks(this.context)).thenReturn(false);
        this.migration.migrate();
        verifyNoInteractions(this.queryManager);
        verifyNoInteractions(this.taskManager);
    }
}
