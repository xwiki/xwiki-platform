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
package org.xwiki.icon.internal;

import java.io.Writer;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.icon.IconException;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.internal.document.DefaultDocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.VelocityRenderer}.
 *
 * @since 6.4M1
 * @version $Id$
 */
@ComponentTest
class VelocityRendererTest
{
    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private DocumentContextExecutor documentContextExecutor;

    @InjectMockComponents
    private VelocityRenderer velocityRenderer;

    @Test
    void renderTest() throws Exception
    {
        // Mocks
        VelocityEngine engine = mock(VelocityEngine.class);
        when(this.velocityManager.getVelocityEngine()).thenReturn(engine);
        when(engine.evaluate(any(VelocityContext.class), any(Writer.class), any(), eq("myCode"))).thenAnswer(
            invocation -> {
                // Get the writer
                Writer writer = (Writer) invocation.getArguments()[1];
                writer.write("Rendered code");
                return true;
            });

        // Test
        assertEquals("Rendered code", this.velocityRenderer.render("myCode", null));

        // Verify
        verify(engine).startedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
        verify(engine).stoppedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
    }

    @Test
    void renderWithException() throws Exception
    {
        //  Mocks
        Exception exception = new XWikiVelocityException("exception");
        when(this.velocityManager.getVelocityEngine()).thenThrow(exception);

        // Test
        IconException caughtException = null;
        try {
            this.velocityRenderer.render("myCode", null);
        } catch (IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to render the icon.", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

    @Test
    void renderWhenEvaluateReturnsFalse() throws Exception
    {
        //  Mocks
        VelocityEngine engine = mock(VelocityEngine.class);
        when(this.velocityManager.getVelocityEngine()).thenReturn(engine);
        when(engine.evaluate(any(VelocityContext.class), any(Writer.class), any(),
            eq("myCode"))).thenReturn(false);

        // Test
        IconException caughtException = assertThrows(IconException.class,
            () -> this.velocityRenderer.render("myCode", null));

        // Verify
        assertEquals("Failed to render the icon. See the Velocity runtime log.",
            caughtException.getMessage());

        verify(engine).startedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
        verify(engine).stoppedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
    }

    @Test
    void renderWithContextDocument() throws Exception
    {
        // Mocks
        VelocityEngine engine = mock(VelocityEngine.class);
        when(this.velocityManager.getVelocityEngine()).thenReturn(engine);
        when(engine.evaluate(any(VelocityContext.class), any(Writer.class), any(), eq("myCode"))).thenAnswer(
            invocation -> {
                // Get the writer
                Writer writer = (Writer) invocation.getArguments()[1];
                writer.write("Rendered code");
                return true;
            });

        DocumentReference contextReference = new DocumentReference("xwiki", "Space", "IconTheme");
        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "User");
        XWikiDocument document = mock(XWikiDocument.class);
        UserReference authorReference = mock(UserReference.class);
        DocumentAuthors documentAuthors = new DefaultDocumentAuthors(document);
        documentAuthors.setContentAuthor(authorReference);
        when(document.getAuthors()).thenReturn(documentAuthors);
        when(this.documentUserSerializer.serialize(authorReference)).thenReturn(documentAuthorReference);
        when(this.documentAccessBridge.getDocumentInstance(contextReference)).thenReturn(document);
        when(this.authorExecutor.call(any(), eq(documentAuthorReference), eq(contextReference)))
            .then(invocation -> invocation.getArgument(0, Callable.class).call());
        when(this.documentContextExecutor.call(any(), eq(document)))
            .then(invocation -> invocation.getArgument(0, Callable.class).call());

        assertEquals("Rendered code", this.velocityRenderer.render("myCode", contextReference));
    }
}
