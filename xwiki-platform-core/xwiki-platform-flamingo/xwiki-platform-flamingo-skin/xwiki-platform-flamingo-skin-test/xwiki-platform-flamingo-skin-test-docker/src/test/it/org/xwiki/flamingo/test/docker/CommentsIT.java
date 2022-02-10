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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NotFoundException;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentForm;
import org.xwiki.test.ui.po.CommentsTab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the comments feature.
 *
 * @version $Id$
 * @since 14.1RC1
 * @since 14.0
 * @since 13.10.3
 */
@UITest
class CommentsIT
{
    private static final String COMMENT_CONTENT = "Some content";

    private static final String COMMENT_AUTHOR_GUEST = "Anonymous";

    private static final String COMMENT_REPLY = "Comment Reply";

    private static final String USER_NAME = "commentUser";

    private static final String USER_PASSWORD = "commentUserPassword";

    private static final String COMMENT_REPLACED_CONTENT = "Some replaced content";

    @Test
    @Order(1)
    void commentAsGuest(TestUtils setup, TestReference reference)
    {
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAllGroup", "", "comment", true);
        setup.deletePage(reference);
        setup.setRights(reference, null, "XWiki.XWikiGuest", "comment", true);
        setup.forceGuestUser();

        CommentsTab commentsTab = setup.createPage(reference, "").openCommentsDocExtraPane();
        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR_GUEST, true);
        assertEquals(COMMENT_CONTENT, commentsTab.getCommentContentByID(0));
        assertEquals(COMMENT_AUTHOR_GUEST, commentsTab.getCommentAuthorByID(0));

        // Reply as anonymous.
        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR_GUEST, true);
        commentsTab.replyToCommentByID(commentsTab.getCommentID(COMMENT_CONTENT), COMMENT_REPLY);
        assertEquals(COMMENT_REPLY, commentsTab.getCommentContentByID(commentsTab.getCommentID(COMMENT_REPLY)));
        assertEquals(COMMENT_AUTHOR_GUEST, commentsTab.getCommentAuthorByID(commentsTab.getCommentID(COMMENT_REPLY)));
        
        // Cannot edit comments.
        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR_GUEST, true);
        assertFalse(commentsTab.hasEditButtonForCommentByID(commentsTab.getCommentID(COMMENT_CONTENT)));
    }

    @Test
    @Order(2)
    void commentAsLoggedInUser(TestUtils setup, TestReference reference)
    {
        setup.deletePage(reference);
        setup.createUserAndLogin(USER_NAME, USER_PASSWORD);
        CommentsTab commentsTab = setup.createPage(reference, "").openCommentsDocExtraPane();
        commentsTab.openCommentForm();
        assertTrue(commentsTab.isCommentFormShown());
        int commentIndex = commentsTab.postComment(COMMENT_CONTENT, true);
        assertEquals(COMMENT_CONTENT, commentsTab.getCommentContentByID(commentIndex));
        assertEquals(USER_NAME, commentsTab.getCommentAuthorByID(commentIndex));

        // Reply to comment.
        commentsTab.openCommentForm();
        commentIndex = commentsTab.postComment(COMMENT_CONTENT, true);
        commentsTab.replyToCommentByID(commentIndex, COMMENT_REPLY);
        int replyIndex = commentsTab.getCommentID(COMMENT_REPLY);
        assertEquals(USER_NAME, commentsTab.getCommentAuthorByID(replyIndex));
       
        // Delete comment.
        commentsTab.openCommentForm();
        assertTrue(commentsTab.isCommentFormShown());
        String toDeleteComment = "delete me";
        int commentId = commentsTab.postComment(toDeleteComment, true);
        commentsTab.deleteCommentByID(commentId);
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            commentsTab.getCommentID(toDeleteComment);
        });
        String exceptionMessage = notFoundException.getMessage();
        String expectedMessage = String.format("Comment with content [%s] cannot be found.", toDeleteComment);
        assertTrue(exceptionMessage.startsWith(expectedMessage),
            () -> String.format("Expected an exception message starting by [%s], got [%s] instead.", expectedMessage,
                exceptionMessage));

        // Edit comment.
        commentsTab.openCommentForm();
        assertTrue(commentsTab.isCommentFormShown());
        commentIndex = commentsTab.postComment(COMMENT_CONTENT, true);
        commentsTab.editCommentByID(commentIndex, COMMENT_REPLACED_CONTENT);
        assertEquals(COMMENT_REPLACED_CONTENT, commentsTab.getCommentContentByID(commentIndex));
        
        // Open then cancel the edit comment forms
        commentsTab.openCommentForm();
        commentsTab.cancelCommentForm();
        commentsTab.editCommentByID(commentIndex);
        commentsTab.cancelCommentForm();
        commentsTab.clickOnReplyToCommentByID(commentIndex);
        commentsTab.cancelCommentForm();
    }

    @Test
    @Order(3)
    void commentAsAdmin(TestUtils setup, TestReference reference)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(reference);

        // Preview comment.
        setup.createAdminUser();
        CommentsTab commentsTab = setup.createPage(reference, "").openCommentsDocExtraPane();
        CommentForm addCommentForm = commentsTab.getAddCommentForm();
        addCommentForm.addToContentField("one **two** three");
        assertEquals("one two three", addCommentForm.clickPreview().getText());
        addCommentForm.clickBack();
        addCommentForm.addToContentField(" //four//");
        addCommentForm.clickPreview();
        addCommentForm.clickSubmit();
        assertTrue(commentsTab.getCommentID("one two three four") >= 0);
        
        setup.loginAsAdmin();
        // We know XWiki.XWikiAdminGroup has a sheet applied and is included in the minimal distribution.
        commentsTab = setup.gotoPage("XWiki", "XWikiAdminGroup").openCommentsDocExtraPane();
        addCommentForm = commentsTab.getAddCommentForm();
        addCommentForm.addToContentField("xyz");
        assertEquals("xyz", addCommentForm.clickPreview().getText());
    }
}
