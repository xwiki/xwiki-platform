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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.EditRightsPane.State;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.xwiki.test.ui.po.EditRightsPane.State.ALLOW;
import static org.xwiki.test.ui.po.EditRightsPane.State.DENY;
import static org.xwiki.test.ui.po.EditRightsPane.State.NONE;

/**
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class CommentAsGuestIT
{
    private static final String COMMENT_CONTENT = "Some content";

    private static final String COMMENT_AUTHOR = "Anonymous";

    private static final String COMMENT_REPLY = "Comment Reply";

    @Test
    @Order(1)
    void testPostCommentAsGuest(TestUtils testUtils, TestReference testReference) throws Exception
    {
        updateCommentsRightsForGuest(testUtils, ALLOW);

        ViewPage vp = testUtils.createPage(testReference, "");
        CommentsTab commentsTab = vp.openCommentsDocExtraPane();

        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR, true);
        assertEquals(COMMENT_CONTENT, commentsTab.getCommentContentByID(0));
        assertEquals(COMMENT_AUTHOR, commentsTab.getCommentAuthorByID(0));
    }

    @Test
    @Order(2)
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
    @Order(3)
    void testCannotEditCommentAsAnonymous(TestUtils testUtils, TestReference testReference) throws Exception
    {
        ViewPage vp = testUtils.createPage(testReference, "");
        CommentsTab commentsTab = vp.openCommentsDocExtraPane();
        commentsTab.postCommentAsGuest(COMMENT_CONTENT, COMMENT_AUTHOR, true);
        assertFalse(commentsTab.hasEditButtonForCommentByID(commentsTab.getCommentID(COMMENT_CONTENT)));
        updateCommentsRightsForGuest(testUtils, NONE);
    }

    private void updateCommentsRightsForGuest(TestUtils testUtils, State state) throws Exception
    {
        testUtils.loginAsSuperAdmin();

        Page page = testUtils.rest().page(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"));
        org.xwiki.rest.model.jaxb.Object rightObject = testUtils.rest().object("XWiki.XWikiGlobalRights");
        String value;
        if (state == ALLOW) {
            value = "1";
        } else if (state == DENY) {
            value = "0";
        } else {
            value = null;
        }
        Property allow = testUtils.rest().property("allow", value);
        rightObject.withProperties(testUtils.rest().property("users", "XWiki.XWikiGuest"),
            testUtils.rest().property("levels", "comment"), allow);
        Objects objects = new Objects();
        objects.withObjectSummaries(rightObject);
        page.setObjects(objects);
        testUtils.rest().save(page);

        testUtils.forceGuestUser();
    }
}
