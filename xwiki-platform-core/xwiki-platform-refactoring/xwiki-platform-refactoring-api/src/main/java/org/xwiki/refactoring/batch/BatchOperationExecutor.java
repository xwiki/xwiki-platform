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
package org.xwiki.refactoring.batch;

import org.xwiki.component.annotation.Role;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

/**
 * Executes a given operation and wraps its sub-operations as part of the same batch (by passing a batch ID to the
 * {@link ExecutionContext}).
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
public interface BatchOperationExecutor
{
    /**
     * Executes a given operation and generates a batch ID if none is already set.
     *
     * @param operation the batch operation to execute
     * @param <E> the type of exception thrown by the {@link BatchOperation}
     * @throws E in case of problems
     */
    <E extends Exception> void execute(BatchOperation<E> operation) throws E;

    /**
     * @param operation the batch operation to execute
     * @param batchId the batch ID to use if none is already set. If {@code null}, a value will be generated
     * @param <E> the type of exception thrown by the {@link BatchOperation}
     * @throws E in case of problems
     */
    <E extends Exception> void execute(BatchOperation<E> operation, String batchId) throws E;

    /**
     * @return the current {@link Execution}'s batch ID or {@code null} if none is set
     */
    String getCurrentBatchId();
}
