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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiCommentsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CommentSaveAction}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(XWikiCommentsDocumentInitializer.class)
class CommentSaveActionTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    private static final String COMMENT_PARAMETER = "XWiki.XWikiComments_0_comment";

    private static final String COMMENT = "comment";

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private CommentSaveAction commentSaveAction;

    @Mock
    private XWikiRequest request;

    @BeforeEach
    void beforeEach()
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        xcontext.setRequest(this.request);
        xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "User"));
        when(this.csrfToken.isTokenValid(any())).thenReturn(true);
        // Render the response with a template instead of redirecting.
        when(this.request.get("xpage")).thenReturn("plain");
        when(this.request.getParameterNames()).thenReturn(Collections.enumeration(List.of(COMMENT_PARAMETER)));
        when(this.request.getParameter(COMMENT_PARAMETER)).thenReturn("edited comment");
        when(this.localizationManager.getTranslationPlain("core.comment.editComment")).thenReturn("Edited comment");
    }

    @Test
    void editCommentAfterConcurrentSave() throws Exception
    {
        this.oldcore.checkDocumentRevision(true);
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        BaseObject commentObject = document.newXObject(XWikiDocument.COMMENTSCLASS_REFERENCE, xcontext);
        commentObject.setStringValue("author", "xwiki:XWiki.User");
        commentObject.setLargeStringValue(COMMENT, "comment");
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);
        xcontext.setDoc(this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext));

        // Another request adds a comment after this request loaded the document.
        XWikiDocument concurrentDocument =
            this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext).clone();
        concurrentDocument.newXObject(XWikiDocument.COMMENTSCLASS_REFERENCE, xcontext)
            .setLargeStringValue(COMMENT, "other comment");
        this.oldcore.getSpyXWiki().saveDocument(concurrentDocument, xcontext);

        assertTrue(this.commentSaveAction.action(xcontext));

        // The comment is edited in the version saved by the other request, which keeps the other comment.
        XWikiDocument storedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext);
        assertEquals("edited comment",
            storedDocument.getXObject(XWikiDocument.COMMENTSCLASS_REFERENCE, 0).getLargeStringValue(COMMENT));
        assertEquals("other comment",
            storedDocument.getXObject(XWikiDocument.COMMENTSCLASS_REFERENCE, 1).getLargeStringValue(COMMENT));
        assertEquals(storedDocument.getVersion(), xcontext.getDoc().getVersion());
    }
}
