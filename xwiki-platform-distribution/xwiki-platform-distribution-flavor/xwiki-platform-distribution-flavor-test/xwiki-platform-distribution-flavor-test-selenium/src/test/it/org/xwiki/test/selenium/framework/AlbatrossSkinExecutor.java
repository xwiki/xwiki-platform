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
package org.xwiki.test.selenium.framework;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.XWikiWebDriver;

import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;

/**
 * Implementation of skin-related actions for the Albatross skin.
 * 
 * @version $Id$
 */
public class AlbatrossSkinExecutor implements SkinExecutor
{
    private static final String WIKI_LOCATOR_FOR_KEY_EVENTS = "content";

    private AbstractXWikiTestCase test;

    public AlbatrossSkinExecutor(AbstractXWikiTestCase test)
    {
        this.test = test;
    }

    protected AbstractXWikiTestCase getTest()
    {
        return this.test;
    }

    @Override
    public void clickEditPage()
    {
        getTest().clickLinkWithLocator("//a[string() = 'Edit']");
    }

    @Override
    public void clickEditPageInWikiSyntaxEditor()
    {
        // In order for this method to work both in view and edit modes we have to locate the link by its text.
        getTest().clickLinkWithText("Wiki");
    }

    @Override
    public void clickEditPageInWysiwyg()
    {
        // In order for this method to work both in view and edit modes we have to locate the link by its text.
        getTest().clickLinkWithText("WYSIWYG");
    }

    @Override
    public void clickEditPageInlineForm()
    {
        getTest().clickLinkWithText("Inline form");
    }

    @Override
    public void clickEditPageAccessRights()
    {
        getTest().clickLinkWithText("Access Rights");
    }

    public void clickDeletePage()
    {
        getTest().clickLinkWithLocator("//a[string() = 'Delete']");
    }

    public void clickCopyPage()
    {
        getTest().clickLinkWithLocator("//a[string() = 'Copy']");
    }

    @Override
    public void clickShowComments()
    {
        getTest().clickLinkWithLocator("//a[@id = 'tmShowComments']", false);
    }

    @Override
    public void clickShowAttachments()
    {
        getTest().clickLinkWithLocator("//a[@id = 'tmShowAttachments']", false);
    }

    @Override
    public void clickShowHistory()
    {
        getTest().clickLinkWithLocator("//a[@id = 'tmShowHistory']", false);
    }

    @Override
    public void clickShowInformation()
    {
        getTest().clickLinkWithLocator("//a[@id = 'tmShowInformation']", false);
    }

    public void clickEditPreview()
    {
        getTest().submit("xpath=//input[@name='formactionpreview' or @name='action_preview']");
    }

    public void clickEditSaveAndContinue()
    {
        if (getTest().isElementPresent("xpath=//input[@name='formactionsac']")) {
            getTest().submit("xpath=//input[@name='formactionsac']");
        } else {
            getTest().submit("xpath=//input[@name='action_saveandcontinue']", false);
            getTest().waitForNotificationSuccessMessage("Saved");
        }
    }

    public void clickEditCancelEdition()
    {
        getTest().submit("xpath=//input[@name='formactioncancel' or @name='action_cancel']");
    }

    public void clickEditSaveAndView()
    {
        getTest().submit("xpath=//input[@name='formactionsave' or @name='action_save' or @name='action_propupdate']");
    }

    @Override
    public void clickEditAddProperty()
    {
        getTest().getSelenium().click("//input[@value = 'Add']");
        getTest().waitForNotificationSuccessMessage("Property added");
    }

    @Override
    public void clickEditAddObject()
    {
        getTest().getSelenium().click("//input[@value = 'Add']");
        getTest().waitForNotificationSuccessMessage("Object created");
    }

    public boolean isAuthenticated()
    {
        return !getTest().isElementPresent("headerlogin") && !getTest().isElementPresent("headerregister");
    }

    @Override
    public boolean isAuthenticated(String username)
    {
        return getTest().isElementPresent("//a[@id='headeruser' and contains(@href, 'XWiki/" + username + "')]");
    }

    @Override
    public boolean isAuthenticationMenuPresent()
    {
        return getTest().isElementPresent("headerlogin") || getTest().isElementPresent("headerlogout");
    }

    public void logout()
    {
        Assert.assertTrue("User wasn't authenticated.", isAuthenticated());
        getTest().clickLinkWithLocator("headerlogout");
        Assert.assertFalse("The user is still authenticated after a logout.", isAuthenticated());
    }

    public void login(String username, String password, boolean rememberme)
    {
        getTest().open("Main", "WebHome");

        if (isAuthenticated()) {
            logout();
        }

        clickLogin();

        getTest().setFieldValue("j_username", username);
        getTest().setFieldValue("j_password", password);
        if (rememberme) {
            getTest().checkField("rememberme");
        }
        getTest().submit();

        Assert.assertTrue("User has not been authenticated", isAuthenticated());
    }

    public void loginAsAdmin()
    {
        // First verify if the logged in user is not already the Administrator. That'll save us execution time.
        if (!isAuthenticated("Admin")) {
            login("Admin", "admin", false);
        }
    }

    public void clickLogin()
    {
        getTest().clickLinkWithLocator("headerlogin");
        assertIsLoginPage();
    }

    protected void assertIsLoginPage()
    {
        getTest().assertElementPresent("loginForm");
        getTest().assertElementPresent("j_username");
        getTest().assertElementPresent("j_password");
        getTest().assertElementPresent("rememberme");
    }

    public void clickRegister()
    {
        getTest().clickLinkWithLocator("headerregister");
        assertIsRegisterPage();
    }

    protected void assertIsRegisterPage()
    {
        // tests that the register form exists
        getTest().assertElementPresent("register");
        getTest().assertElementPresent("register_first_name");
        getTest().assertElementPresent("register_last_name");
        getTest().assertElementPresent("xwikiname");
        getTest().assertElementPresent("register_password");
        getTest().assertElementPresent("register2_password");
        getTest().assertElementPresent("register_email");
    }

    public String getEditorSyntax()
    {
        return getTest().getFieldValue("xwikidocsyntaxinput2");
    }

    public void setEditorSyntax(String syntax)
    {
        if (!syntax.equals(getEditorSyntax())) {
            getTest().getSelenium().select("name=syntaxId", "value=" + syntax);
            clickEditSaveAndContinue();
            getTest().getSelenium().refresh();
            getTest().waitPage();
        }
    }

    public void editInWikiEditor(String space, String page)
    {
        getTest().open("/xwiki/bin/edit/" + space + "/" + page + "?editor=wiki");
    }

    public void editInWikiEditor(String space, String page, String syntax)
    {
        editInWikiEditor(space, page);
        setEditorSyntax(syntax);
    }

    // For WYSIWYG editor

    public void editInWysiwyg(String space, String page)
    {
        getTest().open("/xwiki/bin/edit/" + space + "/" + page + "?editor=wysiwyg");
    }

    public void editInWysiwyg(String space, String page, String syntax)
    {
        editInWysiwyg(space, page);
        setEditorSyntax(syntax);
    }

    public void clearWysiwygContent()
    {
        getTest().waitForCondition("window.tinyMCE.setContent(\"\"); true");
    }

    public void typeInWysiwyg(String text)
    {
        sendKeysToTinyMCE(text);
    }

    public void typeInWiki(String text)
    {
        getTest().getSelenium().type(WIKI_LOCATOR_FOR_KEY_EVENTS, text);
    }

    public void typeEnterInWysiwyg()
    {
        sendKeysToTinyMCE(Keys.ENTER);
    }

    public void typeShiftEnterInWysiwyg()
    {
        sendKeysToTinyMCE(Keys.chord(Keys.SHIFT, Keys.ENTER));
    }

    private void sendKeysToTinyMCE(CharSequence keysToSend)
    {
        WebDriver driver = getDriver();
        String windowHandle = driver.getWindowHandle();
        try {
            WebElement iframe = driver.findElement(By.className("mceEditorIframe"));
            driver.switchTo().frame(iframe).switchTo().activeElement().sendKeys(keysToSend);
        } finally {
            driver.switchTo().window(windowHandle);
        }
    }

    public void clickWysiwygUnorderedListButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Unordered list']", false);
    }

    public void clickWysiwygOrderedListButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Ordered list']", false);
    }

    public void clickWysiwygIndentButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Indent']", false);
    }

    public void clickWysiwygOutdentButton()
    {
        getTest().clickLinkWithLocator("//img[@title='Outdent']", false);
    }

    public void clickWikiBoldButton()
    {
        getTest().clickLinkWithXPath("//img[@title='Bold']", false);
    }

    public void clickWikiItalicsButton()
    {
        getTest().clickLinkWithXPath("//img[@title='Italics']", false);
    }

    public void clickWikiUnderlineButton()
    {
        getTest().clickLinkWithXPath("//img[@title='Underline']", false);
    }

    public void clickWikiLinkButton()
    {
        getTest().clickLinkWithXPath("//img[@title='Internal Link']", false);
    }

    public void clickWikiHRButton()
    {
        getTest().clickLinkWithXPath("//img[@title='Horizontal ruler']", false);
    }

    public void clickWikiImageButton()
    {
        getTest().clickLinkWithXPath("//img[@title='Attached Image']", false);
    }

    public void clickWikiSignatureButton()
    {
        getTest().clickLinkWithXPath("//img[@title='Signature']", false);
    }

    @Override
    public void clickAdministerWiki()
    {
        // Todo: unsupported skin
    }

    public void assertWikiTextGeneratedByWysiwyg(String text)
    {
        clickEditPageInWikiSyntaxEditor();
        Assert.assertEquals(text, getTest().getSelenium().getValue("content"));
    }

    public void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception
    {
        getTest().getSelenium().selectFrame("mce_editor_0");
        Assert.assertTrue(getTest().getSelenium().isElementPresent("xpath=/html/body/" + xpath));
        getTest().getSelenium().selectFrame("relative=top");
    }

    public void assertGeneratedHTML(String xpath) throws Exception
    {
        Assert.assertTrue(getTest().getSelenium().isElementPresent("xpath=//div[@id='xwikicontent']/" + xpath));
    }

    public void openAdministrationPage()
    {
        getTest().open("XWiki", "XWikiPreferences", "admin");
    }

    public void openAdministrationSection(String section)
    {
        this.openAdministrationPage();

        getTest().clickLinkWithLocator("//li[@class='" + section + "']/a");
    }

    public void pressKeyboardShortcut(String shortcut, boolean withCtrlModifier, boolean withAltModifier,
        boolean withShiftModifier) throws InterruptedException
    {
        Actions actions = getDriver().createActions();
        if (withCtrlModifier) {
            actions = actions.keyDown(Keys.CONTROL);
        }
        if (withAltModifier) {
            actions = actions.keyDown(Keys.ALT);
        }
        if (withShiftModifier) {
            actions = actions.keyDown(Keys.SHIFT);
        }
        actions = actions.sendKeys(shortcut);
        if (withCtrlModifier) {
            actions = actions.keyUp(Keys.CONTROL);
        }
        if (withAltModifier) {
            actions = actions.keyUp(Keys.ALT);
        }
        if (withShiftModifier) {
            actions = actions.keyUp(Keys.SHIFT);
        }
        actions.perform();
    }

    @Override
    public boolean copyPage(String spaceName, String pageName, String targetSpaceName, String targetPageName)
    {
        // Open the page in Wiki edit mode because it's faster and because we want to make sure the page actions are
        // available (e.g. they may not be available in the administration mode).
        getTest().editInWikiEditor(spaceName, pageName);
        clickCopyPage();
        getTest().getSelenium().type("targetSpaceName", targetSpaceName);
        getTest().getSelenium().type("targetPageName", targetPageName);
        getTest().clickLinkWithLocator("//input[@value='Copy']");
        return getTest().getSelenium().isTextPresent("successfully copied to");
    }

    protected XWikiWebDriver getDriver()
    {
        return (XWikiWebDriver) ((WebDriverBackedSelenium) this.test.getSelenium()).getWrappedDriver();
    }
}
