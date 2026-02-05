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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.NotFoundException;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentElement;
import org.xwiki.test.ui.po.CommentForm;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;

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

    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @BeforeEach
    void beforeEach(TestUtils setup)
    {
        resetDateFormatProperty(setup);
    }

    @AfterAll
    static void afterAll(TestUtils setup)
    {
        resetDateFormatProperty(setup);
    }

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
        CommentForm editCommentForm = commentsTab.editCommentByID(commentIndex);
        assertEquals(COMMENT_CONTENT, editCommentForm.getContentField().getText());
        editCommentForm.clearAndSetContentField(COMMENT_REPLACED_CONTENT);
        editCommentForm.clickSubmit();
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

    private String getDateString(int seconds)
    {
        return DEFAULT_DATE_FORMAT.format(new Date(seconds * 1000L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Order(4)
    void commentsAreOrderedByDate(boolean isFrenchDateFormat, TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        // ensure the page doesn't exist before the test starts
        setup.rest().delete(reference);
        // ensure that all dates are using GMT-0 to be displayed.
        setup.setPropertyInXWikiPreferences("timezone", "String", "UTC");
        if (isFrenchDateFormat) {
            setup.setPropertyInXWikiPreferences("dateformat", "String", "dd-MM-yyyy HH:mm");
        }
        setup.rest().savePage(reference, "Test comment order", "Test comment order");
        Property authorProperty = TestUtils.RestTestUtils.property("author", "XWiki.superadmin");

        // Object 0 date 100 (order: 5)
        Object commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        Property dateProperty = TestUtils.RestTestUtils.property("date", getDateString(100));
        Property contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 0 at date 100");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 1 date 42 (order: 2)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(42));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 1 at date 42");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 2 date 100 (order: 6)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(100));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 2 at date 100");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 3 date 24 (order: 1)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(24));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 3 at date 24");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 4 date 154 reply to 1 (order: 4 - thread 1)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(154));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 4 at date 154 reply to 1");
        Property replyToProperty = TestUtils.RestTestUtils.property("replyto", 1);
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty, replyToProperty));
        setup.rest().add(commentObject);

        // object 5 date 122 (order: 7)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(122));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 5 at date 122");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 6 date 3600*24*46 (1 month and 15 days after) (order: 10)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(3600*24*46));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 6 at date 1 month 15 days after");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 7 date 66 reply to 1 (order: 3 - thread 1)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(66));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 7 at date 66 reply to 1");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty, replyToProperty));
        setup.rest().add(commentObject);

        // object 8 date 3600*24*10 (11 days after) (order: 8)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(3600*24*11));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 8 at date 11 days after");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 9 date 3600*24*36 (1 month 5 days after) (order: 9)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(3600*24*36));
        contentProperty = TestUtils.RestTestUtils.property("comment", "Comment object 9 at date 1 month 5 days after");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        // object 1 date 3600*24*(365 + 31) (1 year 1 month 1 day after) (order: 10)
        commentObject = setup.rest().object(reference, "XWiki.XWikiComments");
        dateProperty = TestUtils.RestTestUtils.property("date", getDateString(3600*24*396));
        contentProperty = TestUtils.RestTestUtils.property("comment",
            "Comment object 10 at date 1 year 1 month 1 day after");
        commentObject.getProperties().addAll(List.of(dateProperty, authorProperty, contentProperty));
        setup.rest().add(commentObject);

        ViewPage viewPage = setup.gotoPage(reference);
        CommentsTab commentsTab = viewPage.openCommentsDocExtraPane();
        List<CommentElement> comments = commentsTab.getComments();
        assertEquals(11, comments.size());

        CommentElement commentElement = comments.get(0);
        assertDate("1970", "01", "01", "00", commentElement.getDate(), isFrenchDateFormat);
        assertEquals("Comment object 3 at date 24", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertFalse(commentElement.isReply());

        commentElement = comments.get(1);
        assertEquals("Comment object 1 at date 42", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "01", "01", "00", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());

        commentElement = comments.get(2);
        commentsTab.toggleCommentThread(1);
        assertEquals("Comment object 7 at date 66 reply to 1", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "01", "01", "01", commentElement.getDate(), isFrenchDateFormat);
        assertTrue(commentElement.isReply());

        commentElement = comments.get(3);
        assertEquals("Comment object 4 at date 154 reply to 1", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "01", "01", "02", commentElement.getDate(), isFrenchDateFormat);
        assertTrue(commentElement.isReply());

        commentElement = comments.get(4);
        assertEquals("Comment object 0 at date 100", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "01", "01", "01", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());

        commentElement = comments.get(5);
        assertEquals("Comment object 2 at date 100", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "01", "01", "01", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());

        commentElement = comments.get(6);
        assertEquals("Comment object 5 at date 122", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "01", "01", "02", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());

        commentElement = comments.get(7);
        assertEquals("Comment object 8 at date 11 days after", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "01", "12", "00", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());

        commentElement = comments.get(8);
        assertEquals("Comment object 9 at date 1 month 5 days after", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "02", "06", "00", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());

        commentElement = comments.get(9);
        assertEquals("Comment object 6 at date 1 month 15 days after", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1970", "02", "16", "00", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());

        commentElement = comments.get(10);
        assertEquals("Comment object 10 at date 1 year 1 month 1 day after", commentElement.getContent());
        assertEquals("superadmin", commentElement.getAuthor());
        assertDate("1971", "02", "01", "00", commentElement.getDate(), isFrenchDateFormat);
        assertFalse(commentElement.isReply());
    }

    private void assertDate(String expectedYear, String expectedMonth, String expectedDay, String expectedMinute,
        String obtainedDate, boolean isFrenchFormat)
    {
        String englishFormat = "%1$s/%2$s/%3$s 00:%4$s";
        String frenchFormat = "%3$s-%2$s-%1$s 00:%4$s";

        if (isFrenchFormat) {
            assertEquals(String.format(frenchFormat, expectedYear, expectedMonth, expectedDay, expectedMinute),
                obtainedDate);
        } else {
            assertEquals(String.format(englishFormat, expectedYear,  expectedMonth, expectedDay, expectedMinute),
                obtainedDate);
        }
    }

    private static void resetDateFormatProperty(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.setPropertyInXWikiPreferences("dateformat", "String", "");
    }
}
