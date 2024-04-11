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

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentContentDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ DefaultDocumentContentAsyncParser.class })
public class DocumentContentDisplayerTest
{
    @InjectMockComponents
    private DocumentContentAsyncExecutor documentExecutor;

    @InjectMockComponents
    private DocumentContentAsyncRenderer documentRenderer;

    @InjectMockComponents
    private DocumentContentDisplayer documentDisplayer;

    @MockComponent
    private BlockAsyncRendererExecutor executor;

    @MockComponent
    private Execution execution;

    @MockComponent
    private DocumentModelBridge document;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private TransformationManager transformationManager;

    @Test
    public void baseMetaDataIsSetBeforeExecutingTransformations() throws Exception
    {
        when(this.executor.execute(any(), any())).then(new Answer<Block>()
        {
            @Override
            public Block answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.<DocumentContentAsyncRenderer>getArgument(0).render(false, false).getBlock();
            }
        });

        // The execution context is expected to have the "xwikicontext" property set.
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", new HashMap<String, Object>());
        when(this.execution.getContext()).thenReturn(executionContext);

        // The document being displayed.
        XDOM content = new XDOM(Collections.emptyList());
        when(this.document.getPreparedXDOM()).thenReturn(content);

        // The reference of the current document musts be set as the value of the BASE meta data.
        DocumentReference currentDocRef = new DocumentReference("wiki", "Space", "Page");
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(currentDocRef);

        when(this.serializer.serialize(currentDocRef)).thenReturn("foo");

        // We can't verify the meta data after the display method is called because we want to make sure the BASE meta
        // data is correctly set before XDOM transformations are executed, not after.
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation)
            {
                XDOM xdom = (XDOM) invocation.getArguments()[0];
                // We have to assert the meta data before the transformations are executed, not at the end!
                assertEquals("foo", xdom.getMetaData().getMetaData(MetaData.BASE));
                return null;
            }
        }).when(this.transformationManager).performTransformations(any(XDOM.class), any(TransformationContext.class));

        // Note: we use a non-isolated tx context simply to simplify the test and avoid having to setup a
        // VelocityManager for the test.
        DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();
        parameters.setTransformationContextIsolated(false);

        // Execute the display.
        assertSame(content, this.documentDisplayer.display(document, parameters));

        // Make sure the transformations are executed exactly once, and on the right content.
        verify(this.transformationManager, times(1)).performTransformations(same(content),
            any(TransformationContext.class));
    }
}
