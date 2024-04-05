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
package org.xwiki.index.internal.migration;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link R140300000XWIKI19614DataMigration}.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@ComponentTest
class R140300000XWIKI19614DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R140300000XWIKI19614DataMigration migration;

    @Mock
    private QueryManager queryManager;

    @MockComponent
    private TaskManager taskManager;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        XWikiStoreInterface xWikiStoreInterface = mock(XWikiStoreInterface.class);

        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getName()).thenReturn("wiki1");
        when(this.context.getWikiId()).thenReturn("wiki1");
        when(this.wiki.getStore()).thenReturn(xWikiStoreInterface);
        when(xWikiStoreInterface.getQueryManager()).thenReturn(this.queryManager);

        when(this.queryManager.createQuery("SELECT doc.fullName, doc.language FROM XWikiDocument doc",
            Query.HQL)).thenReturn(this.query);
        when(this.query.setWiki(any())).thenReturn(this.query);
    }

    @Test
    void migrate() throws Exception
    {
        DocumentReference doc42 = new DocumentReference("xwiki", "XWiki", "Doc42");
        DocumentReference doc43 = new DocumentReference("xwiki", "XWiki", "Doc43");
        DocumentReference doc44 = new DocumentReference("xwiki", "XWiki", "Doc44");

        when(this.wiki.hasBacklinks(this.context)).thenReturn(true);

        when(this.query.execute()).thenReturn(List.of(
            new String[] { "XWiki.Doc42", "" },
            new String[] { "XWiki.Doc43", "fr" },
            // Simulate a result return by Oracle, when the empty string is returned as null
            new String[] { "XWiki.Doc44", null }
        ));
        when(this.resolver.resolve("XWiki.Doc42")).thenReturn(doc42);
        when(this.resolver.resolve("XWiki.Doc43")).thenReturn(doc43);
        when(this.resolver.resolve("XWiki.Doc44")).thenReturn(doc44);

        this.migration.migrate();

        verify(this.query).setWiki("wiki1");
        verify(this.taskManager).addTask("wiki1", -7672023672109484185L, "links");
        verify(this.taskManager).addTask("wiki1", -3303915339101339304L, "links");
        verify(this.taskManager).addTask("wiki1", 1722724816638329912L, "links");

        assertEquals("[3] documents queued to task [links]", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void migrateQueryException() throws Exception
    {
        when(this.wiki.hasBacklinks(this.context)).thenReturn(true);

        when(this.query.execute()).thenThrow(QueryException.class);

        DataMigrationException queryException =
            assertThrows(DataMigrationException.class, () -> this.migration.migrate());

        assertEquals("Data migration R140300000XWIKI19614 failed", queryException.getMessage());
        assertEquals("Failed retrieve the list of all the documents for wiki [wiki1].",
            queryException.getCause().getMessage());
        assertEquals(DataMigrationException.class, queryException.getCause().getClass());
        assertEquals(QueryException.class, queryException.getCause().getCause().getClass());

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
        assertEquals("Skipped because backlinks are not supported on [wiki1]", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
    }
}
