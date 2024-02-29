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

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.annotation.test.po.AnnotatableViewPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version $Id$
 * @since 11.3RC1
 */
@UITest
class AnnotationsIT
{
    private static final String CONTENT = "It's an easy-to-edit website that will help you work better together. "
        + "This Wiki is made of pages sorted by spaces. You're currently in the Main space, looking at its home page "
        + "(WebHome).";

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

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        setup.deletePage(testReference);

        setup.createUser(USER_NAME, USER_PASS, "", "");
        setup.login(USER_NAME, USER_PASS);
    }

    // TODO: This test must currently be last. We can get back to a more natural order once XWIKI-9759 is fixed
    @Test
    @Order(4)
    void addAnnotationTranslation(TestUtils setup, TestReference testReference,
        LogCaptureConfiguration logCaptureConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("multilingual", "true");
        setup.setWikiPreference("languages", "en,fr");

        LocalDocumentReference referenceFR = new LocalDocumentReference(testReference, Locale.FRENCH);
        LocalDocumentReference referenceEN = new LocalDocumentReference(testReference, Locale.ENGLISH);

        setup.createPage(referenceEN, "Some content in english.", "An english page");
        setup.createPage(referenceFR, "Un peu de contenu en français.", "Une page en français");

        AnnotatableViewPage viewPage = new AnnotatableViewPage(setup.gotoPage(referenceEN));
        viewPage.addAnnotation("Some", "English word.");

        viewPage = new AnnotatableViewPage(setup.gotoPage(referenceFR));
        // We cannot wait for success since the UI is in french...
        viewPage.addAnnotation("peu", "Un mot français", false);

        viewPage = new AnnotatableViewPage(setup.gotoPage(referenceEN));
        viewPage.showAnnotationsPane();
        viewPage.clickShowAnnotations();
        assertEquals("English word.", viewPage.getAnnotationContentByText("Some"));

        viewPage = new AnnotatableViewPage(setup.gotoPage(referenceFR));
        viewPage.showAnnotationsPane();
        viewPage.clickShowAnnotations();
        assertEquals("Un mot français", viewPage.getAnnotationContentByText("peu"));

        logCaptureConfiguration.registerExcludes(
            // Seems to only happen with the default configuration (Jetty Standalone/HSQLDB)
            "java.util.zip.ZipException: zip file is empty",
            "Failed to read resource [iscroll/"
        );
    }

    @Test
    @Order(2)
    void addAndDeleteAnnotations(TestUtils setup, TestReference testReference)
    {
        AnnotatableViewPage annotatableViewPage =
            new AnnotatableViewPage(setup.createPage(testReference, CONTENT, null));
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
    @Order(1)
    void annotationsShouldNotBeShownInXWiki10Syntax(TestUtils setup, TestReference testReference)
    {
        AnnotatableViewPage annotatableViewPage = new AnnotatableViewPage(
            setup.createPage(testReference, "Some content", "AnnotationsTest in XWiki 1.0 Syntax", "xwiki/1.0"));

        annotatableViewPage.showAnnotationsPane();
        // Annotations are disabled in 1.0 Pages. This element should no be here
        assertTrue(annotatableViewPage.checkIfAnnotationsAreDisabled());
        annotatableViewPage.simulateCTRL_M();
        annotatableViewPage.waitforAnnotationWarningNotification();
    }

    @Test
    @Order(3)
    void showAnnotationsByClickingOnAQuote(TestUtils setup, TestReference testReference)
    {
        // Adds 200 'a' after the content to make sure the content is not on-screen when the comment pane is visible.
        // The intent is to make sure that clicking on the annotation quote makes the use jump to the corresponding 
        // annotation (by following the anchor).
        String paddedContent = IntStream.rangeClosed(0, 200)
            .mapToObj(i -> "a")
            .collect(Collectors.joining("\n", CONTENT, ""));
        AnnotatableViewPage annotatableViewPage =
            new AnnotatableViewPage(setup.createPage(testReference, paddedContent, null));
        annotatableViewPage.addAnnotation(ANNOTATED_TEXT_1, ANNOTATION_TEXT_1);

        // Force a page refresh to avoid having the annotations displayed.
        setup.getDriver().navigate().refresh();

        CommentsTab commentsTab = new ViewPage().openCommentsDocExtraPane();
        commentsTab.clickOnAnnotationQuote(0);
        annotatableViewPage = new AnnotatableViewPage(new ViewPage());
        annotatableViewPage.waitForAnnotationsDisplayed();
        assertTrue(annotatableViewPage.getAnnotationTextById(0).isDisplayed());
    }
}
