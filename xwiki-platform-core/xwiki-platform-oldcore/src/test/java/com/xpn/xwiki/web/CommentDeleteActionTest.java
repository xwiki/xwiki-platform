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
package com.xpn.xwiki.web;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.web.CommentDeleteAction}.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@ReferenceComponentList
@OldcoreTest()
class CommentDeleteActionTest
{
    private static final String COMMENT_AUTHOR = "XWiki.Author";

    private static final DocumentReference COMMENT_AUTHOR_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "Author");

    private static final DocumentReference OTHER_USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "Other");

    /**
     * The object being tested.
     */
    @InjectMockComponents
    private final CommentDeleteAction commentDeleteAction = new CommentDeleteAction();

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private StoreConfiguration storeConfiguration;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiDocument mockDocument;

    @Mock
    private XWikiDocument mockClonedDocument;

    @Mock
    private XWikiDocument mockClonedTwiceDocument;

    @Mock
    private ObjectRemoveForm mockForm;

    @Mock
    private BaseObject mockComment;

    XWikiContext context;

    @BeforeEach
    void setup()
    {

        this.context = this.oldcore.getXWikiContext();
        this.context.setDoc(this.mockDocument);

        when(this.mockDocument.clone()).thenReturn(this.mockClonedDocument);
        when(this.mockClonedDocument.clone()).thenReturn(this.mockClonedTwiceDocument);
        when(this.mockClonedDocument.getOriginalDocument()).thenReturn(this.mockDocument);

        this.context.setForm(this.mockForm);
        when(this.mockForm.getClassName()).thenReturn("XWikiComments");
        when(this.mockForm.getClassId()).thenReturn(0);

        DocumentReference commentClassReference = new DocumentReference("XWiki", "XWiki", "XWikiComments");
        when(this.mockClonedDocument.resolveClassReference("XWikiComments")).thenReturn(commentClassReference);
        when(this.mockClonedDocument.getXObject(commentClassReference, 0)).thenReturn(this.mockComment);
        when(this.mockComment.getStringValue("author")).thenReturn(COMMENT_AUTHOR);
        // Those are necessary for the call to checkSavingDocument made while deleting the comment.
        DocumentReference documentReference = new DocumentReference("XWiki", "Foo", "Bar",
            Locale.ENGLISH);
        when(mockClonedDocument.getDocumentReference()).thenReturn(documentReference);
        when(mockClonedDocument.getDocumentReferenceWithLocale()).thenReturn(documentReference);

        when(commentDeleteAction.localizePlainOrReturnKey("core.comment.deleteComment"))
            .thenReturn("changeComment");

        this.context.setRequest(this.request);
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);
    }

    /**
     * Deletes a comment.
     */
    @Test
    void deleteComment() throws Exception
    {
        this.context.setUserReference(COMMENT_AUTHOR_REFERENCE);

        // First, we check that the request has returned the right result.
        assertFalse(this.commentDeleteAction.action(this.context));
        // Then, we check that we did take CSRF validation into consideration
        verify(this.csrfToken).isTokenValid(null);
        // Then, we make sure that the comment provided was actually removed
        verify(this.mockClonedDocument).removeXObject(this.mockComment);
        verify(this.mockClonedDocument).setAuthorReference(COMMENT_AUTHOR_REFERENCE);
        // And that the document where it stood was saved.
        verify(context.getWiki()).saveDocument(this.mockClonedDocument, "changeComment", true, true, this.context);
    }

    @Test
    void rejectNonCommentClass() throws Exception
    {
        when(this.mockForm.getClassName()).thenReturn("XWiki.XWikiUsers");
        when(this.mockClonedDocument.resolveClassReference("XWiki.XWikiUsers"))
            .thenReturn(new DocumentReference("XWiki", "XWiki", "XWikiUsers"));

        assertTrue(this.commentDeleteAction.action(this.context));

        assertEquals("platform.core.action.commentRemove.invalidClass",
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
                .getCurrentScriptContext().getAttribute("message"));
        verify(this.mockClonedDocument, never()).removeXObject(any(BaseObject.class));
        verify(this.context.getWiki(), never()).saveDocument(any(XWikiDocument.class), anyString(), anyBoolean(),
            anyBoolean(), any(XWikiContext.class));
    }

    @Test
    void deleteCommentOfAnotherUserWithoutAdminRight() throws Exception
    {
        this.context.setUserReference(OTHER_USER_REFERENCE);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(eq(Right.ADMIN), eq(OTHER_USER_REFERENCE), any()))
            .thenReturn(false);

        // The action returns true so that the error template is rendered.
        assertTrue(this.commentDeleteAction.action(this.context));
        assertEquals("platform.core.action.commentRemove.notAllowed",
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
                .getCurrentScriptContext().getAttribute("message"));
        verify(this.mockClonedDocument, never()).removeXObject(any());
        verify(this.context.getWiki(), never()).saveDocument(any(), any(), eq(true), eq(true), any());
    }
}
