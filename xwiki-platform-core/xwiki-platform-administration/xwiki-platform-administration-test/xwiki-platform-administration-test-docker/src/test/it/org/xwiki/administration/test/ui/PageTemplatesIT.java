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
package org.xwiki.administration.test.ui;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.TemplateProviderInlinePage;
import org.xwiki.administration.test.po.TemplatesAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DocumentDoesNotExistPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the page templates feature (create page templates, publish them using template providers and use them to create
 * new pages).
 *
 * @version $Id$
 * @since 12.9RC1
 */
@UITest
class PageTemplatesIT
{
    /**
     * Name of the template.
     */
    public static final String TEMPLATE_NAME = "TestTemplate";

    @BeforeEach
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Tests if a new page can be created from a template.
     */
    @Test
    @Order(1)
    void createPagesFromTemplate(TestUtils setup, TestReference testReference) throws Exception
    {
        // Step 0: Setup the correct environment for the test

        cleanUp(setup, testReference);

        String templateContent = "Test Template Content";
        LocalDocumentReference templateProviderReference = new LocalDocumentReference(TEMPLATE_NAME + "Provider",
            testReference.getLocalDocumentReference().getParent());
        String templateProviderFullName = setup.serializeReference(templateProviderReference);
        String testSpace = setup.serializeReference(templateProviderReference.getParent());

        // Step 1: Create a Template and a Template Provider and try to create a new page by using the Add Menu and
        // using the created Template

        ViewPage templateProviderView = createTemplateAndTemplateProvider(setup, templateProviderReference,
            templateContent, "Test Template Title", false);

        // Create the new document from template
        CreatePagePage createPagePage = templateProviderView.createPage();
        // Save the number of available templates so that we can make some checks later on.
        int availableTemplateSize = createPagePage.getAvailableTemplateSize();
        String templateInstanceName = TEMPLATE_NAME + "Instance";
        createPagePage.getDocumentPicker().toggleLocationAdvancedEdit();
        EditPage templateInstanceEditWysiwyg =
            createPagePage.createPageFromTemplate(templateInstanceName, testSpace, null, templateProviderFullName);
        WikiEditPage templateInstanceEdit = templateInstanceEditWysiwyg.clickSaveAndView().editWiki();

        // Verify template instance location and content
        assertEquals(templateInstanceName, templateInstanceEdit.getTitle());
        assertEquals(testSpace + "." + templateInstanceName, templateInstanceEdit.getMetaDataValue("space"));
        assertEquals("WebHome", templateInstanceEdit.getMetaDataValue("page"));
        assertEquals(templateContent, templateInstanceEdit.getContent());
        // check the parent of the template instance
        assertEquals(templateProviderFullName, templateInstanceEdit.getParent());

        // Step 2: Create a wanted link and verify that clicking it displays the Template and that we can use it.

        // Put a wanted link in the template instance
        templateInstanceEdit.setContent("[[doc:NewPage]]");
        ViewPage vp = templateInstanceEdit.clickSaveAndView();

        // Verify that clicking on the wanted link pops up a box to choose the template.
        EntityReference wantedLinkReference =
            setup.resolveDocumentReference(testSpace + "." + TEMPLATE_NAME + "Instance" + ".NewPage");
        createPagePage = vp.clickWantedLink(wantedLinkReference);
        assertEquals(availableTemplateSize, createPagePage.getAvailableTemplateSize());
        assertTrue(createPagePage.getAvailableTemplates().contains(templateProviderFullName));

        // Step 3: Create a new page when located on a non-existing page

        LocalDocumentReference unexistingPageReference =
            new LocalDocumentReference(TEMPLATE_NAME + "UnexistingInstance", templateProviderReference.getParent());
        setup.gotoPage(unexistingPageReference, "view", "spaceRedirect=false");
        vp = new ViewPage();
        assertFalse(vp.exists());
        DocumentDoesNotExistPage unexistingPage = new DocumentDoesNotExistPage();
        unexistingPage.clickEditThisPageToCreate();
        CreatePagePage createUnexistingPage = new CreatePagePage();
        // Make sure we're in create mode.
        assertTrue(setup.isInCreateMode());
        // count the available templates, make sure they're as many as before and that our template is among them
        assertEquals(availableTemplateSize, createUnexistingPage.getAvailableTemplateSize());
        assertTrue(createUnexistingPage.getAvailableTemplates().contains(templateProviderFullName));
        // select it
        createUnexistingPage.setTemplate(templateProviderFullName);
        createUnexistingPage.setTerminalPage(true);
        // and create
        createUnexistingPage.clickCreate();
        EditPage ep = new EditPage();
        WikiEditPage unexistingPageEdit = ep.clickSaveAndView().editWiki();

        // Verify template instance location and content
        assertEquals(testSpace, unexistingPageEdit.getMetaDataValue("space"));
        assertEquals(TEMPLATE_NAME + "UnexistingInstance", unexistingPageEdit.getMetaDataValue("page"));
        assertEquals(TEMPLATE_NAME + "UnexistingInstance", unexistingPageEdit.getTitle());
        assertEquals(templateContent, unexistingPageEdit.getContent());
        // test that this page has no parent
        assertEquals("Main.WebHome", unexistingPageEdit.getParent());

        // Step 4: Create an empty new page when there are Templates available

        // Make sure we are on a page that exists so that Add > Page will show the space and page fields

        CreatePagePage createEmptyPage = unexistingPageEdit.clickCancel().createPage();
        assertTrue(createEmptyPage.getAvailableTemplateSize() > 0);
        createEmptyPage.getDocumentPicker().toggleLocationAdvancedEdit();
        EditPage editEmptyPage = createEmptyPage.createPage(testSpace, "EmptyPage");
        ViewPage emptyPage = editEmptyPage.clickSaveAndView();
        // make sure it's empty
        assertEquals("", emptyPage.getContent());
        // make sure parent is the right one
        assertEquals("/" + testSpace.replace('.', '/') + "/EmptyPage", emptyPage.getBreadcrumbContent());
        // mare sure title is the right one
        assertEquals("EmptyPage", emptyPage.getDocumentTitle());

        // Step 5: Verify that restricting a Template to a space works

        // Restrict the template to its own space
        templateProviderView = setup.gotoPage(templateProviderReference);
        templateProviderView.editInline();
        TemplateProviderInlinePage templateProviderInline = new TemplateProviderInlinePage();
        List<String> allowedSpaces = new ArrayList<String>();
        allowedSpaces.add(testSpace);
        templateProviderInline.setVisibilityRestrictions(allowedSpaces);
        templateProviderInline.setCreationRestrictions(allowedSpaces);
        templateProviderView = templateProviderInline.clickSaveAndView();

        // Verify we can still create a page from template in the test space
        createPagePage = templateProviderView.createPage();
        // Make sure we get in create mode.
        assertTrue(setup.isInCreateMode());
        assertEquals(availableTemplateSize, createPagePage.getAvailableTemplateSize());
        assertTrue(createPagePage.getAvailableTemplates().contains(templateProviderFullName));

        // Modify the target space and verify the form can't be submitted
        createPagePage.setTemplate(templateProviderFullName);
        createPagePage.getDocumentPicker().toggleLocationAdvancedEdit();
        createPagePage.getDocumentPicker().setParent("Foo");
        createPagePage.getDocumentPicker().setName("Bar");
        String currentURL = setup.getDriver().getCurrentUrl();
        createPagePage.clickCreate(false);
        assertEquals(currentURL, setup.getDriver().getCurrentUrl());
        // and check that an error is displayed to the user
        createPagePage.waitForFieldErrorMessage();

        // Verify the template we have removed is no longer available.
        createPagePage = CreatePagePage.gotoPage();

        // make sure that the template provider is not in the list of templates
        assertFalse(createPagePage.getAvailableTemplates().contains(templateProviderFullName));
    }

    /**
     * Tests that creating a page or a space that already exists displays an error.
     */
    @Test
    @Order(2)
    void createExistingPageAndSpace(TestUtils setup, TestReference testReference) throws Exception
    {
        // Step 0: Setup the correct environment for the test

        cleanUp(setup, testReference);

        LocalDocumentReference templateProviderReference = new LocalDocumentReference(TEMPLATE_NAME + "Provider",
            testReference.getLocalDocumentReference().getParent());
        String templateProviderFullName = setup.serializeReference(templateProviderReference);
        String testSpace = setup.serializeReference(templateProviderReference.getParent());

        // create a template to make sure that we have a template to create from
        createTemplateAndTemplateProvider(setup, templateProviderReference, "Templates are fun", "Funny templates",
            false);

        // create a page and a space webhome
        EntityReference existingPageReference = setup.resolveDocumentReference(testSpace + ".ExistingPage.WebHome");
        String existingSpaceName = testSpace + ".ExistingSpace";
        setup.rest().savePage(existingPageReference, "Page that already exists", "Existing page");
        setup.rest().savePage(new LocalDocumentReference(existingSpaceName, "WebHome"), "Some content",
            "Existing space");

        // Step 1: Create an empty page for a page that already exists
        // First we must click on create from a page that already exists as otherwise we won't get the create UI
        ViewPage vp = setup.gotoPage(existingPageReference);
        CreatePagePage createPage = vp.createPage();
        createPage.getDocumentPicker().toggleLocationAdvancedEdit();
        createPage.getDocumentPicker().setParent(testSpace);
        createPage.getDocumentPicker().setName("ExistingPage");
        String currentURL = setup.getDriver().getCurrentUrl();
        createPage.clickCreate(false);
        // make sure that we stay on the same page and that an error is displayed to the user. Maybe we should check the
        // error
        assertEquals(currentURL, setup.getDriver().getCurrentUrl());
        createPage.waitForErrorMessage();

        // Step 2: Create a page from Template for a page that already exists
        // restart everything to make sure it's not the error before
        vp = setup.gotoPage(existingPageReference);
        createPage = vp.createPage();
        createPage.getDocumentPicker().toggleLocationAdvancedEdit();
        createPage.getDocumentPicker().setParent(testSpace);
        createPage.getDocumentPicker().setName("ExistingPage");
        createPage.setTemplate(templateProviderFullName);
        currentURL = setup.getDriver().getCurrentUrl();
        createPage.clickCreate(false);
        // make sure that we stay on the same page and that an error is displayed to the user. Maybe we should check the
        // error
        assertEquals(currentURL, setup.getDriver().getCurrentUrl());
        createPage.waitForErrorMessage();

        // Step 3: Create a space that already exists
        // Since the Flamingo skin no longer supports creating a space from the UI, trigger the Space creation UI
        // by using directly the direct action URL for it.
        setup.gotoPage(existingPageReference, "create", "tocreate=space");
        CreatePagePage createSpace = new CreatePagePage();
        // Check that the terminal choice is not displayed in this mode.
        assertFalse(createSpace.isTerminalOptionDisplayed());

        currentURL = setup.getDriver().getCurrentUrl();
        // strip the parameters out of this URL
        currentURL =
            currentURL.substring(0, currentURL.indexOf('?') > 0 ? currentURL.indexOf('?') : currentURL.length());
        // Try to create the a space (non-terminal document) that already exist.
        createSpace.getDocumentPicker().toggleLocationAdvancedEdit();
        createSpace.fillForm(existingSpaceName, "", null, false);
        createSpace.clickCreate(false);
        String urlAfterSubmit = setup.getDriver().getCurrentUrl();
        urlAfterSubmit = urlAfterSubmit.substring(0,
            urlAfterSubmit.indexOf('?') > 0 ? urlAfterSubmit.indexOf('?') : urlAfterSubmit.length());
        // make sure that we stay on the same page and that an error is displayed to the user. Maybe we should check the
        // error
        assertEquals(currentURL, urlAfterSubmit);
        createSpace.waitForErrorMessage();
    }

    /**
     * Tests the creation of a page from a save and edit template, tests that the page is indeed saved.
     */
    @Test
    @Order(3)
    void createPageWithSaveAndEditTemplate(TestUtils setup, TestReference testReference) throws Exception
    {
        cleanUp(setup, testReference);

        // Create a template
        String templateContent = "Templates are fun";
        LocalDocumentReference templateProviderReference = new LocalDocumentReference(TEMPLATE_NAME + "Provider",
            testReference.getLocalDocumentReference().getParent());
        String templateProviderFullName = setup.serializeReference(templateProviderReference);
        String testSpace = setup.serializeReference(templateProviderReference.getParent());
        ViewPage templatePage = createTemplateAndTemplateProvider(setup, templateProviderReference, templateContent,
            "Funny templates", true);

        // create the page
        CreatePagePage createPage = templatePage.createPage();
        createPage.getDocumentPicker().toggleLocationAdvancedEdit();
        EditPage editCreatedPage = createPage.createPageFromTemplate(testSpace, "NewPage", templateProviderFullName);
        // and now cancel it
        ViewPage newPage = editCreatedPage.clickCancel();
        // make sure we're not in unexisting page
        assertTrue(newPage.exists());
        // we should be in view mode (useless check since the impl of isNonExisting page calls it anyway)
        assertTrue(setup.isInViewMode());
        // make sure it's the page we want
        assertEquals(testSpace + ".NewPage", newPage.getMetaDataValue("space"));
        assertEquals("WebHome", newPage.getMetaDataValue("page"));
        // and now test the title is the name of the page and the content is the one from the template
        assertEquals("NewPage", newPage.getDocumentTitle());
        assertEquals(templateContent, newPage.getContent());
        // and the parent, it should be the template provider, since that's where we created it from
        assertEquals("/" + testSpace.replace('.', '/') + "/NewPage", newPage.getBreadcrumbContent());
    }

    /**
     * The goal of this test is to check that if a template is forbidden for a user then:
     *   1. the template content won't be displayed in the editor
     *   2. the page will still be created properly from an empty template
     */
    @Test
    @Order(4)
    void createPageFromForbiddenTemplate(TestUtils setup, TestReference testReference) throws Exception
    {
        cleanUp(setup, testReference);

        DocumentReference templateReference = new DocumentReference("Template", testReference.getLastSpaceReference());
        DocumentReference newDoc = new DocumentReference("Document", testReference.getLastSpaceReference());

        setup.createPage(templateReference, "A forbidden template");
        String userName = testReference.getLastSpaceReference().getName();
        setup.createUser(userName, userName, "");

        // Prevent the user to see the template
        setup.setRights(templateReference, "", userName, "view", false);

        setup.login(userName, userName);
        ViewPage viewPage = setup.gotoPage(templateReference);
        assertTrue(viewPage.isForbidden());

        setup.gotoPage(newDoc, "edit", "template=" + templateReference.toString());
        WikiEditPage wikiEditPage = new WikiEditPage();

        assertTrue(wikiEditPage.getContent().isEmpty());
        wikiEditPage.setContent("Some content in that page");

        viewPage = wikiEditPage.clickSaveAndView();
        assertEquals("Some content in that page", viewPage.getContent());
    }

    /**
     * The goal of this test is to check that the template provider's title is correctly escaped.
     */
    @Test
    @Order(5)
    void templateProviderTitleEscaping(TestUtils setup, TestReference testReference) throws Exception
    {
        cleanUp(setup, testReference);

        // Create a template
        String templateContent = "Templates are fun";
        String providerName = "{{html}}<span>HTML</span>{{/html}}";
        LocalDocumentReference templateProviderReference = new LocalDocumentReference(providerName,
            testReference.getLocalDocumentReference().getParent());
        createTemplateAndTemplateProvider(setup, templateProviderReference, templateContent,
            "Funny templates", true);

        TemplatesAdministrationSectionPage adminPage = TemplatesAdministrationSectionPage.gotoPage();
        List<WebElement> links = adminPage.getExistingTemplatesLinks();
        assertFalse(links.stream().anyMatch(element -> element.getText().equals("HTML")));
        assertTrue(links.stream().anyMatch(element -> providerName.equals(element.getText())));
    }

    /**
     * Helper function to Create both a Template and a Template Provider for the tests in this class.
     */
    private ViewPage createTemplateAndTemplateProvider(TestUtils setup,
        LocalDocumentReference templateProviderReference, String templateContent, String templateTitle,
        boolean saveAndEdit) throws Exception
    {
        // Create the template page in the same space as the template provider.
        LocalDocumentReference templateReference =
            new LocalDocumentReference(TEMPLATE_NAME, templateProviderReference.getParent());
        setup.rest().savePage(templateReference, templateContent, templateTitle);

        // Create the template provider.
        TemplatesAdministrationSectionPage sectionPage = TemplatesAdministrationSectionPage.gotoPage();
        TemplateProviderInlinePage templateProviderInline =
            sectionPage.createTemplateProvider(templateProviderReference);
        templateProviderInline.setTemplateName("Test Template");
        templateProviderInline.setTemplate(setup.serializeReference(templateReference));
        if (saveAndEdit) {
            templateProviderInline.setActionOnCreate(TemplateProviderInlinePage.ACTION_SAVEANDEDIT);
        }
        return templateProviderInline.clickSaveAndView();
    }

    private void cleanUp(TestUtils setup, TestReference testReference) throws Exception
    {
        // We need to create the root page first in order to be able to delete all its child pages afterwards.
        setup.rest().savePage(testReference);
        setup.deletePage(testReference, true);
    }
}
