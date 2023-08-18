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
package com.xpn.xwiki.objects.classes;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.render.OldRendering;
import com.xpn.xwiki.internal.security.authorization.DefaultAuthorExecutor;
import com.xpn.xwiki.internal.velocity.VelocityEvaluator;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link TextAreaClass} class.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList({ DefaultAuthorExecutor.class })
class TextAreaClassTest
{
    private static final String PROPERTY_NAME = "test";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private Provider<OldRendering> oldRenderingProvider;

    @MockComponent
    private VelocityEvaluator velocityEvaluator;

    @RegisterExtension
    private final LogCaptureExtension logCaptureExtension = new LogCaptureExtension();

    @Test
    void viewWikiText()
    {
        // Use a spy, so we don't need to mess around with clone-support in a mock.
        XWikiDocument spyDocument = getSpyDocument();

        TextAreaClass textAreaClass = new TextAreaClass();
        textAreaClass.setContentType(TextAreaClass.ContentType.WIKI_TEXT);
        BaseObject object = new BaseObject();
        object.setOwnerDocument(spyDocument);
        object.setLargeStringValue(PROPERTY_NAME, "**Test bold**");
        StringBuffer buffer = new StringBuffer();
        String renderingResult = "<p><strong>Test bold</strong></p>";
        doAnswer(invocationOnMock -> {
            XWikiDocument sDoc = invocationOnMock.getArgument(3);
            // Verify that the content author is set to the metadata author.
            assertEquals(GuestUserReference.INSTANCE, sDoc.getAuthors().getContentAuthor());
            return renderingResult;
        }).when(spyDocument).getRenderedContent(anyString(), any(Syntax.class), anyBoolean(), any(XWikiDocument.class),
            anyBoolean(), any(XWikiContext.class));
        textAreaClass.displayView(buffer, PROPERTY_NAME, "", object, true, this.oldcore.getXWikiContext());

        verify(spyDocument).getRenderedContent(anyString(), any(Syntax.class), anyBoolean(),
            any(XWikiDocument.class), anyBoolean(), any(XWikiContext.class));

        assertEquals(renderingResult, buffer.toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void viewVelocityCode(boolean allowExecution)
    {
        XWikiDocument spyDocument = getSpyDocument();
        // Velocity code is only supported in XWiki 1.0.
        spyDocument.setSyntax(Syntax.XWIKI_1_0);

        TextAreaClass textAreaClass = new TextAreaClass();
        textAreaClass.setContentType(TextAreaClass.ContentType.VELOCITY_CODE);

        BaseObject object = new BaseObject();
        object.setOwnerDocument(spyDocument);
        String velocityCode = "#set($x = 1) $1 & 1";
        object.setLargeStringValue(PROPERTY_NAME, velocityCode);
        StringBuffer buffer = new StringBuffer();

        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.SCRIPT)).then(invocationOnMock -> {
            // Verify that the content author is set to the metadata author.
            XWikiDocument sDoc = (XWikiDocument) this.oldcore.getXWikiContext().get(XWikiDocument.CKEY_SDOC);
            assertEquals(GuestUserReference.INSTANCE, sDoc.getAuthors().getContentAuthor());
            return allowExecution;
        });

        String renderingResult = "1 &#38; 1";
        OldRendering oldRendering = mock();
        String renderingInput = velocityCode.replace("&", "&#38;");
        when(oldRendering.parseContent(renderingInput, this.oldcore.getXWikiContext()))
            .thenReturn(renderingResult);
        when(this.oldRenderingProvider.get()).thenReturn(oldRendering);

        textAreaClass.displayView(buffer, PROPERTY_NAME, "", object, true, this.oldcore.getXWikiContext());

        if (allowExecution) {
            assertEquals(renderingResult, buffer.toString());
        } else {
            assertEquals(renderingInput, buffer.toString());
            verify(oldRendering, never()).parseContent(anyString(), any());
        }

        verify(this.oldcore.getMockContextualAuthorizationManager()).hasAccess(Right.SCRIPT);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void viewVelocityWiki(boolean allowExecution)
    {
        XWikiDocument spyDocument = getSpyDocument();

        TextAreaClass textAreaClass = new TextAreaClass();
        textAreaClass.setContentType(TextAreaClass.ContentType.VELOCITYWIKI);

        BaseObject object = new BaseObject();
        object.setOwnerDocument(spyDocument);
        String velocityCode = "#set($x = \"bold\") **$1**";
        object.setLargeStringValue(PROPERTY_NAME, velocityCode);
        StringBuffer buffer = new StringBuffer();

        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.SCRIPT)).then(invocationOnMock -> {
            // Verify that the content author is set to the metadata author.
            XWikiDocument sDoc = (XWikiDocument) this.oldcore.getXWikiContext().get(XWikiDocument.CKEY_SDOC);
            assertEquals(GuestUserReference.INSTANCE, sDoc.getAuthors().getContentAuthor());
            return allowExecution;
        });

        String velocityResult = "**bold**";
        when(this.velocityEvaluator.evaluateVelocityNoException(velocityCode, spyDocument.getDocumentReference()))
            .thenReturn(velocityResult);

        String renderingResult = "<p><strong>bold</strong></p>";

        // Mock the rendering of the result of the Velocity code. Check that the content author is set to the
        // metadata author.
        doAnswer(invocationOnMock -> {
            // Verify that the input is as expected.
            if (allowExecution) {
                assertEquals(velocityResult, invocationOnMock.getArgument(0));
            } else {
                assertEquals(velocityCode, invocationOnMock.getArgument(0));
            }

            XWikiDocument sDoc = invocationOnMock.getArgument(3);
            assertEquals(GuestUserReference.INSTANCE, sDoc.getAuthors().getContentAuthor());

            return renderingResult;
        }).when(spyDocument).getRenderedContent(anyString(), same(Syntax.XWIKI_2_1), anyBoolean(),
            any(XWikiDocument.class), anyBoolean(), any(XWikiContext.class));

        textAreaClass.displayView(buffer, PROPERTY_NAME, "", object, true, this.oldcore.getXWikiContext());

        assertEquals(renderingResult, buffer.toString());

        // Verify that script right was actually checked.
        verify(this.oldcore.getMockContextualAuthorizationManager()).hasAccess(Right.SCRIPT);
    }

    @Test
    void viewVelocityWikiWithoutOwnerDocument()
    {
        TextAreaClass textAreaClass = new TextAreaClass();
        textAreaClass.setContentType(TextAreaClass.ContentType.VELOCITYWIKI);

        BaseObject object = new BaseObject();
        String velocityCode = "1 & 2";
        object.setLargeStringValue(PROPERTY_NAME, velocityCode);
        StringBuffer buffer = new StringBuffer();

        textAreaClass.displayView(buffer, PROPERTY_NAME, "", object, true, this.oldcore.getXWikiContext());

        assertEquals("1 &#38; 2", buffer.toString());

        verify(this.oldcore.getMockContextualAuthorizationManager(), never()).hasAccess(Right.SCRIPT);

        assertEquals(1, this.logCaptureExtension.size());
        assertTrue(this.logCaptureExtension.getMessage(0).matches("\\QError while getting the syntax corresponding to "
            + "object [null]. Defaulting to using XWiki 1.0 syntax. Internal error [NullPointerException:\\E.*]"));
    }

    private XWikiDocument getSpyDocument()
    {
        // Use a spy, so we don't need to mess around with clone-support in a mock.
        XWikiDocument spyDocument = spy(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
        spyDocument.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getXWikiContext().setDoc(spyDocument);
        // Add some authors to the document to verify that TextAreaClass is correctly setting the content author to
        // the metadata author.
        DocumentAuthors authors = spyDocument.getAuthors();
        authors.setEffectiveMetadataAuthor(GuestUserReference.INSTANCE);
        authors.setContentAuthor(SuperAdminUserReference.INSTANCE);
        return spyDocument;
    }
}
