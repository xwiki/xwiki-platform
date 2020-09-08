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
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentForm;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test comment and reply on XWiki Pages when logged as Administrator.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class CommentAsAdminIT
{
    private static final String COMMENT_CONTENT = "Some content";

    private static final String COMMENT_REPLACED_CONTENT = "Some replaced content";

    private static final String ADMIN = "Admin";

    private static final String COMMENT_REPLY = "Comment Reply";

    @Test
    @Order(1)
    void testPostCommentAsAdmin(TestReference testReference, TestUtils testUtils)
    {
        testUtils.createAdminUser();
        testUtils.loginAsAdmin();
        definePreferedEditor(testUtils, "Wysiwyg");
        CommentsTab commentsTab = goToCommentPane(testReference, testUtils);
        assertTrue(commentsTab.isCommentFormShown());
        commentsTab.postComment(COMMENT_CONTENT, true);
        assertEquals(COMMENT_CONTENT, commentsTab.getCommentContentByID(commentsTab.getCommentID(COMMENT_CONTENT)));
        assertEquals(ADMIN, commentsTab.getCommentAuthorByID(commentsTab.getCommentID(COMMENT_CONTENT)));
    }

    @Test
    @Order(2)
    void testReplyToCommentAsAdmin(TestReference testReference, TestUtils testUtils)
    {
        CommentsTab commentsTab = goToCommentPane(testReference, testUtils);
        assertTrue(commentsTab.isCommentFormShown());
        commentsTab.postComment(COMMENT_CONTENT, true);
        commentsTab.replyToCommentByID(commentsTab.getCommentID(COMMENT_CONTENT), COMMENT_REPLY);
        assertEquals(COMMENT_REPLY, commentsTab.getCommentContentByID(commentsTab.getCommentID(COMMENT_REPLY)));
        assertEquals(ADMIN, commentsTab.getCommentAuthorByID(commentsTab.getCommentID(COMMENT_REPLY)));
    }

    @Test
    @Order(3)
    void testDeleteCommentAsAdmin(TestReference testReference, TestUtils testUtils)
    {
        CommentsTab commentsTab = goToCommentPane(testReference, testUtils);
        assertTrue(commentsTab.isCommentFormShown());
        commentsTab.postComment(COMMENT_CONTENT, true);
        commentsTab.deleteCommentByID(commentsTab.getCommentID(COMMENT_CONTENT));
        assertEquals(-1, commentsTab.getCommentID(COMMENT_CONTENT));
    }

    @Test
    @Order(4)
    void testEditCommentAsAdmin(TestReference testReference, TestUtils testUtils)
    {
        CommentsTab commentsTab = goToCommentPane(testReference, testUtils);
        assertTrue(commentsTab.isCommentFormShown());
        commentsTab.postComment(COMMENT_CONTENT, true);
        commentsTab.editCommentByID(0, COMMENT_REPLACED_CONTENT);
        assertEquals(COMMENT_REPLACED_CONTENT, commentsTab.getCommentContentByID(0));
    }

    /**
     * Preview a comment on a plain wiki page.
     */
    @Test
    @Order(5)
    void testPreviewComment(TestReference testReference, TestUtils testUtils)
    {
        definePreferedEditor(testUtils, "Text");
        CommentsTab commentsTab = goToCommentPane(testReference, testUtils);
        CommentForm addCommentForm = commentsTab.getAddCommentForm();
        addCommentForm.addToContentField("one **two** three");
        assertEquals("one two three", addCommentForm.clickPreview().getText());
        addCommentForm.clickBack();
        addCommentForm.addToContentField(" //four//");
        addCommentForm.clickPreview();
        addCommentForm.clickSubmit();
        assertTrue(commentsTab.getCommentID("one two three four") >= 0);
    }

    /**
     * Preview a comment on a wiki page that has a sheet applied.
     */
    @Test
    @Order(6)
    void testPreviewCommentOnPageWithSheet(TestReference testReference, TestUtils testUtils)
    {
        // We know Blog.BlogIntroduction has a sheet applied.
        CommentsTab commentsTab = goToCommentPane(testReference, testUtils);
        CommentForm addCommentForm = commentsTab.getAddCommentForm();
        addCommentForm.addToContentField("xyz");
        assertEquals("xyz", addCommentForm.clickPreview().getText());
    }

    private void definePreferedEditor(TestUtils testUtils, String text)
    {
        testUtils.updateObject("XWiki", "Admin", "XWiki.XWikiUsers", 0, "editor", text);
    }

    private CommentsTab goToCommentPane(TestReference testReference, TestUtils testUtils)
    {
        ViewPage viewPage = testUtils.gotoPage(testReference);
        testUtils.createPage(testReference, "");
        return viewPage.openCommentsDocExtraPane();

    }
}
