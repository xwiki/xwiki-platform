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
package org.xwiki.display.internal;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link DocumentContentDisplayer}.
 * 
 * @version $Id$
 */
public class DocumentContentDisplayerTest
{
    @Rule
    public final MockitoComponentMockingRule<DocumentDisplayer> mocker =
        new MockitoComponentMockingRule<DocumentDisplayer>(DocumentContentDisplayer.class);

    @Test
    public void testBaseMetaDataIsSetBeforeExecutingTransformations() throws Exception
    {
        // The execution context is expected to have the "xwikicontext" property set.
        Execution mockExecution = mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", new HashMap<String, Object>());
        Mockito.when(mockExecution.getContext()).thenReturn(executionContext);

        // The document being displayed.
        DocumentModelBridge mockDocument = Mockito.mock(DocumentModelBridge.class);
        XDOM content = new XDOM(Collections.<Block> emptyList());
        Mockito.when(mockDocument.getXDOM()).thenReturn(content);

        // The reference of the current document musts be set as the value of the BASE meta data.
        DocumentReference currentDocRef = new DocumentReference("wiki", "Space", "Page");
        DocumentAccessBridge mockDocumentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        Mockito.when(mockDocumentAccessBridge.getCurrentDocumentReference()).thenReturn(currentDocRef);

        EntityReferenceSerializer<String> serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        Mockito.when(serializer.serialize(currentDocRef)).thenReturn("foo");

        // We can't verify the meta data after the display method is called because we want to make sure the BASE meta
        // data is correctly set before XDOM transformations are executed, not after.
        TransformationManager mockTransformationManager = mocker.getInstance(TransformationManager.class);
        Mockito.doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XDOM xdom = (XDOM) invocation.getArguments()[0];
                // We have to assert the meta data before the transformations are executed not at the end!
                Assert.assertEquals("foo", xdom.getMetaData().getMetaData(MetaData.BASE));
                return null;
            }
        }).when(mockTransformationManager)
            .performTransformations(Mockito.any(XDOM.class), Mockito.any(TransformationContext.class));

        // Execute the display.
        Assert.assertSame(content,
            mocker.getComponentUnderTest().display(mockDocument, new DocumentDisplayerParameters()));

        // Make sure the transformations are executed exactly once, and on the right content.
        Mockito.verify(mockTransformationManager, Mockito.times(1)).performTransformations(Mockito.same(content),
            Mockito.any(TransformationContext.class));
    }
}
