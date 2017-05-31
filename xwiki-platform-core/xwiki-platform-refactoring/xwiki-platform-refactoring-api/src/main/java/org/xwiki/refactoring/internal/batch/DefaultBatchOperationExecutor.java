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

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.context.Execution;
import org.xwiki.refactoring.batch.BatchOperation;
import org.xwiki.refactoring.batch.BatchOperationExecutor;

/**
 * Default implementation for {@link BatchOperationExecutor}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultBatchOperationExecutor implements BatchOperationExecutor
{
    private static final String CONTEXT_PROPERTY = "BATCH_ID";

    @Inject
    private Execution execution;

    @Override
    public <E extends Exception> void execute(BatchOperation<E> operation) throws E
    {
        String batchId = generateBatchId();
        execute(operation, batchId);
    }

    @Override
    public <E extends Exception> void execute(BatchOperation<E> operation, String batchId) throws E
    {
        boolean cleanBatchId = false;
        String batchIdToUse = getCurrentBatchId();
        if (StringUtils.isBlank(batchIdToUse)) {
            // Set the specified or a newly generated batch ID.
            batchIdToUse = batchId;
            if (StringUtils.isBlank(batchIdToUse)) {
                batchIdToUse = generateBatchId();
            }
            execution.getContext().setProperty(CONTEXT_PROPERTY, batchId);

            // Remember that we`ve set it and we have to clean it when done.
            cleanBatchId = true;
        }

        try {
            // Execute the operation.
            operation.execute();
        } finally {
            if (cleanBatchId) {
                // Clean the context when done.
                execution.getContext().setProperty(CONTEXT_PROPERTY, null);
            }
        }
    }

    protected String generateBatchId()
    {
        String result = UUID.randomUUID().toString();
        return result;
    }

    @Override
    public String getCurrentBatchId()
    {
        Object existingBatchIdObject = execution.getContext().getProperty(CONTEXT_PROPERTY);
        String batchId = Objects.toString(existingBatchIdObject, null);

        return batchId;
    }
}
