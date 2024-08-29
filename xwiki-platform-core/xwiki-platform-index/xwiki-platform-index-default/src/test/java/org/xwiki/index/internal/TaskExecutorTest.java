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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link TaskExecutor}.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@ComponentTest
class TaskExecutorTest
{
    public static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wikiId", "space", "page");

    @InjectMockComponents
    private TaskExecutor taskExecutor;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Provider<TasksStore> tasksStoreProvider;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    private ExecutionContextManager contextManager;

    @Mock
    private XWikiContext context;

    @Mock
    private TasksStore tasksStore;

    @Mock
    private XWikiDocument xwikiDocument;

    @Mock
    private TaskConsumer taskConsumer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;
    
    @MockComponent
    @Named("testtask")
    private TaskConsumer testTaskConsumer;
    

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWikiId()).thenReturn("oldWikiId");
        when(this.tasksStoreProvider.get()).thenReturn(this.tasksStore);
        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @Test
    void execute() throws Exception
    {
        TaskData task = new TaskData(42, "1.5", "testtask", "wikiId");

        when(this.tasksStore.getDocument("wikiId", 42)).thenReturn(this.xwikiDocument);
        when(this.documentRevisionProvider.getRevision(this.xwikiDocument, "1.5")).thenReturn(this.xwikiDocument);

        when(this.xwikiDocument.getDocumentReferenceWithLocale()).thenReturn(DOCUMENT_REFERENCE);
        when(this.xwikiDocument.getVersion()).thenReturn("1.5");

        this.taskExecutor.execute(task);

        verify(this.context).setWikiId("wikiId");
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(false));
        verify(this.contextManager).initialize(any(ExecutionContext.class));
        verify(this.contextManager).popContext();
        verify(this.testTaskConsumer).consume(DOCUMENT_REFERENCE, "1.5");
    }
}
