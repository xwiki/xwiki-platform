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
package org.xwiki.test.selenium;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationMenu;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.*;

/**
 * Verify the overall Administration application features.
 * 
 * @version $Id$
 */
public class AdministrationTest extends AbstractXWikiTestCase
{
    private AdministrationMenu administrationMenu = new AdministrationMenu();

    /**
     * Test to see an application page is included only if that application exists
     */
    @Test
    public void testApplicationSection()
    {
        clickAdministerWiki();
        assertTrue(administrationMenu.hasSectionWithId("Search"));
        // Delete the Search administration page and test it's not present in the global administration menu anymore.
        deletePage("XWiki", "SearchAdmin");
        clickAdministerWiki();
        assertTrue(administrationMenu.hasNotSectionWithId("Search"));
        restorePage("XWiki", "SearchAdmin");
    }

    /**
     * Test modifying XWiki.XWikiPreferences multi-language field and save it.
     */
    @Test
    public void testSettingXWikiPreferences()
    {
        clickAdministerWiki();
        administrationMenu.expandCategoryWithId("content").getSectionById("Localization").click();
        getSelenium().select("//select[@name='XWiki.XWikiPreferences_0_multilingual']", "label=Yes");
        clickLinkWithXPath("//input[@value='Save']", true);
        assertElementPresent("//a[@id='tmLanguages']");
    }

    /**
     * Test Panel Wizard
     */
    @Test
    public void testPanelsAdmin()
    {
        open("XWiki", "XWikiPreferences", "admin");

        // test panel wizard at global level
        administrationMenu.expandCategoryWithName("Look & Feel").getSectionByName("Look & Feel", "Panels").click();
        waitForBodyContains("Page Layout");
        clickLinkWithXPath("//a[@href='#PageLayoutSection']", false);
        waitForElement("//div[@id = 'rightcolumn']");
        clickLinkWithXPath("//div[@id='rightcolumn']", false);
        waitForBodyContains("Panel List");
        clickLinkWithXPath("//a[@href='#PanelListSection']", false);
        dragAndDrop(By.cssSelector(".panel.QuickLinks h1"), By.id("rightPanels"));
        assertElementPresent("//div[@id = 'rightPanels']/div[contains(@class, 'QuickLinks')]");
        clickLinkWithXPath("//button[normalize-space() = 'Save']", false);
        waitForNotificationSuccessMessage("The layout has been saved properly.");
        open("Main", "WebHome");
        assertElementNotPresent("leftPanels");
        assertElementPresent("rightPanels");
        assertElementPresent("//div[@id = 'rightPanels']/div[contains(@class, 'QuickLinks')]");

        // Revert changes
        open("XWiki", "XWikiPreferences", "admin");
        administrationMenu.expandCategoryWithName("Look & Feel").getSectionByName("Look & Feel", "Panels").click();
        waitForBodyContains("Page Layout");
        clickLinkWithXPath("//a[@href='#PageLayoutSection']", false);
        waitForCondition("selenium.isElementPresent(\"//div[@id='bothcolumns']\")!=false;");
        clickLinkWithXPath("//div[@id='bothcolumns']", false);
        waitForBodyContains("Panel List");
        clickLinkWithXPath("//a[@href='#PanelListSection']", false);
        dragAndDrop(By.cssSelector("#rightPanels .panel.QuickLinks h1"),
            By.cssSelector("#allviewpanels .accordionTabContentBox"));
        assertElementNotPresent("//div[@id = 'rightPanels']//div[contains(@class, 'QuickLinks')]");
        clickLinkWithXPath("//button[normalize-space() = 'Save']", false);
        waitForNotificationSuccessMessage("The layout has been saved properly.");
        open("Main", "WebHome");
        assertElementPresent("leftPanels");
        assertElementPresent("rightPanels");
        assertElementNotPresent("//div[@id = 'rightPanels']//div[contains(@class, 'QuickLinks')]");

        // test panel wizard at space level
        open("TestPanelsAdmin", "WebHome", "edit", "editor=wiki");
        setFieldValue("content", "aaa");
        clickEditSaveAndView();
        open("TestPanelsAdmin", "WebPreferences", "admin");
        administrationMenu.expandCategoryWithName("Look & Feel").getSectionByName("Look & Feel", "Panels").click();
        waitForBodyContains("Page Layout");
        clickLinkWithXPath("//a[@href='#PageLayoutSection']", false);
        waitForCondition("selenium.isElementPresent(\"//div[@id='leftcolumn']\")!=false;");
        clickLinkWithXPath("//div[@id='leftcolumn']", false);
        waitForBodyContains("Panel List");
        clickLinkWithXPath("//a[@href='#PanelListSection']", false);
        dragAndDrop(By.cssSelector(".panel.QuickLinks h1"), By.id("leftPanels"));
        clickLinkWithXPath("//button[normalize-space() = 'Save']", false);
        waitForNotificationSuccessMessage("The layout has been saved properly.");
        open("TestPanelsAdmin", "WebHome");
        assertElementPresent("leftPanels");
        assertElementPresent("//div[@id = 'leftPanels']//div[contains(@class, 'QuickLinks')]");
        open("XWiki", "WebHome");
        assertElementPresent("rightPanels");
        assertElementNotPresent("//div[@id = 'leftPanels']//div[contains(@class, 'QuickLinks')]");
    }

    /**
     * Test add configurable application to existing section.
     *
     * This test depends on the "Presentation" section existing.
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testAddConfigurableApplicationInExistingSection()
    {
        // Create the configurable for global admin.
        createConfigurableApplication("Main", "TestConfigurable", "Presentation", true);
        // Check it's available in global section.
        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=Presentation");
        assertConfigurationPresent("Main", "TestConfigurable");
        // Check it's not available in space section.
        open("Main", "WebPreferences", "admin", "editor=spaceadmin&section=Presentation");
        assertConfigurationNotPresent("Main", "TestConfigurable");
        // Switch application to non-global
        open("Main", "TestConfigurable", "edit", "editor=object");
        expandObject("XWiki.ConfigurableClass", 0);
        getSelenium().uncheck("XWiki.ConfigurableClass_0_configureGlobally");
        clickEditSaveAndView();
        // Check that it is available in space section.
        open("Main", "WebPreferences", "admin", "editor=spaceadmin&section=Presentation");
        assertConfigurationPresent("Main", "TestConfigurable");
        // Check that it's not available in another space.
        open("XWiki", "WebPreferences", "admin", "editor=spaceadmin&section=Presentation");
        assertConfigurationNotPresent("Main", "TestConfigurable");
        // Check that it's not available in global section.
        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=Presentation");
        assertConfigurationNotPresent("Main", "TestConfigurable");
    }

    /**
     * Test add configurable application to a nonexistent section.
     * <p>
     * This test depends on the "HopingThereIsNoSectionByThisName" section not existing.<br/>
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testAddConfigurableApplicationInNonexistantSection()
    {
        String section = "HopingThereIsNoSectionByThisName";
        // Create the configurable for global admin.
        createConfigurableApplication("Main", "TestConfigurable", section, true);
        // Check it's available in global section.
        clickAdministerWiki();
        administrationMenu.expandCategoryWithId("other").getSectionById(section).click();
        assertConfigurationPresent("Main", "TestConfigurable");
        // Check that it's not available in space section.
        open("Main", "WebPreferences", "admin");
        // Assert there is no menu item in the administration menu for our configurable application.
        assertTrue(administrationMenu.hasNotSectionWithId(section));
    }

    /**
     * Fails if a user can create a Configurable application without having edit access to the configuration page (in
     * this case: XWikiPreferences)
     * <p>
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testConfigurableCreatedByUnauthorizedWillNotExecute()
    {
        // Make sure the configurable page doesn't exist because otherwise we may fail to overwrite it with a
        // non-administrator user.
        deletePage("Main", "testConfigurableCreatedByUnauthorizedWillNotExecute");
        // Create the configurable for global administrator.
        loginAndRegisterUser("anotherJoker", "bentOnMalice", false);
        String nonExistingSection = "HopingThereIsNoSectionByThisName";
        createConfigurableApplication("Main",
                                      "testConfigurableCreatedByUnauthorizedWillNotExecute",
                                       nonExistingSection, true);
        loginAsAdmin();
        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=" + nonExistingSection);
        assertConfigurationNotEditable("Main", "testConfigurableCreatedByUnauthorizedWillNotExecute");
    }

    /*
     * Creates a document with 2 configurable objects, one gets configured globally in one section and displays
     * 2 configuration fields, the other is configured in the space in another section and displays the other 2
     * fields. Fails if they are not displayed as they should be.
     *
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testApplicationConfiguredInMultipleSections()
    {
        String space = "Main";
        String page = "TestConfigurable";

        createConfigurableApplication(space, page, "TestSection1", true);
        open(space, page, "edit", "editor=object");
        // Add a second configurable object.
        getSelenium().select("classname", "value=XWiki.ConfigurableClass");
        clickButtonAndContinue("//input[@name='action_objectadd']");
        setFieldValue("XWiki.ConfigurableClass_1_displayInSection", "TestSection2");
        setFieldValue("XWiki.ConfigurableClass_1_heading", "Some Other Heading");
        setFieldValue("XWiki.ConfigurableClass_1_configurationClass", space + "." + page);
        getSelenium().uncheck("XWiki.ConfigurableClass_1_configureGlobally");
        // Set propertiesToShow so that each config only shows half of the properties.
        setFieldValue("XWiki.ConfigurableClass_1_propertiesToShow", "TextArea, Select");
        setFieldValue("XWiki.ConfigurableClass_0_propertiesToShow", "String, Boolean");
        clickEditSaveAndView();

        // Assert that half of the configuration shows up but not the other half.
        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=TestSection1");
        assertElementPresent("//div[@id='admin-page-content']/h2[@id='HSomeHeading']/span");
        // Fields
        String fullName = space + "." + page;
        String form = "//div[@id='admin-page-content']/form[@action='/xwiki/bin/save/" + space + "/" + page + "']";
        assertElementPresent(form + "/fieldset//label['String']");
        assertElementPresent(form + "/fieldset//input[@name='" + fullName + "_0_String']");
        assertElementPresent(form + "/fieldset//label['Boolean']");
        assertElementPresent(form + "/fieldset//select[@name='" + fullName + "_0_Boolean']");
        assertElementPresent(form + "/fieldset/input[@id='" + fullName + "_redirect']");
        // xredirect
        assertElementPresent(form + "/fieldset/input[@value='" + getSelenium().getLocation() + "'][@name='xredirect']");
        // Save button
        // assertElementPresent(form + "/div/p/span/input[@type='submit']");
        // Javascript injects a save button outside of the form and removes the default save button.
        waitForElement("//div/div/p/span/input[@type='submit'][@value='Save']");
        // Should not be here
        assertElementNotPresent(form + "/fieldset//textarea[@name='" + fullName + "_0_TextArea']");
        assertElementNotPresent(form + "/fieldset//select[@name='" + fullName + "_0_Select']");

        // Now we go to where the other half of the configuration should be.
        open("Main", "WebPreferences", "admin", "editor=spaceadmin&section=TestSection2");
        assertElementPresent("//h2[@id='HSomeOtherHeading']/span");
        // Fields
        assertElementPresent(form + "/fieldset//label");
        assertElementPresent(form + "/fieldset//textarea[@name='" + fullName + "_0_TextArea']");
        assertElementPresent(form + "/fieldset//select[@name='" + fullName + "_0_Select']");
        assertElementPresent(form + "/fieldset/input[@id='" + fullName + "_redirect']");
        // xredirect
        assertElementPresent(form + "/fieldset/input[@value='" + getSelenium().getLocation() + "'][@name='xredirect']");
        // Save button
        // assertElementPresent(form + "/div/p/span/input[@type='submit']");
        // Javascript injects a save button outside of the form and removes the default save button.
        waitForElement("//div/div/p/span/input[@type='submit'][@value='Save']");
        // Should not be here
        assertElementNotPresent(form + "/fieldset//input[@name='" + fullName + "_0_String']");
        assertElementNotPresent(form + "/fieldset//select[@name='" + fullName + "_0_Boolean']");
    }

    /*
     * Make sure html macros and pre tags are not being stripped 
     * @see: https://jira.xwiki.org/browse/XAADMINISTRATION-141
     *
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testNotStrippingHtmlMacros()
    {
        String space = "Main";
        String page = "TestConfigurable";
        String test = "{{html}} <pre> {{html clean=\"false\"}} </pre> {{/html}}";

        String fullName = space + "." + page;
        String form = "//div[@id='admin-page-content']/form[@action='/xwiki/bin/save/" + space + "/" + page + "']";

        createConfigurableApplication(space, page, "TestSection1", true);
        open(space, page, "edit", "editor=object");
        expandObject(fullName, 0);
        setFieldValue(fullName + "_0_TextArea", test);
        setFieldValue(fullName + "_0_String", test);
        clickEditSaveAndView();

        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=TestSection1");
        waitForTextPresent(form + "/fieldset//textarea[@name='" + fullName + "_0_TextArea']", test);
        // Getting content from an input field required getValue and not getText
        assertTrue(getSelenium().getValue(form + "/fieldset//input[@name='" + fullName + "_0_String']").equals(test));
    }

    /*
     * If a value is specified for linkPrefix, then a link is generated with linkPrefix + prettyName of the property from
     * the configuration class.
     * linkPrefix = "http://www.xwiki.org/bin/view/Main/"
     * property prettyName = "WebHome"
     * generated link should equal "http://www.xwiki.org/bin/view/Main/WebHome"
     *
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testLabelLinkGeneration()
    {
        String space = "Main";
        String page = "TestConfigurable";
        createConfigurableApplication(space, page, "TestSection3", true);
        open(space, page, "edit", "editor=object");
        setFieldValue("XWiki.ConfigurableClass_0_linkPrefix", "TheLinkPrefix");
        clickEditSaveAndView();

        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=TestSection3");
        assertElementPresent("//form/fieldset//a[@href='TheLinkPrefixString']");
        assertElementPresent("//form/fieldset//a[@href='TheLinkPrefixBoolean']");
        assertElementPresent("//form/fieldset//a[@href='TheLinkPrefixTextArea']");
        assertElementPresent("//form/fieldset//a[@href='TheLinkPrefixSelect']");
    }

    /*
     * Fails unless XWiki.ConfigurableClass locks each page on view and unlocks any other configurable page.
     * Also fails if codeToExecute is not being evaluated.
     *
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testLockingAndUnlocking()
    {
        String space = "Main";
        String page1 = "TestConfigurable";
        String page2 = "TestConfigurable2";
        String isThisPageLocked = "{{velocity}}Is This Page Locked $doc.getLocked(){{/velocity}}";
        createConfigurableApplication(space, page1, "TestSection4", true);
        createConfigurableApplication(space, page2, "TestSection5", true);
        open(space, page1, "edit", "editor=wiki");
        setFieldValue("content", isThisPageLocked);
        clickEditSaveAndView();
        open(space, page2, "edit", "editor=wiki");
        setFieldValue("content", isThisPageLocked);
        clickEditSaveAndView();

        // Now we go to the documents and see which is locked.
        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=TestSection4");

        try {
            // We have to switch user context without logging out, logging out removes all locks.
            // We have to open a new window because otherwise the lock is removed when we leave the administration page.
            getSelenium().openWindow("http://127.0.0.1:8080" + getUrl(space, page1, "view"), getTestMethodName());
            getSelenium().selectWindow(getTestMethodName());
            assertTextPresent("Is This Page Locked true");

            open("http://127.0.0.1:8080" + getUrl(space, page2, "view"));
            assertTextPresent("Is This Page Locked false");

            getSelenium().selectWindow(null);
            open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=TestSection5");

            getSelenium().selectWindow(getTestMethodName());
            open("http://127.0.0.1:8080" + getUrl(space, page1, "view"));
            assertTextPresent("Is This Page Locked false");

            open("http://127.0.0.1:8080" + getUrl(space, page2, "view"));
            assertTextPresent("Is This Page Locked true");

            // Close the window we needed for a different user context.
            getSelenium().close();
        } finally {
            getSelenium().selectWindow(null);
        }
    }

    /*
     * If CodeToExecute is defined in a configurable app, then it should be evaluated.
     * Also header should be evaluated and not just printed.
     * If XWiki.ConfigurableClass is saved with programming rights, it should resave itself so that it doesn't have them.
     */
    @Test
    public void testCodeToExecutionAndAutoSandboxing() throws Exception
    {
        String space = "Main";
        String page = "TestConfigurable";
        String codeToExecute = "#set($code = 's sh')"
                             + "Thi${code}ould be displayed."
                             + "#if($xcontext.hasProgrammingRights())"
                             + "This should not be displayed."
                             + "#end";
        String heading = "#set($code = 'his sho')"
                       + "T${code}uld also be displayed.";
        createConfigurableApplication(space, page, "TestSection6", true);
        open(space, page, "edit", "editor=object");
        expandObject("XWiki.ConfigurableClass", 0);
        setFieldValue("XWiki.ConfigurableClass_0_codeToExecute", codeToExecute);
        setFieldValue("XWiki.ConfigurableClass_0_heading", heading);
        setFieldValue("XWiki.ConfigurableClass_0_configurationClass", "");
        clickEditSaveAndView();

        Page restPage = getUtil().rest().get(new LocalDocumentReference("XWiki", "ConfigurableClass"));
        String standardContent = restPage.getContent();
        try {
            // Modify content
            restPage.setContent(standardContent + "\n\n{{velocity}}Has Programming permission: $xcontext.hasProgrammingRights(){{/velocity}}");
            // Our admin will foolishly save XWiki.ConfigurableClass, giving it programming rights.
            getUtil().setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
            getUtil().rest().save(restPage);

            // Now we look at the section for our configurable.
            open("XWiki", "ConfigurableClass", "view", "editor=globaladmin&section=TestSection6");

            assertTextPresent("This should be displayed.");
            assertTextPresent("This should also be displayed.");
            assertTextNotPresent("This should not be displayed.");
            assertTextPresent("Has Programming permission: false");
            // Make sure javascript has not added a Save button.
            assertElementNotPresent("//div/div/p/span/input[@type='submit'][@value='Save']");
        } finally {
            // Restore initial content
            restPage.setContent(standardContent);
            // Save
            getUtil().rest().save(restPage);
        }
    }

    /*
     * Proves that ConfigurationClass#codeToExecute is not rendered inline even if there is no
     * custom configuration class and the on;y content is custom content.
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testCodeToExecuteNotInlineIfNoConfigurationClass()
    {
        String space = "Main";
        String page = "TestConfigurable";
        String test = "{{html}} <div> <p> hello </p> </div> {{/html}}";

        open(space, page, "delete", "confirm=1");
        createConfigurableApplication(space, page, "TestSection1", true);
        open(space, page, "edit", "editor=object");
        expandObject("XWiki.ConfigurableClass", 0);
        setFieldValue("XWiki.ConfigurableClass_0_configurationClass", "");
        setFieldValue("XWiki.ConfigurableClass_0_codeToExecute", test);
        clickEditSaveAndView();

        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=TestSection1");
        assertElementNotPresent("//span[@class='xwikirenderingerror']");
    }

    /*
     * Proves that ConfigurationClass#codeToExecute is not rendered inline whether it's at the top of the
     * form or inside of the form.
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void testCodeToExecuteNotInline()
    {
        String space = "Main";
        String page = "TestConfigurable";
        String test = "{{html}} <div> <p> hello </p> </div> {{/html}}";

        createConfigurableApplication(space, page, "TestSection1", true);
        open(space, page, "edit", "editor=object");
        expandObject("XWiki.ConfigurableClass", 0);
        setFieldValue("XWiki.ConfigurableClass_0_codeToExecute", test);
        setFieldValue("XWiki.ConfigurableClass_0_propertiesToShow", "String, Boolean");
        
        getSelenium().select("classname", "value=XWiki.ConfigurableClass");
        clickButtonAndContinue("//input[@name='action_objectadd']");
        setFieldValue("XWiki.ConfigurableClass_1_displayInSection", "TestSection1");
        setFieldValue("XWiki.ConfigurableClass_1_configurationClass", space + "." + page);
        setFieldValue("XWiki.ConfigurableClass_1_propertiesToShow", "TextArea, Select");
        setFieldValue("XWiki.ConfigurableClass_1_codeToExecute", test);
        getSelenium().check("XWiki.ConfigurableClass_1_configureGlobally");
        clickEditSaveAndView();

        open("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=TestSection1");
        assertElementNotPresent("//span[@class='xwikirenderingerror']");
    }

    /**
     * Test functionality of the ForgotUsername page:
     * <ul>
     * <li>A user can be found using correct email</li>
     * <li>No user is found using wrong email</li>
     * <li>Email text is properly escaped</li>
     * </ul>
     */
    @Test
    public void testForgotUsername()
    {
        String space = "Test";
        String page = "SQLTestPage";
        String mail = "webmaster@xwiki.org"; // default Admin mail
        String user = "Admin";
        String badMail = "bad_mail@evil.com";

        // Ensure there is a page we will try to find using HQL injection
        editInWikiEditor(space, page);
        setFieldValue("title", page);
        setFieldValue("content", page);
        clickEditSaveAndView();

        // test that it finds the correct user
        open("XWiki", "ForgotUsername");
        setFieldValue("e", mail);
        submit("//input[@type='submit']"); // there are no other buttons
        assertTextNotPresent("No account is registered using this email address");
        assertElementPresent("//div[@id='xwikicontent']//strong[text()='" + user + "']");

        // test that bad mail results in no results
        open("XWiki", "ForgotUsername");
        setFieldValue("e", badMail);
        submit("//input[@type='submit']"); // there are no other buttons
        assertTextPresent("No account is registered using this email address");
        assertElementNotPresent("//div[@id='xwikicontent']//strong[@value='" + user + "']");

        // XWIKI-4920 test that the email is properly escaped
        open("XWiki", "ForgotUsername");
        setFieldValue("e", "a' synta\\'x error");
        submit("//input[@type='submit']"); // there are no other buttons
        assertTextPresent("No account is registered using this email address");
        assertTextNotPresent("Error");
    }

    /*
     * Fails if there is an administration icon for the named section.
     * Must be in the administration app first.
     * Tests: XWiki.ConfigurableClass
     */
    public void assertConfigurationIconNotPresent(String section)
    {
        assertElementNotPresent("//div[contains(@class,'admin-menu')]//li[contains(@href,'section=" + section + "')]");
    }

    /**
     * Will fail unless it detects a configuration of the type created by createConfigurableApplication.<br/>
     * Tests: XWiki.ConfigurableClass
     */
    public void assertConfigurationPresent(String space, String page)
    {
        assertElementPresent("//div[@id='admin-page-content']/h2[@id='HSomeHeading']/span");
        // Fields
        String fullName = space + "." + page;
        String form = "//div[@id='admin-page-content']/form[@action='/xwiki/bin/save/" + space + "/" + page + "']";
        assertElementPresent(form + "/fieldset/dl/dt[1]/label");
        assertElementPresent(form + "/fieldset/dl/dd[1]/input[@name='" + fullName + "_0_String']");
        assertElementPresent(form + "/fieldset/dl/dt[2]/label");
        assertElementPresent(form + "/fieldset/dl/dd[2]/select[@name='" + fullName + "_0_Boolean']");
        assertElementPresent(form + "/fieldset/dl/dt[3]/label");
        assertElementPresent(form + "/fieldset/dl/dd[3]/textarea[@name='" + fullName + "_0_TextArea']");
        assertElementPresent(form + "/fieldset/dl/dt[4]/label");
        assertElementPresent(form + "/fieldset/dl/dd[4]/select[@name='" + fullName + "_0_Select']");
        assertElementPresent(form + "/fieldset/input[@id='" + fullName + "_redirect']");
        assertElementPresent(form + "/fieldset/input[@value='" + getSelenium().getLocation() + "'][@name='xredirect']");
        // JavaScript injects a save button outside of the form and removes the default save button.
        waitForElement("//*[@class = 'admin-buttons']//input[@type = 'submit' and @value = 'Save']");
    }

    /*
     * Will fail if it detects a configuration of the type created by createConfigurableApplication.
     * Tests: XWiki.ConfigurableClass
     */
    public void assertConfigurationNotPresent(String space, String page)
    {
        assertElementNotPresent("//div[@id='admin-page-content']/h1[@id='HCustomize" + space + "." + page + ":']/span");
        assertElementNotPresent("//div[@id='admin-page-content']/h2[@id='HSomeHeading']/span");
        assertConfigurationNotEditable(space, page);
    }

    public void assertConfigurationNotEditable(String space, String page)
    {
        assertElementNotPresent("//div[@id='admin-page-content']/form[@action='/xwiki/bin/save/"
                                + space + "/" + page + "']");
    }

    /**
     * Creates a new page with a configuration class with some simple fields<br/>
     * then adds an object of class configurable and one of it's own class.<br/>
     * Tests: XWiki.ConfigurableClass
     */
    public void createConfigurableApplication(String space, String page, String section, boolean global)
    {
        // We have to use an existing space because the copy page form doesn't allow entering a new space.
        String storageSpace = "Sandbox";
        String storagePage = "CreateConfigurableApplication";

        if (!tryToCopyPage(storageSpace, storagePage, space, page)) {
            // Create the page with a simple configuration class.
            createPage(space, page, "Test configurable application.", "xwiki/2.1");
            open(space, page, "edit", "editor=class");
            setFieldValue("propname", "String");
            setFieldValue("proptype", "String");
            clickButtonAndContinue("//input[@name='action_propadd']");
            setFieldValue("propname", "Boolean");
            setFieldValue("proptype", "Boolean");
            clickButtonAndContinue("//input[@name='action_propadd']");
            setFieldValue("propname", "TextArea");
            setFieldValue("proptype", "TextArea");
            clickButtonAndContinue("//input[@name='action_propadd']");
            // Expand the TextArea property we just added in order to set the "editor" meta property.
            getSelenium().click("xproperty_TextArea");
            getSelenium().select("TextArea_editor", "value=Text");
            clickEditSaveAndContinue();
            setFieldValue("propname", "Select");
            setFieldValue("proptype", "StaticList");
            clickButtonAndContinue("//input[@name='action_propadd']");

            // Go to the object section.
            open(space, page, "edit", "editor=object");

            // Add a configurable object which points to the new class as the configuration class.
            getSelenium().select("classname", "value=XWiki.ConfigurableClass");
            clickButtonAndContinue("//input[@name='action_objectadd']");
            clickEditSaveAndView();

            // Try to place it in the storage area.
            tryToCopyPage(space, page, storageSpace, storagePage);
        }

        // Go to the object section.
        open(space, page, "edit", "editor=object");

        // Add an object of the new class.
        waitForElement("classname");
        getSelenium().select("classname", "value=" + space + "." + page);
        // Scroll the page to the top because the edit menu can be activated when we hover over the add button.
        getSelenium().runScript("window.scrollTo(0, 0)");
        clickButtonAndContinue("//input[@name='action_objectadd']");

        setFieldValue("XWiki.ConfigurableClass_0_displayInSection", section);
        setFieldValue("XWiki.ConfigurableClass_0_heading", "Some Heading");
        setFieldValue("XWiki.ConfigurableClass_0_configurationClass", space + "." + page);
        
        // Unfold the XWiki.ConfigurableClass object so that we can modify its properties
        WebElement configurableClassObj = getDriver().findElement(By.id("xobject_XWiki.ConfigurableClass_0"));
        if (configurableClassObj.getAttribute("class").contains("collapsed")) {
            configurableClassObj.click();
        }
        
        if (global) {
            getSelenium().check("XWiki.ConfigurableClass_0_configureGlobally");
        } else {
            getSelenium().uncheck("XWiki.ConfigurableClass_0_configureGlobally");
        }
        // We won't set linkPrefix, propertiesToShow, codeToExecute, or iconAttachment.

        clickEditSaveAndView();
    }

    /**
     * This is used by createConfigurableApplication to store a copy of the default configurable to speed up making
     * them.
     */
    public boolean tryToCopyPage(String fromSpace, String fromPage, String toSpace, String toPage)
    {
        open(fromSpace, fromPage);
        if (!isExistingPage()) {
            return false;
        }
        return copyPage(fromSpace, fromPage, toSpace, toPage);
    }
}
