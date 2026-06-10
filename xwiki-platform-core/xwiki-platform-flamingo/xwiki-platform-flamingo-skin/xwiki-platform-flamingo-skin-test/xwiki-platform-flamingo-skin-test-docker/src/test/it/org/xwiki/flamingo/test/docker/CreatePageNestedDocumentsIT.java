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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BreadcrumbElement;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DocumentDoesNotExistPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that the create action and UI properly create Nested Documents at various levels in the hierarchy.
 *
 * @version $Id$
 * @since 7.2RC1
 */
@UITest
class CreatePageNestedDocumentsIT
{
    // @formatter:off
    static List<DocumentReference> nestedDocuments = Arrays.asList(
        // /A (Top level document)
        new DocumentReference("xwiki", List.of("A"), "WebHome"),
        // /A/B (Child document of a top level document, A)
        new DocumentReference("xwiki", Arrays.asList("A", "B"), "WebHome"),
        // /A/B/C (Child document of a child document, B)
        new DocumentReference("xwiki", Arrays.asList("A", "B", "C"), "WebHome"),
        // /A/B/C/D (Child of a child, of a child, C)
        new DocumentReference("xwiki", Arrays.asList("A", "B", "C", "D"), "WebHome"),
        // /A/B/C/D/E (etc...)
        new DocumentReference("xwiki", Arrays.asList("A", "B", "C", "D", "E"), "WebHome"),
        // /A/B/C/D/E/F/G (Child document of a non-existing document, i.e. hole in the hierarchy at the end)
        new DocumentReference("xwiki", Arrays.asList("A", "B", "C", "D", "E", "F", "G"), "WebHome"),
        // /X/Y (Child document of a non-existing top level document, i.e. hole in the hierarchy at the beginning)
        new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome"));
    // @formatter:on

    @BeforeEach
    void setUp(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Cleanup to avoid problems from previous runs.
        for (DocumentReference pageReference : nestedDocuments) {
            setup.rest().delete(pageReference);
        }
    }

    @Test
    @Order(1)
    void createNestedDocumentsFromURL(TestUtils setup)
    {
        // Create and assert each nested document.
        for (DocumentReference pageReference : nestedDocuments) {
            // Navigate from URL to the page in view mode, using the no-WebHome URL, e.g. /A instead of /A/WebHome
            SpaceReference spaceReference = pageReference.getLastSpaceReference();
            ViewPage viewPage = setup.gotoPage(spaceReference);

            // It should not exist and we will create it.
            assertFalse(viewPage.exists(), String.format("Document [%s] already exists", pageReference));
            new DocumentDoesNotExistPage().clickEditThisPageToCreate();
            new CreatePagePage().clickCreate();
            EditPage editPage = new EditPage();
            viewPage = editPage.clickSaveAndView();

            // Check that we created the right page
            assertCreatedNestedDocument(setup, pageReference, viewPage);
        }
    }

    @Test
    @Order(2)
    void createNestedDocumentsFromUI(TestUtils setup)
    {
        // Create the homepage if it does not exist and start the test from there.
        DocumentReference homepage = new DocumentReference("xwiki", "Main", "WebHome");
        ViewPage viewPage = setup.createPage(homepage, "", "Home Page");

        // Create and assert each nested document.
        for (DocumentReference pageReference : nestedDocuments) {
            SpaceReference spaceReference = pageReference.getLastSpaceReference();

            // Click the create button from the previous page.
            CreatePagePage createPage = viewPage.createPage();

            // Determine the values to fill in the form.
            WikiReference wikiReference = spaceReference.getWikiReference();
            EntityReference localParentSpaceReference = spaceReference.removeParent(wikiReference).getParent();
            String spaceReferenceString = setup.serializeReference(localParentSpaceReference);
            String pageName = spaceReference.getName();

            // Fill in the form and submit it, using the space name as title.
            EditPage editPage = createPage.createPage(pageName, spaceReferenceString, null, false);

            // Save the page.
            viewPage = editPage.clickSaveAndView();

            // Check that we created the right page
            assertCreatedNestedDocument(setup, pageReference, viewPage);
        }
    }

    private void assertCreatedNestedDocument(TestUtils setup, DocumentReference pageReference, ViewPage viewPage)
    {
        SpaceReference spaceReference = pageReference.getLastSpaceReference();

        BreadcrumbElement breadcrumb = viewPage.getBreadcrumb();
        if (breadcrumb.canBeExpanded()) {
            breadcrumb.expand();
        }
        assertEquals("/" + setup.getURLFragment(spaceReference), breadcrumb.getPathAsString());
        assertEquals(spaceReference.getName(), viewPage.getDocumentTitle());
        assertEquals(setup.serializeReference(pageReference), viewPage.getMetaDataValue("reference"));
    }
}
