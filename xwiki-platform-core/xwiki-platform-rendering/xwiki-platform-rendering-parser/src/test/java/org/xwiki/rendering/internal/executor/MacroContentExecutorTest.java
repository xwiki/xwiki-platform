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
package org.xwiki.rendering.internal.executor;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.executor.ContentExecutorException;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link MacroContentExecutor}.
 *
 * @version $Id$
 * @since 8.4RC1
 */
public class MacroContentExecutorTest
{
    @Rule
    public final MockitoComponentMockingRule<MacroContentExecutor> mocker =
        new MockitoComponentMockingRule<>(MacroContentExecutor.class);

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @Test
    public void executeWithNoSource() throws Exception
    {
        XDOM parsedBlocks = new XDOM(Collections.emptyList());
        ContentParser contentParser = this.mocker.getInstance(ContentParser.class);
        when(contentParser.parse("", Syntax.PLAIN_1_0)).thenReturn(parsedBlocks);

        TransformationContext transformationContext = new TransformationContext();
        MacroTransformationContext context = new MacroTransformationContext(transformationContext);

        this.mocker.getComponentUnderTest().execute("", Syntax.PLAIN_1_0, context);

        // The test is here: Verify that the Macro Transformation has been called
        Transformation macroTransformation = this.mocker.getInstance(Transformation.class, "macro");
        verify(macroTransformation).transform(parsedBlocks, transformationContext);
    }

    @Test
    public void executeWithSource() throws Exception
    {
        XDOM parsedBlocks = new XDOM(Collections.emptyList());
        ContentParser contentParser = this.mocker.getInstance(ContentParser.class);
        when(contentParser.parse("", Syntax.PLAIN_1_0, DOCUMENT_REFERENCE)).thenReturn(parsedBlocks);

        TransformationContext transformationContext = new TransformationContext();
        MacroTransformationContext context = new MacroTransformationContext(transformationContext);

        this.mocker.getComponentUnderTest().execute("", Syntax.PLAIN_1_0, DOCUMENT_REFERENCE, context);

        // The test is here: Verify that the Macro Transformation has been called
        Transformation macroTransformation = this.mocker.getInstance(Transformation.class, "macro");
        verify(macroTransformation).transform(parsedBlocks, transformationContext);
    }

    @Test
    public void executeWhenTransformationException() throws Exception
    {
        XDOM parsedBlocks = new XDOM(Collections.emptyList());
        ContentParser contentParser = this.mocker.getInstance(ContentParser.class);
        when(contentParser.parse("", Syntax.PLAIN_1_0)).thenReturn(parsedBlocks);

        TransformationContext transformationContext = new TransformationContext();
        MacroTransformationContext context = new MacroTransformationContext(transformationContext);

        Transformation macroTransformation = this.mocker.getInstance(Transformation.class, "macro");
        doThrow(new TransformationException("error"))
            .when(macroTransformation).transform(parsedBlocks, transformationContext);

        try {
            this.mocker.getComponentUnderTest().execute("", Syntax.PLAIN_1_0, context);
            fail("Should have raised a ContentExecutorException");
        } catch (ContentExecutorException expected) {
            assertEquals("Failed to execute content", expected.getMessage());
        }
    }
}