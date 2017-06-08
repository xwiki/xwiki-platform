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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.po.CommentForm;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Test comment and reply on XWiki Pages when logged as Administrator.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public class CommentAsAdminTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    private CommentsTab commentsTab;

    private static final String CONTENT = "Some dummy Content";

    private static final String TITLE = "CommentsTest Page";

    private static final String COMMENT_CONTENT = "Some content";

    private static final String COMMENT_REPLACED_CONTENT = "Some replaced content";

    private static final String ADMIN = "Administrator";

    private static final String COMMENT_REPLY = "Comment Reply";

    @Before
    public void setUp() throws Exception
    {
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        ViewPage vp = getUtil().createPage(getTestClassName(), getTestMethodName(), CONTENT, TITLE);
        this.commentsTab = vp.openCommentsDocExtraPane();
    }

    @Test
    public void testPostCommentAsAdmin()
    {
        Assert.assertTrue(this.commentsTab.isCommentFormShown());
        this.commentsTab.postComment(COMMENT_CONTENT, true);
        Assert.assertEquals(COMMENT_CONTENT,
            this.commentsTab.getCommentContentByID(this.commentsTab.getCommentID(COMMENT_CONTENT)));
        Assert.assertEquals(ADMIN,
            this.commentsTab.getCommentAuthorByID(this.commentsTab.getCommentID(COMMENT_CONTENT)));
    }

    @Test
    public void testReplyToCommentAsAdmin()
    {
        this.commentsTab.postComment(COMMENT_CONTENT, true);
        this.commentsTab.replyToCommentByID(this.commentsTab.getCommentID(COMMENT_CONTENT), COMMENT_REPLY);
        Assert.assertEquals(COMMENT_REPLY,
            this.commentsTab.getCommentContentByID(this.commentsTab.getCommentID(COMMENT_REPLY)));
        Assert.assertEquals(ADMIN, this.commentsTab.getCommentAuthorByID(this.commentsTab.getCommentID(COMMENT_REPLY)));
    }

    @Test
    public void testDeleteCommentAsAdmin()
    {
        Assert.assertTrue(this.commentsTab.isCommentFormShown());
        this.commentsTab.postComment(COMMENT_CONTENT, true);
        this.commentsTab.deleteCommentByID(this.commentsTab.getCommentID(COMMENT_CONTENT));
        Assert.assertTrue(this.commentsTab.getCommentID(COMMENT_CONTENT) == -1);
    }

    @Test
    public void testEditCommentAsAdmin()
    {
        Assert.assertTrue(this.commentsTab.isCommentFormShown());
        this.commentsTab.postComment(COMMENT_CONTENT, true);
        this.commentsTab.editCommentByID(0, COMMENT_REPLACED_CONTENT);
        Assert.assertEquals(COMMENT_REPLACED_CONTENT,
            this.commentsTab.getCommentContentByID(this.commentsTab.getCommentID(COMMENT_REPLACED_CONTENT)));
    }

    @Test
    public void testPostCommentAsAdminNoJs()
    {
        // In this test class, the only user who logs in is admin.
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "view", "xpage=xpart&vm=commentsinline.vm");
        this.commentsTab.postComment(COMMENT_CONTENT, false);
        ViewPage vp = new ViewPage();
        // This opens with ?viewer=comments, don't explicitly load the comments tab
        vp.waitUntilPageIsLoaded();
        Assert.assertEquals(COMMENT_CONTENT,
            this.commentsTab.getCommentContentByID(this.commentsTab.getCommentID(COMMENT_CONTENT)));
        Assert.assertEquals(ADMIN,
            this.commentsTab.getCommentAuthorByID(this.commentsTab.getCommentID(COMMENT_CONTENT)));
    }

    /**
     * Preview a comment on a plain wiki page.
     */
    @Test
    public void testPreviewComment()
    {
        CommentForm addCommentForm = commentsTab.getAddCommentForm();
        addCommentForm.getContentField().sendKeys("one **two** three");
        Assert.assertEquals("one two three", addCommentForm.clickPreview().getText());
        addCommentForm.clickBack();
        addCommentForm.getContentField().sendKeys(" //four//");
        addCommentForm.clickPreview();
        addCommentForm.clickSubmit();
        Assert.assertTrue(commentsTab.getCommentID("one two three four") >= 0);
    }

    /**
     * Preview a comment on a wiki page that has a sheet applied.
     */
    @Test
    public void testPreviewCommentOnPageWithSheet()
    {
        // We know Blog.BlogIntroduction has a sheet applied.
        commentsTab = getUtil().gotoPage("Blog", "BlogIntroduction").openCommentsDocExtraPane();
        CommentForm addCommentForm = commentsTab.getAddCommentForm();
        addCommentForm.getContentField().sendKeys("xyz");
        Assert.assertEquals("xyz", addCommentForm.clickPreview().getText());
    }
}
