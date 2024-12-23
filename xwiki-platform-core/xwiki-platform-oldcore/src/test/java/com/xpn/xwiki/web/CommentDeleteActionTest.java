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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.web.CommentDeleteAction}.
 *
 * @version $Id$
 * @since 17.0.0RC1
 */
@ComponentList
@ReferenceComponentList
@OldcoreTest(mockXWiki = false)
class CommentDeleteActionTest
{
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
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

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

    @Mock
    private DocumentAuthors mockAuthors;

    @Mock
    private UserReference currentUserReference;

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

        when(this.mockClonedDocument.getXObject(any(), eq(0))).thenReturn(this.mockComment);
        when(this.mockClonedDocument.getAuthors()).thenReturn(mockAuthors);
        // Those are necessary for the call to checkSavingDocument made while deleting the comment.
        DocumentReference documentReference = new DocumentReference("XWiki", "Foo", "Bar",
            Locale.ENGLISH);
        when(mockClonedDocument.getDocumentReference()).thenReturn(documentReference);
        when(mockClonedDocument.getDocumentReferenceWithLocale()).thenReturn(documentReference);

        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.currentUserReference);
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
        // First, we check that the request has returned the right result.
        assertFalse(this.commentDeleteAction.action(this.context));
        // Then, we check that we did take CSRF validation into consideration
        verify(this.csrfToken).isTokenValid(null);
        // Then, we make sure that the comment provided was actually removed
        verify(this.mockClonedDocument).removeXObject(this.mockComment);
        // And that the document where it stood was saved.
        verify(context.getWiki()).saveDocument(mockClonedDocument, "changeComment", true, context);
    }
}
