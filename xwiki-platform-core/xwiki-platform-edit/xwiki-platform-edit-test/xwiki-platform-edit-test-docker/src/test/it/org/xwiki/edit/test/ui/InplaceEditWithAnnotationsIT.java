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
package org.xwiki.edit.test.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.TimeoutException;
import org.xwiki.annotation.test.po.AnnotatableViewPage;
import org.xwiki.annotation.test.po.AnnotationsLabel;
import org.xwiki.annotation.test.po.AnnotationsPane;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that page annotations don't break in-place editing.
 * 
 * @version $Id$
 */
@UITest
class InplaceEditWithAnnotationsIT
{
    @BeforeEach
    void setup(TestUtils setup, TestReference testReference)
    {
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg");
        setup.deletePage(testReference);
        setup.createPage(testReference, "one two three", "title");

        // Create some annotations.
        String target = setup.serializeReference(testReference.getLocalDocumentReference());
        setup.addObject(testReference, "XWiki.XWikiComments", "selection", "two", "comment", "foo", "target", target,
            "author", "XWiki.alice", "state", "SAFE");
        setup.addObject(testReference, "XWiki.XWikiComments", "selection", "three", "comment", "bar", "target", target,
            "author", "XWiki.alice", "state", "SAFE");
    }

    @Test
    void editInplaceWithAnnotations(TestUtils setup, TestReference testReference)
    {
        setup.gotoPage(testReference);
        InplaceEditablePage viewPage = new InplaceEditablePage();

        // Check what happens if in-place edit is triggered while annotations are displayed.
        AnnotatableViewPage annotatedPage = new AnnotatableViewPage(viewPage);
        AnnotationsPane annotationsPane = annotatedPage.showAnnotationsPane();
        annotatedPage.clickShowAnnotations(true);
        // Moreover, let's open an annotation balloon (with the annotation details).
        new AnnotationsLabel().showAnnotationByText("two");
        // And also open the annotation creation dialog.
        annotatedPage.beginAddAnnotation("one");

        viewPage.editInplace();

        // The annotation settings pane should be hidden now.
        assertFalse(annotationsPane.isDisplayed());

        // Try to open the annotation settings pane. It shouldn't work. Note that we don't wait for the pane to appear
        // because the pane should be already loaded but hidden.
        annotationsPane = annotatedPage.showAnnotationsPane(false);
        assertFalse(annotationsPane.isDisplayed());

        // Annotations should not be displayed.
        assertEquals(0, annotatedPage.getAnnotationCount());

        // Try to toggle the annotation display using the keyboard shortcut.
        annotatedPage.toggleAnnotationDisplayUsingShortcutKey(false);
        assertEquals(0, annotatedPage.getAnnotationCount());

        // Try to add an annotation.
        try {
            annotatedPage.beginAddAnnotation("one");
            fail("The annotation creation dialog was opened.");
        } catch (TimeoutException e) {
            // Expected exception.
        }

        // Verify that the edited content is not affected if we delete an annotation (comment) while editing in place.
        CommentsTab commentsTab = viewPage.openCommentsDocExtraPane();
        commentsTab.deleteCommentByID(0);

        viewPage.setDocumentTitle("updated title");
        viewPage.saveAndView();

        // Annotation display must be restored after in-place editing ends.
        annotatedPage.waitForAnnotationsDisplayed();
        // Also verify that one annotation was deleted (from the in-place edit mode).
        assertEquals(1, annotatedPage.getAnnotationCount());

        // Verify that the annotated content was not affected.
        WikiEditPage wikiEditPage = WikiEditPage.gotoPage(testReference);
        assertEquals("updated title", wikiEditPage.getTitle());
        assertEquals("one two three", wikiEditPage.getContent());
    }
}
