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
package org.xwiki.index.internal;

import javax.inject.Named;
import javax.inject.Provider;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.tasks.XWikiDocumentIndexingTask;
import com.xpn.xwiki.doc.tasks.XWikiDocumentIndexingTaskId;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link TasksStore}.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@ComponentTest
class TasksStoreTest
{
    @InjectMockComponents
    private TasksStore tasksStore;

    @MockComponent
    private ExecutionContextManager contextManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("readonly")
    private Provider<XWikiContext> readonlyxcontextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private HibernateStore store;

    @Mock
    private XWikiContext context;

    @Mock
    private Session session;

    @Mock
    private Query query;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.readonlyxcontextProvider.get()).thenReturn(this.context);
        when(this.store.getCurrentSession()).thenReturn(this.session);
        when(this.session.createQuery(anyString())).thenReturn(this.query);
        when(this.query.setParameter(anyString(), any())).thenReturn(this.query);
    }

    @Test
    void getAllTasks() throws Exception
    {
        this.tasksStore.getAllTasks("wikiId", "instance-id");
        verify(this.contextManager).initialize(any());
        verify(this.context).setWikiId("wikiId");
        verify(this.session).createQuery("SELECT t FROM XWikiDocumentIndexingTask t " 
            + "WHERE t.id.instanceId = :instanceId");
        verify(this.query).setParameter("instanceId", "instance-id");
        verify(this.query).getResultList();
    }

    @Test
    void addTask() throws Exception
    {
        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        XWikiDocumentIndexingTaskId id = new XWikiDocumentIndexingTaskId();
        id.setInstanceId("instance-id");
        id.setType("testtask");
        id.setVersion("7.1");
        id.setDocId(42);
        task.setId(id);
        this.tasksStore.addTask("wikiId", task);
        verify(this.contextManager).initialize(any());
        verify(this.context).setWikiId("wikiId");
        verify(this.session).saveOrUpdate(task);
        assertNotNull(task.getTimestamp());
    }

    @Test
    void deleteTask() throws Exception
    {
        this.tasksStore.deleteTask("wikiId", 42, "7.1", "testtask");
        verify(this.contextManager).initialize(any());
        verify(this.context).setWikiId("wikiId");
        verify(this.session).createQuery("delete from XWikiDocumentIndexingTask t where t.id.docId = :docId "
            + "and t.id.version = :version and t.id.type = :type");
        verify(this.query).setParameter("docId", 42L);
        verify(this.query).setParameter("version", "7.1");
        verify(this.query).setParameter("type", "testtask");
        verify(this.query).executeUpdate();
    }

    @Test
    void replaceTask() throws Exception
    {
        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        XWikiDocumentIndexingTaskId id = new XWikiDocumentIndexingTaskId();
        id.setDocId(42);
        id.setType("testtask");
        task.setId(id);
        this.tasksStore.replaceTask("wikiId", task);
        verify(this.contextManager).initialize(any());
        verify(this.context).setWikiId("wikiId");
        verify(this.session).createQuery("delete from XWikiDocumentIndexingTask t where t.id.docId = :docId " 
            + "and t.id.type = :type");
        verify(this.query).setParameter("docId", 42L);
        verify(this.query).setParameter("type", "testtask");
        verify(this.query).executeUpdate();
        verify(this.session).saveOrUpdate(task);
        assertNotNull(task.getTimestamp());
    }

    @Test
    void getDocument() throws Exception
    {
        this.tasksStore.getDocument("wikiId", 42);
        verify(this.contextManager).initialize(any());
        verify(this.context).setWikiId("wikiId");
        verify(this.session).createQuery("select doc from XWikiDocument doc where doc.id = :docId");
        verify(this.query).setParameter("docId", 42L);
        verify(this.query).getSingleResult();
    }
}
