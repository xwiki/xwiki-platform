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
package org.xwiki.resource.internal;

import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultResourceReferenceHandlerChain}.
 *
 * @version $Id$
 * @since 6.1M2
 */
class DefaultResourceReferenceHandlerChainTest
{
    @Test
    void executeNextWhenNoMoreAction() throws Exception
    {
        DefaultResourceReferenceHandlerChain chain = DefaultResourceReferenceHandlerChain.EMPTY;
        Queue<ResourceReferenceHandler> queue = mock(Queue.class);
        when(queue.isEmpty()).thenReturn(true);
        ReflectionUtils.setFieldValue(chain, "handlerStack", queue);

        chain.handleNext(mock(ResourceReference.class));

        // Verify that we don't get a ResourceReferenceHandler since the queue is empty.
        verify(queue, never()).poll();
    }
}
