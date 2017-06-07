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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.refactoring.batch.BatchOperation;
import org.xwiki.refactoring.batch.BatchOperationExecutor;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

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
public class DefaultBatchOperationExecutorTest
{
    @Rule
    public MockitoComponentMockingRule<BatchOperationExecutor> mocker =
        new MockitoComponentMockingRule<>(DefaultBatchOperationExecutor.class);

    private Execution execution;

    private ExecutionContext executionContext;

    @Before
    public void setup() throws Exception
    {
        executionContext = mock(ExecutionContext.class);
        execution = mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void executeWithNoSpecifiedBatchId() throws Exception
    {
        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        // Execute it.
        mocker.getComponentUnderTest().execute(operation, null);

        // Verify, in order that:
        InOrder inOrder = inOrder(operation, executionContext);

        // * a batch ID is generated and set
        inOrder.verify(executionContext, times(1)).setProperty(eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY),
            matches(".*.*-.*-.*"));

        // * the operation is executed
        inOrder.verify(operation).execute();

        // * the batch ID is cleaned from the context.
        inOrder.verify(executionContext, times(1)).setProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY, null);
    }

    @Test
    public void executeWithSpecifiedBatchId() throws Exception
    {
        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        String specifiedBatchId = "a-b-c-d";

        // Execute it.
        mocker.getComponentUnderTest().execute(operation, specifiedBatchId);

        // Verify, in order that:
        InOrder inOrder = inOrder(operation, executionContext);

        // * the specified batch ID is used
        inOrder.verify(executionContext, times(1)).setProperty(eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY),
            eq(specifiedBatchId));

        // * the operation is executed
        inOrder.verify(operation).execute();

        // * the batch ID is cleaned from the context.
        inOrder.verify(executionContext, times(1)).setProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY, null);
    }

    @Test
    public void executeNested() throws Exception
    {
        // Set an existing batchId in the execution, i.e. we are in a nested batch operation execution.
        String existingBatchId = "a-b-c-d";
        when(executionContext.getProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY)).thenReturn(existingBatchId);

        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        // Execute it.
        mocker.getComponentUnderTest().execute(operation);

        // Verify that:

        // * the existing batch ID is detected and the context batch ID will not be touched (re-set or cleared)
        verify(executionContext, never()).setProperty(eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY), any());

        // * the operation is executed
        verify(operation).execute();
    }

    @Test
    public void executeNestedWithSpecifiedBatchId() throws Exception
    {
        // Set an existing batchId in the execution, i.e. we are in a nested batch operation execution.
        String existingBatchId = "a-b-c-d";
        when(executionContext.getProperty(DefaultBatchOperationExecutor.CONTEXT_PROPERTY)).thenReturn(existingBatchId);

        String specifiedBatchId = "d-c-b-a";

        // Mock an operation.
        BatchOperation operation = mock(BatchOperation.class);

        // Execute it.
        mocker.getComponentUnderTest().execute(operation, specifiedBatchId);

        // Verify that:

        // * the existing batch ID is detected and the context batch ID will not be touched (re-set or cleared), even if
        // a batch ID is specified explicitly.
        verify(executionContext, never()).setProperty(eq(DefaultBatchOperationExecutor.CONTEXT_PROPERTY), any());

        // * the operation is executed
        verify(operation).execute();
    }
}
