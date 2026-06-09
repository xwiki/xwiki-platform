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
package org.xwiki.refactoring.internal.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.refactoring.batch.BatchOperation;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultBatchOperationExecutor}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultBatchOperationExecutorTest
{
    @InjectMockComponents
    private DefaultBatchOperationExecutor batchOperationExecutor;

    @MockComponent
    private Execution execution;

    @Mock
    private ExecutionContext executionContext;

    @BeforeEach
    void setup()
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
    }

    @Test
    void executeWithNoSpecifiedBatchId() throws Exception
    {
        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        // Execute it.
        this.batchOperationExecutor.execute(operation, null);

        // Verify, in order that:
        InOrder inOrder = inOrder(operation, this.executionContext);

        // * a batch ID is generated and set
        inOrder.verify(this.executionContext, times(1)).setProperty(
            eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY), matches(".*.*-.*-.*"));

        // * the operation is executed
        inOrder.verify(operation).execute();

        // * the batch ID is cleaned from the context.
        inOrder.verify(this.executionContext, times(1)).setProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY,
            null);
    }

    @Test
    void executeWithSpecifiedBatchId() throws Exception
    {
        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        String specifiedBatchId = "a-b-c-d";

        // Execute it.
        this.batchOperationExecutor.execute(operation, specifiedBatchId);

        // Verify, in order that:
        InOrder inOrder = inOrder(operation, this.executionContext);

        // * the specified batch ID is used
        inOrder.verify(this.executionContext, times(1)).setProperty(
            eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY), eq(specifiedBatchId));

        // * the operation is executed
        inOrder.verify(operation).execute();

        // * the batch ID is cleaned from the context.
        inOrder.verify(this.executionContext, times(1)).setProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY,
            null);
    }

    @Test
    void executeNested() throws Exception
    {
        // Set an existing batchId in the execution, i.e. we are in a nested batch operation execution.
        String existingBatchId = "a-b-c-d";
        when(this.executionContext.getProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY))
            .thenReturn(existingBatchId);

        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        // Execute it.
        this.batchOperationExecutor.execute(operation);

        // Verify that:

        // * the existing batch ID is detected and the context batch ID will not be touched (re-set or cleared)
        verify(this.executionContext, never()).setProperty(eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY), any());

        // * the operation is executed
        verify(operation).execute();
    }

    @Test
    void executeNestedWithSpecifiedBatchId() throws Exception
    {
        // Set an existing batchId in the execution, i.e. we are in a nested batch operation execution.
        String existingBatchId = "a-b-c-d";
        when(this.executionContext.getProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY))
            .thenReturn(existingBatchId);

        String specifiedBatchId = "d-c-b-a";

        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        // Execute it.
        this.batchOperationExecutor.execute(operation, specifiedBatchId);

        // Verify that:

        // * the existing batch ID is detected and the context batch ID will not be touched (re-set or cleared), even if
        // a batch ID is specified explicitly.
        verify(this.executionContext, never()).setProperty(eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY), any());

        // * the operation is executed
        verify(operation).execute();
    }
}
