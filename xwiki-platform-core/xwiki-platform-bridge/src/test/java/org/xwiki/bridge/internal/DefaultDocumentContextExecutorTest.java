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
package org.xwiki.bridge.internal;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultDocumentContextExecutor}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultDocumentContextExecutorTest
{
    private static final WikiReference TEST_WIKI_REFERENCE = new WikiReference("xwiki");

    private static final DocumentReference TEST_DOCUMENT_REFERENCE =
        new DocumentReference(TEST_WIKI_REFERENCE.getName(), "Space", "WebHome");

    private static final WikiReference INITIAL_WIKI_REFERENCE = new WikiReference("initial");

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @InjectMockComponents
    private DefaultDocumentContextExecutor executor;

    @Mock
    private Callable<Object> mockCallable;

    @Mock
    private DocumentModelBridge documentModelBridge;

    private WikiReference modelWikiReference;

    @BeforeEach
    void beforeEach()
    {
        when(this.documentModelBridge.getDocumentReference()).thenReturn(TEST_DOCUMENT_REFERENCE);

        // Mock actually storing the wiki reference
        this.modelWikiReference = INITIAL_WIKI_REFERENCE;
        when(this.modelContext.getCurrentEntityReference()).thenReturn(this.modelWikiReference);
        doAnswer(invocationOnMock -> {
            EntityReference entityWikiReference =
                invocationOnMock.getArgument(0, EntityReference.class).extractReference(EntityType.WIKI);
            this.modelWikiReference = new WikiReference(entityWikiReference.getName());
            return null;
        }).when(this.modelContext).setCurrentEntityReference(any(EntityReference.class));
    }

    @Test
    void call() throws Exception
    {
        String callResult = "callResult";
        when(this.mockCallable.call()).thenReturn(callResult);
        AtomicReference<Map<String, Object>> backupObjects = new AtomicReference<>();
        doAnswer(invocationOnMock -> {
            backupObjects.set(invocationOnMock.getArgument(0));
            return null;
        }).when(this.documentAccessBridge).pushDocumentInContext(any(), same(this.documentModelBridge));

        Object actualResult = this.executor.call(this.mockCallable, this.documentModelBridge);

        assertSame(callResult, actualResult);
        InOrder inOrder = inOrder(this.documentAccessBridge, this.modelContext);
        inOrder.verify(this.documentAccessBridge).pushDocumentInContext(any(), same(this.documentModelBridge));
        inOrder.verify(this.modelContext).setCurrentEntityReference(TEST_WIKI_REFERENCE);
        inOrder.verify(this.documentAccessBridge).popDocumentFromContext(backupObjects.get());
        inOrder.verify(this.modelContext).setCurrentEntityReference(INITIAL_WIKI_REFERENCE);
    }

    @Test
    void popWhenCallableThrows() throws Exception
    {
        Exception testException = new Exception("Callable failed");
        when(this.mockCallable.call()).thenThrow(testException);

        assertThrows(Exception.class, () -> this.executor.call(this.mockCallable, this.documentModelBridge));
        InOrder inOrder = inOrder(this.documentAccessBridge, this.modelContext);
        inOrder.verify(this.modelContext).setCurrentEntityReference(TEST_WIKI_REFERENCE);
        inOrder.verify(this.documentAccessBridge).popDocumentFromContext(any());
        inOrder.verify(this.modelContext).setCurrentEntityReference(INITIAL_WIKI_REFERENCE);
    }

    @Test
    void doNothingWhenPushThrows() throws Exception
    {
        Exception testException = new Exception("Test");
        doThrow(testException)
            .when(this.documentAccessBridge).pushDocumentInContext(any(), same(this.documentModelBridge));

        assertThrows(Exception.class, () -> this.executor.call(this.mockCallable, this.documentModelBridge));

        verifyNoInteractions(this.mockCallable);
        verify(this.modelContext, never()).setCurrentEntityReference(any());
    }
}
