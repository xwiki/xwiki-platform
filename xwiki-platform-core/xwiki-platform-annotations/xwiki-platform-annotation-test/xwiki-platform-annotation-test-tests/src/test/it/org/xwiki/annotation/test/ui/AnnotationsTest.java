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
package org.xwiki.annotation.test.ui;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.annotation.test.po.AnnotatableViewPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.CommentsTab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Annotation Test.
 *
 * @version $Id$
 * @since 2.7RC1
 */
public class AnnotationsTest extends AbstractTest
{
    // Note: Make sure Annotations and Comments are merged (this is not the default ATM!) by setting the
    // $showannotation velocity variable to false in the page content
    private static final String CONTENT = "{{velocity}}#set ($showannotations = false){{/velocity}}"
        + "It's an easy-to-edit website that will help you work better together. This Wiki is made of pages "
        + "sorted by spaces. You're currently in the Main space, looking at its home page (WebHome).";

    private static final String ANNOTATED_TEXT_1 = "work better together";

    private static final String ANNOTATION_TEXT_1 = "XWiki motto";

    private static final String ANNOTATED_TEXT_2 = "WebHome";

    private static final String ANNOTATION_TEXT_2 = "Every Space has it's own webhome";

    private static final String ANNOTATED_TEXT_3 = "Main space";

    private static final String ANNOTATION_TEXT_3 = "Each XWiki instance has a Main space";

    private static final String ANNOTATED_TEXT_4 = "easy-to-edit website";

    private static final String ANNOTATION_TEXT_4 = "Yes, we have our WYSIWYG";

    private static final String USER_NAME = "UserAnnotation";

    private static final String USER_PASS = "pass";

    @Before
    public void setUp() throws Exception
    {
        getUtil().createUser(USER_NAME, USER_PASS, "", "");
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    public void addAndDeleteAnnotations() throws Exception
    {
        getUtil().loginAsAdmin();
        getUtil().deletePage(getTestClassName(), getTestMethodName());
        getUtil().login(USER_NAME, USER_PASS);
        AnnotatableViewPage annotatableViewPage = new AnnotatableViewPage(
            getUtil().createPage(getTestClassName(), getTestMethodName(), CONTENT, null));
        CommentsTab commentsTab = annotatableViewPage.getWrappedViewPage().openCommentsDocExtraPane();

        annotatableViewPage.addAnnotation(ANNOTATED_TEXT_1, ANNOTATION_TEXT_1);
        annotatableViewPage.addAnnotation(ANNOTATED_TEXT_2, ANNOTATION_TEXT_2);
        annotatableViewPage.addAnnotation(ANNOTATED_TEXT_3, ANNOTATION_TEXT_3);
        annotatableViewPage.addAnnotation(ANNOTATED_TEXT_4, ANNOTATION_TEXT_4);

        int commentId = annotatableViewPage.getCommentId(annotatableViewPage.getAnnotationIdByText(ANNOTATED_TEXT_1));
        assertTrue(commentsTab.hasEditButtonForCommentByID(commentId));
        assertTrue(commentsTab.hasDeleteButtonForCommentByID(commentId));
        assertEquals(ANNOTATION_TEXT_1, annotatableViewPage.getAnnotationContentByText(ANNOTATED_TEXT_1));
        commentId = annotatableViewPage.getCommentId(annotatableViewPage.getAnnotationIdByText(ANNOTATED_TEXT_2));
        assertTrue(commentsTab.hasEditButtonForCommentByID(commentId));
        assertTrue(commentsTab.hasDeleteButtonForCommentByID(commentId));
        assertEquals(ANNOTATION_TEXT_2, annotatableViewPage.getAnnotationContentByText(ANNOTATED_TEXT_2));
        commentId = annotatableViewPage.getCommentId(annotatableViewPage.getAnnotationIdByText(ANNOTATED_TEXT_3));
        assertTrue(commentsTab.hasEditButtonForCommentByID(commentId));
        assertTrue(commentsTab.hasDeleteButtonForCommentByID(commentId));
        assertEquals(ANNOTATION_TEXT_3, annotatableViewPage.getAnnotationContentByText(ANNOTATED_TEXT_3));
        commentId = annotatableViewPage.getCommentId(annotatableViewPage.getAnnotationIdByText(ANNOTATED_TEXT_4));
        assertTrue(commentsTab.hasEditButtonForCommentByID(commentId));
        assertTrue(commentsTab.hasDeleteButtonForCommentByID(commentId));
        assertEquals(ANNOTATION_TEXT_4, annotatableViewPage.getAnnotationContentByText(ANNOTATED_TEXT_4));

        // It seems that there are some issues refreshing content while this tab is not open. This might be a bug in the
        // Annotations Application
        annotatableViewPage.showAnnotationsPane();
        annotatableViewPage.deleteAnnotationByText(ANNOTATED_TEXT_1);
        annotatableViewPage.deleteAnnotationByText(ANNOTATED_TEXT_2);
        annotatableViewPage.deleteAnnotationByText(ANNOTATED_TEXT_3);
        annotatableViewPage.deleteAnnotationByText(ANNOTATED_TEXT_4);
    }

    /**
     * This test creates a XWiki 1.0 syntax page, and tries to add annotations to it, and checks if the warning messages
     * are shown This test is against XAANNOTATIONS-17
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    public void annotationsShouldNotBeShownInXWiki10Syntax() throws Exception
    {
        getUtil().loginAsAdmin();
        getUtil().deletePage(getTestClassName(), getTestMethodName());
        // Note: Make sure Annotations and Comments are merged (this is not the default ATM!) by setting the
        // $showannotation velocity variable to false in the page content
        AnnotatableViewPage annotatableViewPage = new AnnotatableViewPage(
            getUtil().createPage(getTestClassName(), getTestMethodName(),
                "#set ($showannotations = false)\nSome content",
                "AnnotationsTest in XWiki 1.0 Syntax", "xwiki/1.0"));

        annotatableViewPage.showAnnotationsPane();
        // Annotations are disabled in 1.0 Pages. This element should no be here
        assertTrue(annotatableViewPage.checkIfAnnotationsAreDisabled());
        annotatableViewPage.simulateCTRL_M();
        annotatableViewPage.waitforAnnotationWarningNotification();
    }
}
