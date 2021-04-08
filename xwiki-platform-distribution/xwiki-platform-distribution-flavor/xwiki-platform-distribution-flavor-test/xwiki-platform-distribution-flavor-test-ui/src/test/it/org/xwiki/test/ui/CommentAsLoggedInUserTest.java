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
package org.xwiki.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NotFoundException;
import org.xwiki.test.ui.po.CommentForm;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test comment and reply on XWiki Pages when logged in as a standard user.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public class CommentAsLoggedInUserTest extends AbstractTest
{
    private static final String USER_NAME = "commentUser";
    private static final String USER_PASSWORD = "commentUserPassword";

    private static final String CONTENT = "Some dummy Content";

    private static final String TITLE = "CommentsTest Page";

    private static final String COMMENT_CONTENT = "Some content";

    private static final String COMMENT_REPLACED_CONTENT = "Some replaced content";

    private static final String COMMENT_REPLY = "Comment Reply";

    private CommentsTab commentsTab;

    @BeforeAll
    public void beforeAll()
    {
        getUtil().createUser(USER_NAME, USER_PASSWORD, null);
        // Use wysiwyg editor to test using comments with CKEditor.
        getUtil().updateObject("XWiki", "Admin", "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        getUtil().login(USER_NAME, USER_PASSWORD);
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        ViewPage vp = getUtil().createPage(getTestClassName(), getTestMethodName(), CONTENT, TITLE);
        this.commentsTab = vp.openCommentsDocExtraPane();
    }

    @Test
    void postComment()
    {
        this.commentsTab.openCommentForm();
        assertTrue(this.commentsTab.isCommentFormShown());
        int commentIndex = this.commentsTab.postComment(COMMENT_CONTENT, true);
        assertEquals(COMMENT_CONTENT, this.commentsTab.getCommentContentByID(commentIndex));
        assertEquals(USER_NAME, this.commentsTab.getCommentAuthorByID(commentIndex));
    }

    @Test
    void replyToComment()
    {
        this.commentsTab.openCommentForm();
        int commentIndex = this.commentsTab.postComment(COMMENT_CONTENT, true);
        this.commentsTab.replyToCommentByID(commentIndex, COMMENT_REPLY);
        int replyIndex = this.commentsTab.getCommentID(COMMENT_REPLY);
        assertEquals(USER_NAME, this.commentsTab.getCommentAuthorByID(replyIndex));
    }

    @Test
    void deleteComment()
    {
        this.commentsTab.openCommentForm();
        assertTrue(this.commentsTab.isCommentFormShown());
        int commentId = this.commentsTab.postComment(COMMENT_CONTENT, true);
        this.commentsTab.deleteCommentByID(commentId);
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            this.commentsTab.getCommentID(COMMENT_CONTENT);
        });
        assertEquals(String.format("Comment with content [%s] cannot be found.", COMMENT_CONTENT),
            notFoundException.getMessage());
    }

    @Test
    void editComment()
    {
        this.commentsTab.openCommentForm();
        assertTrue(this.commentsTab.isCommentFormShown());
        int commentIndex = this.commentsTab.postComment(COMMENT_CONTENT, true);
        this.commentsTab.editCommentByID(commentIndex, COMMENT_REPLACED_CONTENT);
        assertEquals(COMMENT_REPLACED_CONTENT, this.commentsTab.getCommentContentByID(commentIndex));
    }

    /**
     * Preview a comment on a plain wiki page.
     */
    @Test
    void previewComment()
    {
        // We login as admin to have the text editor.
        getUtil().loginAsAdmin();
        final ViewPage vp = getUtil().gotoPage(getTestClassName(), getTestMethodName());
        vp.openCommentsDocExtraPane();
        CommentForm addCommentForm = this.commentsTab.getAddCommentForm();
        addCommentForm.addToContentField("one **two** three");
        assertEquals("one two three", addCommentForm.clickPreview().getText());
        addCommentForm.clickBack();
        addCommentForm.addToContentField(" //four//");
        addCommentForm.clickPreview();
        addCommentForm.clickSubmit();
        assertTrue(this.commentsTab.getCommentID("one two three four") >= 0);
    }

    
    /**
     * Preview a comment on a wiki page that has a sheet applied.
     */
    @Test
    void previewCommentOnPageWithSheet()
    {
        // We login as admin to have the text editor.
        getUtil().loginAsAdmin();
        // We know Blog.BlogIntroduction has a sheet applied.
        this.commentsTab = getUtil().gotoPage("XWiki", "DefaultSkin").openCommentsDocExtraPane();
        CommentForm addCommentForm = this.commentsTab.getAddCommentForm();
        addCommentForm.addToContentField("xyz");
        assertEquals("xyz", addCommentForm.clickPreview().getText());
    }
}
