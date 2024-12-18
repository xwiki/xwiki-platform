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
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CopyAsJob}.
 *
 * @version $Id$
 */
@ComponentTest
class CopyAsJobTest extends AbstractEntityJobTest
{
    @InjectMockComponents
    private CopyAsJob copyAsJob;


    @Override
    protected Job getJob()
    {
        return this.copyAsJob;
    }

    @Test
    void copyAsDocument() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);

        DocumentReference copyReference = new DocumentReference("wiki", "Copy", "Page");
        when(this.modelBridge.copy(sourceReference, copyReference)).thenReturn(true);

        CopyRequest request = new CopyRequest();
        request.setEntityReferences(List.of(sourceReference));
        request.setDestination(copyReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setInteractive(false);
        Map<String, String> parameters = Map.of("foo", "bar");
        request.setEntityParameters(sourceReference, parameters);
        run(request);

        verify(this.modelBridge).copy(sourceReference, copyReference);
        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
    }

}
