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
package org.xwiki.rendering.internal.transformation;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Unit tests for {@link DefaultTransformationExecutor}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultTransformationExecutorTest
{
    @InjectMockComponents
    private DefaultTransformationExecutor defaultTransformationExecutor;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private TransformationManager transformationManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Captor
    private ArgumentCaptor<TransformationContext> transformationContextCaptor;

    private DocumentReference currentUserReference = new DocumentReference("xwiki", "XWiki", "CurrentUser");

    private DocumentReference contentDocumentReference = new DocumentReference("xwiki", "Test", "ContentDocument");

    @BeforeEach
    void beforeEach()
    {
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(this.currentUserReference);
    }

    @Test
    @SuppressWarnings("null")
    void performTransformations() throws Exception
    {
        doAnswer(invocation -> {
            return invocation.getArgument(0, Callable.class).call();
        }).when(this.authorExecutor).call(any(), eq(this.currentUserReference), eq(this.contentDocumentReference));

        XDOM xdom = new XDOM(List.of(new WordBlock("content")));
        this.defaultTransformationExecutor.withId("test").withXDOM(xdom).withSyntax(XWIKI_2_1)
            .withTargetSyntax(PLAIN_1_0).withRestricted(true).withTransformations(List.of("icon", "macro"))
            .withContentDocument(contentDocumentReference).execute();

        verify(this.transformationManager).performTransformations(eq(xdom), this.transformationContextCaptor.capture());

        TransformationContext transformationContext = this.transformationContextCaptor.getValue();
        assertEquals("test", transformationContext.getId());
        assertEquals(xdom, transformationContext.getXDOM());
        assertEquals(XWIKI_2_1, transformationContext.getSyntax());
        assertEquals(PLAIN_1_0, transformationContext.getTargetSyntax());
        assertTrue(transformationContext.isRestricted());
        assertEquals(List.of("icon", "macro"), transformationContext.getTransformationNames());
    }
}
