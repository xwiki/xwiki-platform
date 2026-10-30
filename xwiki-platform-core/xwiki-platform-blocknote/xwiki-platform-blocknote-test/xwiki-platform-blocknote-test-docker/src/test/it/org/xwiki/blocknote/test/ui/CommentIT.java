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
package org.xwiki.blocknote.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentForm;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verify that BlockNote can be used to write comments when it is the default WYSIWYG editor.
 *
 * @version $Id$
 */
@UITest
class CommentIT extends AbstractBlockNoteIT
{
    @Test
    @Order(1)
    void addComment(TestUtils setup, TestReference testReference)
    {
        setup.deletePage(testReference);
        ViewPage viewPage = setup.createPage(testReference, "Some content to comment on.");

        CommentsTab commentsTab = viewPage.openCommentsDocExtraPane();
        CommentForm commentForm = commentsTab.getAddCommentForm();
        // The editor gets the focus when the comment form is opened, so we can type right away.
        new BlockNoteEditor("XWiki.XWikiComments_comment").getRichTextArea().waitUntilFocused()
            .sendKeys("A comment written with BlockNote");
        commentForm.clickSubmit();

        assertEquals("A comment written with BlockNote", commentsTab.getCommentContentByID(0));
    }

    @Test
    @Order(2)
    void editComment(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "Some content to comment on.");
        // The comments can't be displayed without a date.
        CommentsTab.restPostComment(testReference, "A comment to edit", "author", "XWiki.John", "date",
            "01/01/2026 00:00:00");

        CommentsTab commentsTab = setup.gotoPage(testReference).openCommentsDocExtraPane();
        CommentForm commentForm = commentsTab.editCommentByID(0);
        // The editor gets the focus with the caret at the start of the content.
        new BlockNoteEditor("XWiki.XWikiComments_0_comment").getRichTextArea().waitUntilFocused()
            .sendKeys("Edited: ");
        commentForm.clickSubmit();

        assertEquals("Edited: A comment to edit", commentsTab.getCommentContentByID(0));
    }

    @Test
    @Order(3)
    void replyToComment(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "Some content to comment on.");
        // The comments can't be displayed without a date.
        CommentsTab.restPostComment(testReference, "A comment to reply to", "date", "01/01/2026 00:00:00");

        CommentsTab commentsTab = setup.gotoPage(testReference).openCommentsDocExtraPane();
        CommentForm commentForm = commentsTab.replyToCommentByID(0);
        new BlockNoteEditor("XWiki.XWikiComments_comment").getRichTextArea().waitUntilFocused().sendKeys("A reply");
        // The reply button of the comment we reply to is hidden while the reply form is open.
        assertFalse(commentsTab.isReplyButtonVisibleForCommentByID(0));
        commentForm.clickSubmit();

        assertEquals("A reply", commentsTab.getCommentContentByID(1));
    }
}
