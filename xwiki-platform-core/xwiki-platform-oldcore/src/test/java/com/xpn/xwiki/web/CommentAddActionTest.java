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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CommentAddAction}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class CommentAddActionTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private CommentAddAction commentAddAction;

    @Mock
    private XWikiRequest request;

    @BeforeEach
    void beforeEach() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        xcontext.setRequest(this.request);
        xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "User"));
        when(this.csrfToken.isTokenValid(any())).thenReturn(true);
        when(this.localizationManager.getTranslationPlain("core.comment.addComment")).thenReturn("Added comment");
        // Render the response with a template instead of redirecting.
        when(this.request.get("xpage")).thenReturn("plain");

        ObjectAddForm form = mock();
        when(form.getObject("XWiki.XWikiComments")).thenReturn(Map.of("comment", new String[] { "new comment" }));
        xcontext.setForm(form);

        BaseClass commentsClass = new BaseClass();
        commentsClass.setDocumentReference(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
        commentsClass.addTextField("author", "Author", 30);
        commentsClass.addTextAreaField("comment", "Comment", 40, 5);
        doReturn(commentsClass).when(this.oldcore.getSpyXWiki()).getCommentsClass(any());
    }

    @Test
    void addCommentAfterConcurrentSave() throws Exception
    {
        this.oldcore.checkDocumentRevision(true);
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(DOCUMENT_REFERENCE), xcontext);
        xcontext.setDoc(this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext));

        // Another request saves the document after this request loaded it.
        XWikiDocument concurrentDocument =
            this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext).clone();
        concurrentDocument.setTitle("concurrent title");
        this.oldcore.getSpyXWiki().saveDocument(concurrentDocument, xcontext);

        assertTrue(this.commentAddAction.action(xcontext));

        // The comment is added to the version saved by the other request, which is the one shown in the response.
        XWikiDocument storedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext);
        assertEquals("concurrent title", storedDocument.getTitle());
        BaseObject commentObject = storedDocument.getXObject(XWikiDocument.COMMENTSCLASS_REFERENCE);
        assertEquals("new comment", commentObject.getLargeStringValue("comment"));
        assertEquals(storedDocument.getVersion(), xcontext.getDoc().getVersion());
    }
}
