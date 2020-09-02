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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.GlobalRightsAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.EditRightsPane.Right;
import org.xwiki.test.ui.po.EditRightsPane.State;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class CommentAsGuestTest
{
    private static final String COMMENT_CONTENT = "Some content";

    private static final String COMMENT_AUTHOR = "Anonymous";

    private static final String COMMENT_REPLY = "Comment Reply";

    @BeforeAll
    void initializeCommentRights()
    {
        // Ensure that guest user has comment permission

        setRightsOnGuest(Right.COMMENT, State.ALLOW);
    }

    private static void setRightsOnGuest(Right right, State state)
    {
        GlobalRightsAdministrationSectionPage globalRights = GlobalRightsAdministrationSectionPage.gotoPage();
        // Wait for the rights live table to load before switching in order to prevent any possible race conditions.
        globalRights.getEditRightsPane().getRightsTable().waitUntilReady();
        globalRights.getEditRightsPane().switchToUsers();
        globalRights.getEditRightsPane().setGuestRight(right, state);
    }

    @Test
    @Order(1)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testPostCommentAsGuest(TestUtils testUtils, TestReference testReference)
    {
        testUtils.loginAsAdmin();
        setRightsOnGuest(Right.COMMENT, State.ALLOW);
        testUtils.forceGuestUser();

        ViewPage vp = testUtils.createPage(testReference, "");
        CommentsTab commentsTab = vp.openCommentsDocExtraPane();

        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR, true);
        assertEquals(COMMENT_CONTENT, commentsTab.getCommentContentByID(0));
        assertEquals(COMMENT_AUTHOR, commentsTab.getCommentAuthorByID(0));
    }

    @Test
    @Order(2)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testPostCommentAsGuestNoJs(TestUtils testUtils, TestReference testReference)
    {
        testUtils.createPage(testReference, "");
        testUtils.gotoPage(testReference, "view", "xpage=xpart&vm=commentsinline.vm");
        CommentsTab commentsTab = new CommentsTab();

        commentsTab.postComment(COMMENT_CONTENT, false);
        // This opens with ?viewer=comments, don't explicitly load the comments tab
        new ViewPage().waitUntilPageIsLoaded();
        assertEquals(COMMENT_CONTENT,
            commentsTab.getCommentContentByID(commentsTab.getCommentID(COMMENT_CONTENT)));
        assertEquals(COMMENT_AUTHOR,
            commentsTab.getCommentAuthorByID(commentsTab.getCommentID(COMMENT_CONTENT)));
    }

    @Test
    @Order(3)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testReplyCommentAsAnonymous(TestUtils testUtils, TestReference testReference)
    {
        ViewPage vp = testUtils.createPage(testReference, "");
        CommentsTab commentsTab = vp.openCommentsDocExtraPane();

        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR, true);
        commentsTab.replyToCommentByID(commentsTab.getCommentID(COMMENT_CONTENT), COMMENT_REPLY);
        assertEquals(COMMENT_REPLY,
            commentsTab.getCommentContentByID(commentsTab.getCommentID(COMMENT_REPLY)));
        assertEquals(COMMENT_AUTHOR,
            commentsTab.getCommentAuthorByID(commentsTab.getCommentID(COMMENT_REPLY)));
    }

    @Test
    @Order(4)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testCannotEditCommentAsAnonymous(TestUtils testUtils, TestReference testReference)
    {
        ViewPage vp = testUtils.createPage(testReference, "");
        CommentsTab commentsTab = vp.openCommentsDocExtraPane();
        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR, true);
        assertFalse(commentsTab.hasEditButtonForCommentByID(commentsTab.getCommentID(COMMENT_CONTENT)));
    }
}
