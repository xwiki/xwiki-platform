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

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the ConfigurableClass feature.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest(properties = {
    // Notes:
    // - .*.testCodeToExecutionAndAutoSandboxing.WebHome is needed since this test verifies that even if user has
    //   PR, if XWiki.ConfigurableClass is saved (thus with programming rights), it is resaved automatically to not
    //   have them.
    // - .*.testLockingAndUnlocking.* is needed because the test itself requires PR to call the
    //   $doc.getDocument().getLock() API for lack of a public API doing the same.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:.*ConfigurableClassIT\\."
        + "(testCodeToExecutionAndAutoSandboxing.WebHome"
        + "|testLockingAndUnlocking.TestConfigurable1"
        + "|testLockingAndUnlocking.TestConfigurable2)"
})
class ConfigurableClassIT
{
    @BeforeEach
    public void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Verify that if a value is specified for the {@code linkPrefix} xproperty, then a link is generated with
     * linkPrefix + prettyName of the property from the configuration class.
     */
    @Test
    @Order(1)
    void labelLinkGeneration(TestUtils setup, TestReference testReference)
    {
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", testReference.getLastSpaceReference().getName(),
            "heading", "Some Heading",
            "scope", "WIKI",
            "configurationClass", setup.serializeReference(testReference),
            "linkPrefix", "TheLinkPrefix");

        // Check that the links are there and contain the expected values
        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(
            testReference.getLastSpaceReference().getName());
        asp.waitUntilActionButtonIsLoaded();
        assertTrue(asp.hasLink("TheLinkPrefixString"));
        assertTrue(asp.hasLink("TheLinkPrefixBoolean"));
        assertTrue(asp.hasLink("TheLinkPrefixTextArea"));
        assertTrue(asp.hasLink("TheLinkPrefixSelect"));
    }

    /**
     * Creates a document with 2 configurable objects, one gets configured globally in one section and displays
     * 2 configuration fields, the other is configured in the space in another section and displays the other 2
     * fields. Fails if they are not displayed as they should be.
     *
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    @Order(2)
    void testApplicationConfiguredInMultipleSections(TestUtils setup, TestReference testReference)
    {
        String app1Section = testReference.getLastSpaceReference().getName() + "_1";
        String app2Section = testReference.getLastSpaceReference().getName() + "_2";
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", app1Section,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "String, Boolean");

        setup.addObject(testReference, "XWiki.ConfigurableClass",
            "displayInSection", app2Section,
            "scope", "SPACE",
            "heading", "Some Other Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "TextArea, Select");

        String fullName = setup.serializeReference(testReference).split(":")[1];

        // Assert that half of the configuration shows up but not the other half.
        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(app1Section);
        asp.waitUntilActionButtonIsLoaded();
        assertTrue(asp.hasHeading(2, "HSomeHeading"));
        // Save button
        // Javascript injects a save button outside of the form and removes the default save button.
        setup.getDriver().waitUntilElementIsVisible(By.xpath(
            "//div/div/p/span/input[@type='submit'][@value='Save']"));

        FormContainerElement formContainerElement = asp.getFormContainerElement();

        // Form and fields
        assertEquals(String.format("%ssave/%s", setup.getBaseBinURL(), fullName.replace('.', '/')),
            formContainerElement.getFormAction());
        assertEquals(setup.getDriver().getCurrentUrl(),
            formContainerElement.getFieldValue(By.name("xredirect")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_String")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Boolean")));
        assertTrue(formContainerElement.hasField(By.id(fullName + "_redirect")));

        // Should not be there
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_TextArea")));
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_Select")));

        // Now we go to where the other half of the configuration should be.
        asp = AdministrationSectionPage.gotoSpaceAdministration(testReference.getLastSpaceReference(), app2Section);
        asp.waitUntilActionButtonIsLoaded();
        assertTrue(asp.hasHeading(2, "HSomeOtherHeading"));
        // Save button
        // Javascript injects a save button outside of the form and removes the default save button.
        setup.getDriver().waitUntilElementIsVisible(By.xpath(
            "//div/div/p/span/input[@type='submit'][@value='Save']"));

        formContainerElement = asp.getFormContainerElement();

        // Form and fields
        assertEquals(String.format("%ssave/%s", setup.getBaseBinURL(), fullName.replace('.', '/')),
            formContainerElement.getFormAction());
        assertEquals(setup.getDriver().getCurrentUrl(),
            formContainerElement.getFieldValue(By.name("xredirect")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_TextArea")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Select")));
        assertTrue(formContainerElement.hasField(By.id(fullName + "_redirect")));

        // Should not be there
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_String")));
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_Boolean")));
    }

    /**
     * If CodeToExecute is defined in a configurable app, then it should be evaluated.
     * Also, header should be evaluated and not just printed.
     * We test with a user having Programming Rights, since if XWiki.ConfigurableClass is saved with programming
     * rights, it should resave itself so that it doesn't have them, and we want to verify this.
     */
    @Test
    @Order(3)
    void testCodeToExecutionAndAutoSandboxing(TestUtils setup, TestReference testReference) throws Exception
    {
        // fixture
        String codeToExecute = "#set($code = 's sh')"
            + "Thi${code}ould be displayed."
            + "#if($xcontext.hasProgrammingRights())"
            + "This should be displayed too."
            + "#end";
        String heading = "#set($code = 'his sho')"
            + "T${code}uld also be displayed.";
        setupConfigurableApplication(setup, testReference,
            "displayInSection", testReference.getLastSpaceReference().getName(),
            "scope", "WIKI",
            "codeToExecute", codeToExecute,
            "heading", heading
            );
        LocalDocumentReference configurableClassReference = new LocalDocumentReference("XWiki", "ConfigurableClass");
        Page restPage = setup.rest().get(configurableClassReference);
        String standardContent = restPage.getContent();
        try {
            // Modify content
            restPage.setContent(standardContent +
                "\n\n{{velocity}}Has Programming permission: $xcontext.hasProgrammingRights(){{/velocity}}");
            // Our admin will foolishly save XWiki.ConfigurableClass, giving it programming rights.
            setup.rest().save(restPage);

            // Now we look at the section for our configurable.
            setup.gotoPage(configurableClassReference, "view",
                "editor=globaladmin&section=" + testReference.getLastSpaceReference().getName());
            ViewPage viewPage = new ViewPage();
            String content = viewPage.getContent();
            assertTrue(content.contains("This should be displayed."));
            assertTrue(content.contains("This should also be displayed."));
            // Since the user has PR, the following is expected to be true
            assertTrue(content.contains("This should be displayed too."));
            // It's false because of the dropPermission in ConfigurableClass (but supposed to be fixed at some point)
            assertTrue(content.contains("Has Programming permission: false"));
            // Make sure javascript has not added a Save button.
            assertFalse(setup.getDriver().hasElementWithoutWaiting(
                By.xpath("//div/div/p/span/input[@type='submit'][@value='Save']")));
        } finally {
            // Restore initial content
            restPage.setContent(standardContent);
            // Save
            setup.rest().save(restPage);
        }
    }

    /**
     * Test add configurable application to existing section.
     *
     * This test depends on the "Presentation" section existing.
     */
    @Test
    @Order(4)
    void testAddConfigurableApplicationInExistingSection(TestUtils setup, TestReference testReference)
    {
        String section = "presentation";
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", section,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "String, Boolean, TextArea, Select");
        String fullName = setup.serializeReference(testReference).split(":")[1];

        // Check it's available in global section.
        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(section);
        assertTrue(asp.hasHeading(2, "HSomeHeading"));

        FormContainerElement formContainerElement = asp.getFormContainerElementForClass(fullName);
        assertEquals(String.format("%ssave/%s", setup.getBaseBinURL(), fullName.replace('.', '/')),
            formContainerElement.getFormAction());
        assertEquals(setup.getDriver().getCurrentUrl(),
            formContainerElement.getFieldValue(By.name("xredirect")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_String")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Boolean")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_TextArea")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Select")));
        assertTrue(formContainerElement.hasField(By.id(fullName + "_redirect")));

        // Check it's not available in space section.
        asp = AdministrationSectionPage.gotoSpaceAdministration(testReference.getLastSpaceReference(), section);
        asp.waitUntilActionButtonIsLoaded();
        assertFalse(setup.getDriver().hasElementWithoutWaiting(By.id(String.format("%s_%s", section, fullName))));
        assertFalse(asp.hasHeading(2, "HSomeHeading"));

        // Switch application to non-global
        setup.updateObject(testReference, "XWiki.ConfigurableClass", 0, "scope", "SPACE");

        // Check that it is available in space section.
        asp = AdministrationSectionPage.gotoSpaceAdministration(testReference.getLastSpaceReference(), section);
        asp.waitUntilActionButtonIsLoaded();
        assertTrue(asp.hasHeading(2, "HSomeHeading"));
        formContainerElement = asp.getFormContainerElementForClass(fullName);
        assertEquals(String.format("%ssave/%s", setup.getBaseBinURL(), fullName.replace('.', '/')),
            formContainerElement.getFormAction());
        assertEquals(setup.getDriver().getCurrentUrl(),
            formContainerElement.getFieldValue(By.name("xredirect")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_String")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Boolean")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_TextArea")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Select")));
        assertTrue(formContainerElement.hasField(By.id(fullName + "_redirect")));

        // Check that it's not available in another space.
        asp = AdministrationSectionPage.gotoSpaceAdministration(new SpaceReference("xwiki", "XWiki"), section);
        asp.waitUntilActionButtonIsLoaded();
        assertFalse(setup.getDriver().hasElementWithoutWaiting(By.id(String.format("%s_%s", section, fullName))));
        assertFalse(asp.hasHeading(2, "HSomeHeading"));

        // Check that it's not available in global section.
        asp = AdministrationSectionPage.gotoPage(section);
        asp.waitUntilActionButtonIsLoaded();
        assertFalse(setup.getDriver().hasElementWithoutWaiting(By.id(String.format("%s_%s", section, fullName))));
        assertFalse(asp.hasHeading(2, "HSomeHeading"));
    }

    /**
     * Test add configurable application to a nonexistent section.
     * This test depends on the "HopingThereIsNoSectionByThisName" section not existing.
     */
    @Test
    @Order(5)
    void testAddConfigurableApplicationInNonexistantSection(TestUtils setup, TestReference testReference)
    {
        String section = testReference.getLastSpaceReference().getName();

        // Ensure the section does not exist yet
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasNotSection("Other", section));

        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", section,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "String, Boolean, TextArea, Select");

        String fullName = setup.serializeReference(testReference).split(":")[1];

        // Check it's available in global section.
        administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasSection("Other", section));
        administrationPage.clickSection("Other", section);
        AdministrationSectionPage asp = new AdministrationSectionPage(section);
        asp.waitUntilActionButtonIsLoaded();
        assertTrue(asp.hasHeading(2, "HSomeHeading"));
        FormContainerElement formContainerElement = asp.getFormContainerElement();
        assertEquals(String.format("%ssave/%s", setup.getBaseBinURL(), fullName.replace('.', '/')),
            formContainerElement.getFormAction());
        assertEquals(setup.getDriver().getCurrentUrl(),
            formContainerElement.getFieldValue(By.name("xredirect")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_String")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Boolean")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_TextArea")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Select")));
        assertTrue(formContainerElement.hasField(By.id(fullName + "_redirect")));

        administrationPage = AdministrationPage.gotoSpaceAdministrationPage(testReference.getLastSpaceReference());
        asp.waitUntilActionButtonIsLoaded();
        assertTrue(administrationPage.hasNotSection(section));
    }

    /**
     * Fails if a user can create a Configurable application without having edit access to the configuration page (in
     * this case: XWikiPreferences)
     */
    @Test
    @Order(6)
    void testConfigurableCreatedByUnauthorizedWillNotExecute(TestUtils setup, TestReference testReference)
    {
        // Make sure the configurable page doesn't exist because otherwise we may fail to overwrite it with a
        // non-administrator user.
        setup.deletePage(testReference);

        setup.createUserAndLogin("anotherJoker", "bentOnMalice");
        String section = testReference.getLastSpaceReference().getName();
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", section,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "String, Boolean, TextArea, Select");

        String fullName = setup.serializeReference(testReference).split(":")[1];

        setup.loginAsSuperAdmin();
        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(section);
        asp.waitUntilActionButtonIsLoaded();
        assertFalse(setup.getDriver().
            hasElementWithoutWaiting(By.id(String.format("%s_%s", section, fullName))));
        assertFalse(asp.hasHeading(2, "HSomeHeading"));
    }

    /**
     * Proves that ConfigurationClass#codeToExecute is not rendered inline even if there is no
     * custom configuration class and the only content is custom content.
     */
    @Test
    @Order(7)
    void testCodeToExecuteNotInlineIfNoConfigurationClass(TestUtils setup, TestReference testReference)
    {
        String fullName = setup.serializeReference(testReference).split(":")[1];
        String helloDiv = String.format("%s_%s", fullName, "hello");
        String test = "{{html}} <div id=\""+helloDiv+"\"> <p> hello </p> </div> {{/html}}";
        String section = testReference.getLastSpaceReference().getName();
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", section,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "codeToExecute", test);

        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(section);
        asp.waitUntilActionButtonIsLoaded();
        assertFalse(asp.hasRenderingError());
        assertTrue(setup.getDriver().hasElementWithoutWaiting(By.id(helloDiv)));
    }

    /**
     * Proves that ConfigurationClass#codeToExecute is not rendered inline whether it's at the top of the
     * form or inside of the form.
     */
    @Test
    @Order(8)
    void testCodeToExecuteNotInline(TestUtils setup, TestReference testReference)
    {
        String fullName = setup.serializeReference(testReference).split(":")[1];
        String helloDiv = String.format("%s_%s", fullName, "hello");
        String test = "{{html}} <div id=\""+helloDiv+"\"> <p> hello </p> </div> {{/html}}";
        String section = testReference.getLastSpaceReference().getName();
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", section,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "codeToExecute", test,
            "propertiesToShow", "String, Boolean");

        setup.addObject(testReference, "XWiki.ConfigurableClass",
            "displayInSection", section,
            "scope", "WIKI",
            "heading", "Some Other Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "TextArea, Select");

        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(section);
        asp.waitUntilActionButtonIsLoaded();
        assertFalse(asp.hasRenderingError());
        assertTrue(setup.getDriver().hasElementWithoutWaiting(By.id(helloDiv)));
    }

    /**
     * Make sure html macros and pre tags are not being stripped
     * @see <a href="https://jira.xwiki.org/browse/XAADMINISTRATION-141">XAADMINISTRATION-141</a>
     */
    @Test
    @Order(9)
    void testNotStrippingHtmlMacros(TestUtils setup, TestReference testReference)
    {
        String test = "{{html}} <pre> {{html clean=\"false\"}} </pre> {{/html}}";
        String section = testReference.getLastSpaceReference().getName();
        String fullName = setup.serializeReference(testReference).split(":")[1];

        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", section,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "String, Boolean, TextArea, Select");

        setup.updateObject(testReference, fullName, 0,
            "TextArea", test,
            "String", test);

        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(section);
        asp.waitUntilActionButtonIsLoaded();
        FormContainerElement formContainerElement = asp.getFormContainerElement();
        assertEquals(test, formContainerElement.getFieldValue(By.name(fullName + "_0_TextArea")));
        assertEquals(test, formContainerElement.getFieldValue(By.name(fullName + "_0_String")));
    }

    /**
     * Fails unless XWiki.ConfigurableClass locks each page on view and unlocks any other configurable page.
     * Also fails if codeToExecute is not being evaluated.
     */
    @Test
    @Order(10)
    void testLockingAndUnlocking(TestUtils setup, TestReference testReference)
    {
        // Fixture
        DocumentReference page1 = new DocumentReference("TestConfigurable1", testReference.getLastSpaceReference());
        DocumentReference page2 = new DocumentReference("TestConfigurable2", testReference.getLastSpaceReference());

        // We cannot use $doc.getLock* API here since those APIs always check if the lock belongs to the current user.
        String isThisPageLocked = "{{velocity}}"
            + "#set($lock = $doc.getDocument().getLock($xcontext.getContext()))\n"
            + "#set($isLocked = $lock.getUserName() == \"XWiki.superadmin\")\n"
            + "Is This Page Locked $isLocked{{/velocity}}";

        setup.deletePage(page1);
        setup.deletePage(page2);

        setup.createPage(page1, isThisPageLocked, "");
        setup.createPage(page2, isThisPageLocked, "");

        String section1 = testReference.getLastSpaceReference().getName() + "_1";
        String section2 = testReference.getLastSpaceReference().getName() + "_2";

        setupConfigurableApplication(false, setup, page1,
            "displayInSection", section1,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(page1),
            "propertiesToShow", "String, Boolean, TextArea, Select");

        setupConfigurableApplication(false, setup, page2,
            "displayInSection", section2,
            "scope", "WIKI",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(page2),
            "propertiesToShow", "String, Boolean, TextArea, Select");

        // Go to page1 so we can retrieve the link to open a new tab
        String testPageName = page1.getLastSpaceReference().getName();
        ViewPage viewPage = setup.gotoPage(page1);
        assertEquals("Is This Page Locked false", viewPage.getContent());

        // We have to switch user context without logging out, logging out removes all locks.
        // We have to open a new window because otherwise the lock is removed when we leave the administration page.
        setup.getDriver().findElement(By.linkText(testPageName)).sendKeys(Keys.chord(Keys.CONTROL, Keys.RETURN));
        String firstTab = setup.getDriver().getWindowHandle();

        // It might take a bit of time for the driver to know there's another window.
        setup.getDriver().waitUntilCondition(input -> input.getWindowHandles().size() == 2);
        Set<String> windowHandles = setup.getDriver().getWrappedDriver().getWindowHandles();
        String secondTab = null;
        for (String handle : windowHandles) {
            if (!handle.equals(firstTab)) {
                secondTab = handle;
            }
        }

        // Go to the document, it will create a lock.
        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage(section1);
        asp.waitUntilActionButtonIsLoaded();
        setup.getDriver().switchTo().window(secondTab);

        viewPage = setup.gotoPage(page1);
        assertEquals("Is This Page Locked true", viewPage.getContent());

        viewPage = setup.gotoPage(page2);
        assertEquals("Is This Page Locked false", viewPage.getContent());

        setup.getDriver().switchTo().window(firstTab);

        asp = AdministrationSectionPage.gotoPage(section2);
        asp.waitUntilActionButtonIsLoaded();
        setup.getDriver().switchTo().window(secondTab);
        viewPage = setup.gotoPage(page1);
        assertEquals("Is This Page Locked false", viewPage.getContent());

        viewPage = setup.gotoPage(page2);
        assertEquals("Is This Page Locked true", viewPage.getContent());

        // close the second tab
        setup.getDriver().close();
        setup.getDriver().switchTo().window(firstTab);
    }

    /**
     * Make sure a user with only ADMIN right on a space can access a Configurable section.
     */
    @Test
    @Order(11)
    void testSpaceAdminUserAcess(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the admin page in a space where normal users are not allowed to edit
        DocumentReference adminSheet = new DocumentReference("xwiki", "XWiki", "testSpaceAdminUserAcess");

        // Cleanup
        setup.rest().delete(adminSheet);
        setup.deleteSpace(testReference.getLastSpaceReference());

        // Create an admin page available in a page administration
        String section = "testSpaceAdminUserAcess";
        setupConfigurableApplication(setup, adminSheet,
            "displayInSection", section,
            "scope", "WIKI+ALL_SPACES",
            "codeToExecute", "<div id=\"testSpaceAdminUserAcess\">OK</div>");

        // Create a normal user with ADMIN right on the test space
        setup.setRightsOnSpace(testReference.getLastSpaceReference(), "", "XWiki.spaceadmin", "admin", true);
        setup.createUserAndLogin("spaceadmin", "spaceadmin");

        // Make sure the user has access to the space admin section
        AdministrationSectionPage adminPage =
            AdministrationSectionPage.gotoSpaceAdministration(testReference.getLastSpaceReference(), section);
        adminPage.waitUntilActionButtonIsLoaded();
        assertFalse(setup.getDriver().hasElementWithoutWaiting(By.id("testSpaceAdminUserAcess")));
    }

    private void setupConfigurableApplication(TestUtils setup, DocumentReference testReference,
        Object... configurableClassProperties)
    {
        setupConfigurableApplication(true, setup, testReference, configurableClassProperties);
    }

    private void setupConfigurableApplication(boolean deleteDoc, TestUtils setup, DocumentReference testReference,
        Object... configurableClassProperties)
    {
        if (deleteDoc) {
            setup.deletePage(testReference);

            // Create the page with a simple configuration class.
            setup.createPage(testReference, "Test configurable application",
                testReference.getLastSpaceReference().getName());
        }

        // We always call the editor constructor to properly waits when we need, to avoid locks not being correctly
        // handled.
        setup.addClassProperty(testReference, "String", "String");
        new ClassEditPage();
        setup.addClassProperty(testReference, "Boolean", "Boolean");
        new ClassEditPage();
        setup.addClassProperty(testReference, "TextArea", "TextArea");
        new ClassEditPage();

        // Set the editor to Text and the select to static list
        setup.updateClassProperty(testReference, "TextArea_editor", "Text");
        new ViewPage();

        setup.addClassProperty(testReference, "Select", "StaticList");
        new ClassEditPage();

        // Add a ConfigurableClass xobject.
        setup.addObject(testReference, "XWiki.ConfigurableClass", configurableClassProperties);
        new ObjectEditPage();

        // Add an xobject of the new class.
        setup.addObject(testReference, setup.serializeReference(testReference));

        // Click cancel to ensure the lock on the page is deleted: the object should already be added.
        new ObjectEditPage().clickCancel();
    }
}
