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
package org.xwiki.rest.internal.resources.comments;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.DocumentRevisionConflictException;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CommentsResourceImpl}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class CommentsResourceImplTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "space", "page");

    private static final DocumentReference COMMENTS_CLASS = new DocumentReference("xwiki", "XWiki", "XWikiComments");

    private static final String COMMENT = "comment";

    @InjectMockComponents
    private CommentsResourceImpl commentsResource;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Mock
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.uriInfo.getBaseUri()).thenReturn(new URI("https://test/"));
        FieldUtils.writeField(this.commentsResource, "uriInfo", this.uriInfo, true);

        XWikiContext xcontext = this.oldcore.getXWikiContext();
        xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "User"));
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(Right.class), any())).thenReturn(true);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(Right.class), any(), any())).thenReturn(true);
        // Document#hasAccessLevel, used by the resource, goes through the right service.
        when(this.oldcore.getMockRightService().hasAccessLevel(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);

        XWikiDocument commentsClassDocument = new XWikiDocument(COMMENTS_CLASS);
        BaseClass commentsClass = commentsClassDocument.getXClass();
        commentsClass.addTextField("author", "Author", 30);
        commentsClass.addDateField("date", "Date");
        commentsClass.addTextAreaField("highlight", "Highlight", 40, 5);
        commentsClass.addTextAreaField(COMMENT, "Comment", 40, 5);
        commentsClass.addNumberField("replyto", "Reply To", 5, "integer");
        this.oldcore.getSpyXWiki().saveDocument(commentsClassDocument, xcontext);
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(DOCUMENT_REFERENCE), xcontext);
        this.oldcore.checkDocumentRevision(true);
    }

    @Test
    void postComment() throws Exception
    {
        Comment comment = new Comment();
        comment.setText("text");

        Response response = this.commentsResource.postComment("xwiki", "space", "page", comment);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(0, ((Comment) response.getEntity()).getId());
        assertEquals(List.of("text"), getStoredComments());
    }

    @Test
    void postCommentAfterConcurrentSave() throws Exception
    {
        // Another request adds a comment between the moment the document is loaded and the moment it's saved.
        commentConcurrently(1);

        Comment comment = new Comment();
        comment.setText("text");

        Response response = this.commentsResource.postComment("xwiki", "space", "page", comment);

        // Both comments are kept, and the response describes the comment that was actually saved.
        assertEquals(List.of("concurrent text 1", "text"), getStoredComments());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        Comment createdComment = (Comment) response.getEntity();
        assertEquals(1, createdComment.getId());
        assertEquals("text", createdComment.getText());
        assertEquals(new URI("https://test/wikis/xwiki/spaces/space/pages/page/comments/1"), response.getLocation());
    }

    @Test
    void postCommentAlwaysAfterConcurrentSave() throws Exception
    {
        AtomicInteger concurrentComments = commentConcurrently(Integer.MAX_VALUE);

        Comment comment = new Comment();
        comment.setText("text");

        XWikiRestException exception = assertThrows(XWikiRestException.class,
            () -> this.commentsResource.postComment("xwiki", "space", "page", comment));

        assertInstanceOf(DocumentRevisionConflictException.class, exception.getCause());
        assertEquals(5, concurrentComments.get());
    }

    /**
     * Make another request add a comment while the resource checks that it can save the document, the given number
     * of times.
     *
     * @return the number of comments added by the other request
     */
    private AtomicInteger commentConcurrently(int times) throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        AtomicInteger concurrentComments = new AtomicInteger();
        doAnswer(invocation -> {
            if (concurrentComments.get() < times) {
                XWikiDocument concurrentDocument =
                    this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext).clone();
                BaseObject concurrentComment = concurrentDocument.newXObject(COMMENTS_CLASS, xcontext);
                concurrentComment.set(COMMENT, "concurrent text " + concurrentComments.incrementAndGet(), xcontext);
                this.oldcore.getSpyXWiki().saveDocument(concurrentDocument, xcontext);
            }
            return null;
        }).when(this.oldcore.getSpyXWiki()).checkSavingDocument(any(DocumentReference.class), any(XWikiDocument.class),
            anyString(), anyBoolean(), any(XWikiContext.class));
        return concurrentComments;
    }

    private List<String> getStoredComments() throws Exception
    {
        XWikiDocument storedDocument =
            this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        return storedDocument.getXObjects(COMMENTS_CLASS).stream()
            .map(commentObject -> commentObject.getStringValue(COMMENT)).toList();
    }
}
