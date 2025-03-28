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
package org.xwiki.refactoring.internal.job;

import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.MoveRequest;

import static org.mockito.Mockito.*;

/**
 * Base class for writing unit tests for jobs extending {@link MoveJob}.
 * 
 * @version $Id$
 */
public abstract class AbstractMoveJobTest extends AbstractEntityJobTest
{
    protected MoveRequest createRequest(EntityReference source, EntityReference destination)
    {
        MoveRequest request = new MoveRequest();
        request.setEntityReferences(List.of(source));
        request.setDestination(destination);
        return request;
    }

    protected void verifyNoMove() throws Exception
    {
        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
    }
}
