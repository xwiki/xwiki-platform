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
package org.xwiki.annotation.io.internal.migration.hibernate;

import java.util.List;
import java.util.Locale;

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
import com.xpn.xwiki.doc.XWikiDocument;
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
 * Test of {@link R180600000XWIKI20699DataMigration}.
 *
 * @version $Id$
 * @since 17.10.11
 * @since 18.4.3
 * @since 18.6.0
 */
@ComponentTest
class R180600000XWIKI20699DataMigrationTest
{
    private static final String TASK_TYPE = "internal-annotation-target-fix";

    private static final String WIKI_ID = "wiki1";

    private static final String SELECT_QUERY = "SELECT distinct doc.fullName "
        + "FROM XWikiDocument doc, BaseObject as obj, StringProperty as prop "
        + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiComments' "
        + "and obj.id = prop.id.id "
        + "and prop.id.name = 'target' "
        + "and length(prop.value) > 0";

    @InjectMockComponents(role = HibernateDataMigration.class)
    private R180600000XWIKI20699DataMigration migration;

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
        XWikiStoreInterface store = mock(XWikiStoreInterface.class);

        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWikiId()).thenReturn(WIKI_ID);
        when(this.wiki.getName()).thenReturn(WIKI_ID);
        when(this.wiki.getStore()).thenReturn(store);
        when(store.getQueryManager()).thenReturn(this.queryManager);

        when(this.queryManager.createQuery(SELECT_QUERY, Query.HQL)).thenReturn(this.query);
        when(this.query.setWiki(any())).thenReturn(this.query);
    }

    @Test
    void getVersion()
    {
        assertEquals(180600000, this.migration.getVersion().getVersion());
    }

    @Test
    void migrate() throws Exception
    {
        DocumentReference selfRef = new DocumentReference("xwiki", "Annot", "SelfRef");
        DocumentReference mixed = new DocumentReference("xwiki", "Annot", "Mixed");

        when(this.query.execute()).thenReturn(List.of("Annot.SelfRef", "Annot.Mixed"));
        when(this.resolver.resolve("Annot.SelfRef")).thenReturn(selfRef);
        when(this.resolver.resolve("Annot.Mixed")).thenReturn(mixed);

        this.migration.migrate();

        verify(this.query).setWiki(WIKI_ID);
        verify(this.taskManager).addTask(WIKI_ID, documentId(selfRef), TASK_TYPE);
        verify(this.taskManager).addTask(WIKI_ID, documentId(mixed), TASK_TYPE);

        assertEquals(3, this.logCapture.size());
        assertEquals("[2] documents queued to task [internal-annotation-target-fix]", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("document [xwiki:Annot.SelfRef()] queued to task [internal-annotation-target-fix]",
            this.logCapture.getMessage(1));
        assertEquals("document [xwiki:Annot.Mixed()] queued to task [internal-annotation-target-fix]",
            this.logCapture.getMessage(2));
    }

    @Test
    void migrateNoDocumentToFix() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());

        this.migration.migrate();

        verifyNoInteractions(this.taskManager);
        assertEquals(1, this.logCapture.size());
        assertEquals("[0] documents queued to task [internal-annotation-target-fix]", this.logCapture.getMessage(0));
    }

    @Test
    void migrateQueryException() throws Exception
    {
        when(this.query.execute()).thenThrow(QueryException.class);

        DataMigrationException exception = assertThrows(DataMigrationException.class, () -> this.migration.migrate());

        assertEquals("Data migration R180600000XWIKI20699 failed", exception.getMessage());
        assertEquals("Failed retrieve the list of all the documents with annotations for wiki [wiki1].",
            exception.getCause().getMessage());
        assertEquals(QueryException.class, exception.getCause().getCause().getClass());
        verifyNoInteractions(this.taskManager);
    }

    /**
     * The queued identifier is derived from the reference <em>and</em> its locale, so a document queued with the wrong
     * locale addresses a different, non-existing document.
     */
    private long documentId(DocumentReference reference)
    {
        return new XWikiDocument(new DocumentReference(reference, Locale.ROOT)).getId();
    }
}
