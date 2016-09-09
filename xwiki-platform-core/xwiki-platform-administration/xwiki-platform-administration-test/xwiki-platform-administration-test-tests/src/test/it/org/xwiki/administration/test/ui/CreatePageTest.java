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

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.TemplateProviderInlinePage;
import org.xwiki.administration.test.po.TemplatesAdministrationSectionPage;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DocumentDoesNotExistPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests page creation with and without a Template Provider/Template.
 *
 * @version $Id$
 * @since 2.4M1
 */
public class CreatePageTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    /**
     * Name of the template.
     */
    public static final String TEMPLATE_NAME = "TestTemplate";

    /**
     * Tests if a new page can be created from a template.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void createPagesFromTemplate() throws Exception
    {
        // Step 0: Setup the correct environment for the test

        // All these pages are created during this test
        getUtil().deleteSpace(getTestClassName());
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        EntityReference templateInstanceReference =
            getUtil().resolveDocumentReference(getTestClassName() + "." + TEMPLATE_NAME + "Instance" + ".WebHome");
        getUtil().rest().delete(templateInstanceReference);
        getUtil().rest().deletePage(getTestClassName(), "NewPage");
        getUtil().rest().deletePage(getTestClassName(), TEMPLATE_NAME + "UnexistingInstance");
        getUtil().rest().deletePage(getTestClassName(), "EmptyPage");

        String templateContent = "Test Template Content";
        String templateTitle = "Test Template Title";
        String templateProviderName = TEMPLATE_NAME + "Provider";
        String templateProviderFullName = getTestClassName() + "." + templateProviderName;

        // Step 1: Create a Template and a Template Provider and try to create a new page by using the Add Menu and
        //         using the created Template

        ViewPage templateProviderView =
            createTemplateAndTemplateProvider(templateProviderName, templateContent, templateTitle, false);

        // Create the new document from template
        CreatePagePage createPagePage = templateProviderView.createPage();
        // Save the number of available templates so that we can make some checks later on.
        int availableTemplateSize = createPagePage.getAvailableTemplateSize();
        String templateInstanceName = TEMPLATE_NAME + "Instance";
        createPagePage.getDocumentPicker().toggleLocationAdvancedEdit();
        EditPage templateInstanceEditWysiwyg =
            createPagePage.createPageFromTemplate(templateInstanceName, getTestClassName(), null,
                templateProviderFullName);
        WikiEditPage templateInstanceEdit = templateInstanceEditWysiwyg.clickSaveAndView().editWiki();

        // Verify template instance location and content
        assertEquals(templateInstanceName, templateInstanceEdit.getTitle());
        assertEquals(getTestClassName() + "." + templateInstanceName, templateInstanceEdit.getMetaDataValue("space"));
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
            getUtil().resolveDocumentReference(getTestClassName() + "." + TEMPLATE_NAME + "Instance" + ".NewPage");
        vp.clickWantedLink(wantedLinkReference, true);
        // TODO: a page object should be better here
        List<WebElement> templates =
                getDriver().findElements(By.xpath("//input[@name='type' and @data-type='template']"));
        // Note: We need to remove 1 to exclude the "Empty Page" template entry
        assertEquals(availableTemplateSize, templates.size());
        assertTrue(createPagePage.getAvailableTemplates().contains(templateProviderFullName));

        // Step 3: Create a new page when located on a non-existing page

        getUtil().gotoPage(getTestClassName(), TEMPLATE_NAME + "UnexistingInstance", "view", "spaceRedirect=false");
        vp = new ViewPage();
        assertFalse(vp.exists());
        DocumentDoesNotExistPage unexistingPage = new DocumentDoesNotExistPage();
        unexistingPage.clickEditThisPageToCreate();
        CreatePagePage createUnexistingPage = new CreatePagePage();
        // Make sure we're in create mode.
        assertTrue(getUtil().isInCreateMode());
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
        assertEquals(getTestClassName(), templateInstanceEdit.getMetaDataValue("space"));
        assertEquals(TEMPLATE_NAME + "UnexistingInstance", templateInstanceEdit.getMetaDataValue("page"));
        assertEquals(TEMPLATE_NAME + "UnexistingInstance", unexistingPageEdit.getTitle());
        assertEquals(templateContent, unexistingPageEdit.getContent());
        // test that this page has no parent
        assertEquals("Main.WebHome", unexistingPageEdit.getParent());

        // Step 4: Create an empty new page when there are Templates available

        // Make sure we are on a page that exists so that Add > Page will show the space and page fields

        CreatePagePage createEmptyPage = unexistingPageEdit.clickCancel().createPage();
        assertTrue(createEmptyPage.getAvailableTemplateSize() > 0);
        createEmptyPage.getDocumentPicker().toggleLocationAdvancedEdit();
        EditPage editEmptyPage = createEmptyPage.createPage(getTestClassName(), "EmptyPage");
        ViewPage emptyPage = editEmptyPage.clickSaveAndView();
        // make sure it's empty
        assertEquals("", emptyPage.getContent());
        // make sure parent is the right one
        assertEquals("/" + getTestClassName() + "/EmptyPage", emptyPage.getBreadcrumbContent());
        // mare sure title is the right one
        assertEquals("EmptyPage", emptyPage.getDocumentTitle());

        // Step 5: Verify that restricting a Template to a space works

        // Restrict the template to its own space
        templateProviderView = getUtil().gotoPage(getTestClassName(), TEMPLATE_NAME + "Provider");
        templateProviderView.editInline();
        TemplateProviderInlinePage templateProviderInline = new TemplateProviderInlinePage();
        List<String> allowedSpaces = new ArrayList<String>();
        allowedSpaces.add(getTestClassName());
        templateProviderInline.setVisibilityRestrictions(allowedSpaces);
        templateProviderInline.setCreationRestrictions(allowedSpaces);
        templateProviderView = templateProviderInline.clickSaveAndView();

        // Verify we can still create a page from template in the test space
        createPagePage = templateProviderView.createPage();
        // Make sure we get in create mode.
        assertTrue(getUtil().isInCreateMode());
        assertEquals(availableTemplateSize, createPagePage.getAvailableTemplateSize());
        assertTrue(createPagePage.getAvailableTemplates().contains(templateProviderFullName));

        // Modify the target space and verify the form can't be submitted
        createPagePage.setTemplate(templateProviderFullName);
        createPagePage.getDocumentPicker().toggleLocationAdvancedEdit();
        createPagePage.getDocumentPicker().setParent("Foo");
        createPagePage.getDocumentPicker().setName("Bar");
        String currentURL = getDriver().getCurrentUrl();
        createPagePage.clickCreate();
        assertEquals(currentURL, getDriver().getCurrentUrl());
        // and check that an error is displayed to the user
        createPagePage.waitForFieldErrorMessage();

        // Verify the template we have removed is no longer available.
        CreatePagePage.gotoPage();

        // make sure that the template provider is not in the list of templates
        assertFalse(createPagePage.getAvailableTemplates().contains(templateProviderFullName));
    }

    /**
     * Tests that creating a page or a space that already exists displays an error.
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See http://jira.xwiki.org/browse/XE-1146")
    public void createExistingPageAndSpace() throws Exception
    {
        // Step 0: Setup the correct environment for the test

        EntityReference existingPageReference =
            getUtil().resolveDocumentReference(getTestClassName() + ".ExistingPage.WebHome");
        String existingSpaceName = getTestClassName() + "Existing";

        // All these pages are created during this test
        getUtil().rest().delete(existingPageReference);
        getUtil().rest().deletePage(existingSpaceName, "WebHome");

        // create a template to make sure that we have a template to create from
        String templateProviderName = TEMPLATE_NAME + "Provider";
        String templateContent = "Templates are fun";
        String templateTitle = "Funny templates";
        createTemplateAndTemplateProvider(templateProviderName, templateContent, templateTitle, false);

        // create a page and a space webhome
        getUtil().rest().savePage(existingPageReference, "Page that already exists", "Existing page");
        getUtil().rest().savePage(new LocalDocumentReference(existingSpaceName, "WebHome"), "Some content",
            "Existing space");

        // Step 1: Create an empty page for a page that already exists
        // First we must click on create from a page that already exists as otherwise we won't get the create UI
        ViewPage vp = getUtil().gotoPage(existingPageReference);
        CreatePagePage createPage = vp.createPage();
        createPage.getDocumentPicker().toggleLocationAdvancedEdit();
        createPage.getDocumentPicker().setParent(getTestClassName());
        createPage.getDocumentPicker().setName("ExistingPage");
        String currentURL = getDriver().getCurrentUrl();
        createPage.clickCreate();
        // make sure that we stay on the same page and that an error is displayed to the user. Maybe we should check the
        // error
        assertEquals(currentURL, getDriver().getCurrentUrl());
        createPage.waitForErrorMessage();

        // Step 2: Create a page from Template for a page that already exists
        // restart everything to make sure it's not the error before
        vp = getUtil().gotoPage(existingPageReference);
        createPage = vp.createPage();
        createPage.getDocumentPicker().toggleLocationAdvancedEdit();
        createPage.getDocumentPicker().setParent(getTestClassName());
        createPage.getDocumentPicker().setName("ExistingPage");
        createPage.setTemplate(getTestClassName() + "." + templateProviderName);
        currentURL = getDriver().getCurrentUrl();
        createPage.clickCreate();
        // make sure that we stay on the same page and that an error is displayed to the user. Maybe we should check the
        // error
        assertEquals(currentURL, getDriver().getCurrentUrl());
        createPage.waitForErrorMessage();

        // Step 3: Create a space that already exists
        // Since the Flamingo skin no longer supports creating a space from the UI, trigger the Space creation UI
        // by using directly the direct action URL for it.
        getUtil().gotoPage(
            getUtil().getURL("create", new String[] {getTestClassName(), "ExistingPage", "WebHome"}, "tocreate=space"));
        CreatePagePage createSpace = new CreatePagePage();
        // Check that the terminal choice is not displayed in this mode.
        assertFalse(createSpace.isTerminalOptionDisplayed());

        currentURL = getDriver().getCurrentUrl();
        // strip the parameters out of this URL
        currentURL =
            currentURL.substring(0, currentURL.indexOf('?') > 0 ? currentURL.indexOf('?') : currentURL.length());
        // Try to create the a space (non-terminal document) that already exist.
        createSpace.getDocumentPicker().toggleLocationAdvancedEdit();
        createSpace.fillForm(existingSpaceName, "", null, false);
        createSpace.clickCreate();
        String urlAfterSubmit = getDriver().getCurrentUrl();
        urlAfterSubmit =
            urlAfterSubmit.substring(0,
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
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void createPageWithSaveAndEditTemplate() throws Exception
    {
        // Cleanup of the test space for any leftovers from previous tests.
        getUtil().deleteSpace(getTestClassName());

        // Create a template
        String templateProviderName = TEMPLATE_NAME + "Provider";
        String templateContent = "Templates are fun";
        String templateTitle = "Funny templates";
        String templateProviderFullName = getTestClassName() + "." + templateProviderName;
        ViewPage templatePage =
            createTemplateAndTemplateProvider(templateProviderName, templateContent, templateTitle, true);

        // create the page
        CreatePagePage createPage = templatePage.createPage();
        createPage.getDocumentPicker().toggleLocationAdvancedEdit();
        EditPage editCreatedPage =
            createPage.createPageFromTemplate(getTestClassName(), "NewPage", templateProviderFullName);
        // and now cancel it
        ViewPage newPage = editCreatedPage.clickCancel();
        // make sure we're not in unexisting page
        assertTrue(newPage.exists());
        // we should be in view mode (useless check since the impl of isNonExisting page calls it anyway)
        assertTrue(getUtil().isInViewMode());
        // make sure it's the page we want
        assertEquals(getTestClassName() + ".NewPage", newPage.getMetaDataValue("space"));
        assertEquals("WebHome", newPage.getMetaDataValue("page"));
        // and now test the title is the name of the page and the content is the one from the template
        assertEquals("NewPage", newPage.getDocumentTitle());
        assertEquals(templateContent, newPage.getContent());
        // and the parent, it should be the template provider, since that's where we created it from
        assertEquals("/CreatePageTest/NewPage", newPage.getBreadcrumbContent());
    }

    /**
     * Helper function to Create both a Template and a Template Provider for the tests in this class.
     */
    private ViewPage createTemplateAndTemplateProvider(String templateProviderName, String templateContent,
        String templateTitle, boolean saveAndEdit) throws Exception
    {
        // Cleanup of the test space for any leftovers from previous tests.
        getUtil().deleteSpace(getTestClassName());

        // Create a Template page
        getUtil().rest().savePage(new LocalDocumentReference(getTestClassName(), TEMPLATE_NAME), templateContent,
            templateTitle);

        // Create a Template Provider
        TemplatesAdministrationSectionPage sectionPage = TemplatesAdministrationSectionPage.gotoPage();
        TemplateProviderInlinePage templateProviderInline =
            sectionPage.createTemplateProvider(getTestClassName(), templateProviderName);
        templateProviderInline.setTemplateName("Test Template");
        templateProviderInline.setTemplate(getTestClassName() + "." + TEMPLATE_NAME);
        if (saveAndEdit) {
            templateProviderInline.setActionOnCreate(TemplateProviderInlinePage.ACTION_SAVEANDEDIT);
        }
        return templateProviderInline.clickSaveAndView();
    }
}
