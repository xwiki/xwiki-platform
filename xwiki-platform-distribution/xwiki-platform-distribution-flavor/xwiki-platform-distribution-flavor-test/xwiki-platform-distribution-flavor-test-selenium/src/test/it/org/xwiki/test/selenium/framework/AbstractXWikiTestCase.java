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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.AbstractTest;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;
import com.thoughtworks.selenium.Wait;
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;

/**
 * All XWiki Selenium tests must extend this class.
 * 
 * @version $Id$
 */
public abstract class AbstractXWikiTestCase extends AbstractTest implements SkinExecutor
{
    public static final String BASEDIR = System.getProperty("basedir");

    public static final String DOC = "selenium.browserbot.getCurrentWindow().document.";

    private static final int WAIT_TIME = 30000;

    private SkinExecutor skinExecutor = new FlamingoSkinExecutor(this);

    private Selenium selenium;

    public void setSkinExecutor(SkinExecutor skinExecutor)
    {
        this.skinExecutor = skinExecutor;
    }

    public SkinExecutor getSkinExecutor()
    {
        return this.skinExecutor;
    }

    public Selenium getSelenium()
    {
        if (this.selenium == null) {
            String baseURL = "http://localhost:" + System.getProperty("xwikiPort", "8080");
            this.selenium = new WebDriverBackedSelenium(getDriver(), baseURL);
        }
        return this.selenium;
    }

    @Before
    public void setUp()
    {
        loginAsAdmin();
    }

    // Convenience methods wrapping Selenium

    public void open(String url)
    {
        getSelenium().open(url);
    }

    public void open(String space, String page)
    {
        open(getUrl(space, page));
    }

    public void open(String space, String page, String action)
    {
        open(getUrl(space, page, action));
    }

    public void open(String space, String page, String action, String queryString)
    {
        open(getUrl(space, page, action, queryString));
    }

    public String getTitle()
    {
        return getSelenium().getTitle();
    }

    public void assertPage(String space, String page)
    {
        Assert.assertTrue(getTitle().matches(".*\\(" + space + "." + page + "\\) - XWiki"));
    }

    /**
     * Visits the specified page and checks if it exists, coming back to the current page.
     * 
     * @param space the space name
     * @param page the page name
     * @return {@code true} if the specified page exists
     */
    public boolean isExistingPage(String space, String page)
    {
        String saveUrl = getSelenium().getLocation();

        open(getUrl(space, page));
        boolean exists = isExistingPage();

        // Restore original URL
        open(saveUrl);

        return exists;
    }

    /**
     * @return {@code true} if we are on an existing page, {@code false} otherwise
     */
    public boolean isExistingPage()
    {
        return !getSelenium().isTextPresent("The requested page could not be found.");
    }

    public void assertTitle(String title)
    {
        Assert.assertEquals(title, getTitle());
    }

    public boolean isElementPresent(String locator)
    {
        return getSelenium().isElementPresent(locator);
    }

    public boolean isElementPresentWithoutWaiting(By by)
    {
        return getDriver().hasElementWithoutWaiting(by);
    }

    public boolean isLinkPresent(String text)
    {
        return isElementPresent("link=" + text);
    }

    public void clickLinkWithText(String text)
    {
        clickLinkWithText(text, true);
    }

    public void assertTextPresent(String text)
    {
        Assert.assertTrue("[" + text + "] isn't present.", getSelenium().isTextPresent(text));
    }

    public void assertTextNotPresent(String text)
    {
        Assert.assertFalse("[" + text + "] is present.", getSelenium().isTextPresent(text));
    }

    public void assertElementPresent(String elementLocator)
    {
        Assert.assertTrue("[" + elementLocator + "] isn't present.", isElementPresent(elementLocator));
    }

    public void assertElementNotPresent(String elementLocator)
    {
        Assert.assertFalse("[" + elementLocator + "] is present.", isElementPresent(elementLocator));
    }

    public void waitPage()
    {
        waitPage(WAIT_TIME);
    }

    /**
     * @deprecated use {@link #waitPage()} instead
     */
    @Deprecated
    public void waitPage(int nbMillisecond)
    {
        getSelenium().waitForPageToLoad(String.valueOf(nbMillisecond));
    }

    public void createPage(String space, String page, String content)
    {
        createPage(space, page, content, null);
    }

    public void createPage(String space, String page, String content, String syntax)
    {
        // If the page already exists, delete it first
        deletePage(space, page);
        if (syntax == null) {
            editInWikiEditor(space, page);
        } else {
            editInWikiEditor(space, page, syntax);
        }
        setFieldValue("content", content);
        clickEditSaveAndView();
    }

    public void deletePage(String space, String page)
    {
        open(space, page, "delete", "confirm=1");
    }

    public void restorePage(String space, String page)
    {
        open(space, page, "view");
        if (getSelenium().isTextPresent("Restore")) {
            clickLinkWithText("Restore", true);
        }
    }

    public void clickLinkWithLocator(String locator)
    {
        clickLinkWithLocator(locator, true);
    }

    public void clickLinkWithLocator(String locator, boolean wait)
    {
        assertElementPresent(locator);
        getSelenium().click(locator);
        if (wait) {
            waitPage();
        }
    }

    public void clickLinkWithText(String text, boolean wait)
    {
        clickLinkWithLocator("link=" + text, wait);
    }

    public boolean isChecked(String locator)
    {
        return getSelenium().isChecked(locator);
    }

    public String getFieldValue(String fieldName)
    {
        // Note: We could use getSelenium().getvalue() here. However getValue() is stripping spaces
        // and some of our tests verify that there are leading spaces/empty lines.
        return getSelenium().getEval(
            "selenium.browserbot.getCurrentWindow().document.getElementById(\"" + fieldName + "\").value");
    }

    public void setFieldValue(String fieldName, String value)
    {
        getSelenium().type(fieldName, value);
    }

    public void checkField(String locator)
    {
        getSelenium().check(locator);
    }

    public void submit()
    {
        clickLinkWithXPath("//input[@type='submit']");
    }

    public void submit(String locator)
    {
        clickLinkWithLocator(locator);
    }

    public void submit(String locator, boolean wait)
    {
        clickLinkWithLocator(locator, wait);
    }

    public void clickLinkWithXPath(String xpath)
    {
        clickLinkWithXPath(xpath, true);
    }

    public void clickLinkWithXPath(String xpath, boolean wait)
    {
        clickLinkWithLocator("xpath=" + xpath, wait);
    }

    public void waitForCondition(String condition)
    {
        getSelenium().waitForCondition(condition, "" + WAIT_TIME);
    }

    public void waitForTextPresent(final String elementLocator, final String expectedValue)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().getText(elementLocator).equals(expectedValue);
            }
        }.wait(getSelenium().isElementPresent(elementLocator) ? "Element [" + elementLocator + "] not found"
            : "Element [" + elementLocator + "] found but it doesn't have the expected value [" + expectedValue + "]");
    }

    public void waitForTextContains(final String elementLocator, final String containsValue)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().getText(elementLocator).indexOf(containsValue) > -1;
            }
        }.wait(getSelenium().isElementPresent(elementLocator) ? "Element [" + elementLocator + "] not found"
            : "Element [" + elementLocator + "] found but it doesn't contain the expected value [" + containsValue
                + "]");
    }

    public void waitForBodyContains(final String containsValue)
    {
        new Wait()
        {
            public boolean until()
            {
                try {
                    return getSelenium().getBodyText().indexOf(containsValue) > -1;
                } catch (SeleniumException e) {
                    // The page might not be loaded yet and so the BODY element is missing. Try again later.
                    return false;
                }
            }
        }.wait("Body text doesn't contain the value [" + containsValue + "]");
    }

    public void waitForElement(final String elementLocator)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().isElementPresent(elementLocator);
            }
        }.wait("element [" + elementLocator + "] not found");
    }

    /**
     * Waits until an alert message appears or the timeout expires. You can use {@link Selenium#getAlert()} to assert
     * the alert message afterwards.
     */
    public void waitForAlert()
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().isAlertPresent();
            }
        }.wait("The alert didn't appear.");
    }

    /**
     * Waits until a confirmation message appears or the timeout expires. You can use {@link Selenium#getConfirmation()}
     * to assert the confirmation message afterwards.
     */
    public void waitForConfirmation()
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().isConfirmationPresent();
            }
        }.wait("The confirmation didn't appear.");
    }

    /**
     * Waits for a notification message of the specified type with the given message to be displayed.
     * 
     * @param level the notification type (one of error, warning, done)
     * @param message the notification message
     */
    private void waitForNotificationMessage(String level, String message)
    {
        String xpath = String.format("//div[contains(@class,'xnotification-%s') and contains(.,'%s')]", level, message);
        waitForElement(xpath);
        // In order to improve test speed, clicking on the notification will make it disappear. This also ensures that
        // this method always waits for the last notification message of the specified level.
        try {
            // The notification message may disappear before we get to click on it.
            getSelenium().click(xpath);
        } catch (Exception e) {
            // Ignore.
        }
    }

    public void waitForNotificationErrorMessage(String message)
    {
        waitForNotificationMessage("error", message);
    }

    public void waitForNotificationWarningMessage(String message)
    {
        waitForNotificationMessage("warning", message);
    }

    public void waitForNotificationSuccessMessage(String message)
    {
        waitForNotificationMessage("done", message);
    }

    public void clickButtonAndContinue(String locator)
    {
        submit(locator, false);
        waitForNotificationSuccessMessage("");
    }

    @Override
    public void clickEditPage()
    {
        getSkinExecutor().clickEditPage();
    }

    @Override
    public void clickEditPageInWikiSyntaxEditor()
    {
        getSkinExecutor().clickEditPageInWikiSyntaxEditor();
    }

    @Override
    public void clickEditPageInWysiwyg()
    {
        getSkinExecutor().clickEditPageInWysiwyg();
    }

    @Override
    public void clickEditPageAccessRights()
    {
        getSkinExecutor().clickEditPageAccessRights();
    }

    @Override
    public void clickEditPageInlineForm()
    {
        getSkinExecutor().clickEditPageInlineForm();
    }

    @Override
    public void clickDeletePage()
    {
        getSkinExecutor().clickDeletePage();
    }

    @Override
    public void clickCopyPage()
    {
        getSkinExecutor().clickCopyPage();
    }

    @Override
    public void clickShowComments()
    {
        getSkinExecutor().clickShowComments();
    }

    @Override
    public void clickShowAttachments()
    {
        getSkinExecutor().clickShowAttachments();
    }

    @Override
    public void clickShowHistory()
    {
        getSkinExecutor().clickShowHistory();
    }

    @Override
    public void clickShowInformation()
    {
        getSkinExecutor().clickShowInformation();
    }

    @Override
    public void clickEditPreview()
    {
        getSkinExecutor().clickEditPreview();
    }

    @Override
    public void clickEditSaveAndContinue()
    {
        getSkinExecutor().clickEditSaveAndContinue();
    }

    @Override
    public void clickEditCancelEdition()
    {
        getSkinExecutor().clickEditCancelEdition();
    }

    @Override
    public void clickEditSaveAndView()
    {
        getSkinExecutor().clickEditSaveAndView();
    }

    /**
     * Clicks on the add property button in the class editor. As a result the specified property is added to the edited
     * class and the class is saved. This method waits for the class to be saved.
     */
    @Override
    public void clickEditAddProperty()
    {
        getSkinExecutor().clickEditAddProperty();
    }

    /**
     * Clicks on the add object button in the object editor. As a result an object of the specified class is added to
     * the edited document and the document is saved. This method waits for the document to be saved.
     */
    @Override
    public void clickEditAddObject()
    {
        getSkinExecutor().clickEditAddObject();
    }

    @Override
    public boolean isAuthenticated()
    {
        return getSkinExecutor().isAuthenticated();
    }

    @Override
    public boolean isAuthenticated(String username)
    {
        return getSkinExecutor().isAuthenticated(username);
    }

    @Override
    public boolean isAuthenticationMenuPresent()
    {
        return getSkinExecutor().isAuthenticationMenuPresent();
    }

    @Override
    public void logout()
    {
        getSkinExecutor().logout();
        recacheSecretToken();
    }

    @Override
    public void login(String username, String password, boolean rememberme)
    {
        getSkinExecutor().login(username, password, rememberme);
        recacheSecretToken();
    }

    @Override
    public void loginAsAdmin()
    {
        getSkinExecutor().loginAsAdmin();
        recacheSecretToken();
        // Set the Admin user as an advanced user
        getUtil().updateObject("XWiki", "Admin", "XWiki.XWikiUsers", 0, "usertype", "Advanced");
    }

    /**
     * If the user is not logged in already and if the specified user page exists, it is logged in. Otherwise the user
     * is registered first and then the login is executed.
     * 
     * @param username the user name to login as. If the user is to be created, this will also be used as the user first
     *            name while the user last name will be left blank
     * @param password the password of the user
     * @param rememberMe whether the login should be remembered or not
     */
    public void loginAndRegisterUser(String username, String password, boolean rememberMe)
    {
        if (!isAuthenticationMenuPresent()) {
            // navigate to the main page
            open("Main", "WebHome");
        }

        // if user is already authenticated, don't login
        if (isAuthenticated(username)) {
            return;
        }

        // try to go to the user page
        open("XWiki", username);
        // if user page doesn't exist, register the user first
        boolean exists = !getSelenium().isTextPresent("The requested page could not be found.");
        if (!exists) {
            if (isAuthenticated()) {
                logout();
            }
            clickRegister();
            fillRegisterForm(username, "", username, password, "");
            submit();
            // assume registration was done successfully, otherwise the register test should fail too
        }

        login(username, password, rememberMe);
    }

    public void fillRegisterForm(String firstName, String lastName, String username, String password, String email)
    {
        setFieldValue("register_first_name", firstName);
        setFieldValue("register_last_name", lastName);
        setFieldValue("xwikiname", username);
        setFieldValue("register_password", password);
        setFieldValue("register2_password", password);
        setFieldValue("register_email", email);
    }

    @Override
    public void clickLogin()
    {
        getSkinExecutor().clickLogin();
    }

    @Override
    public void clickRegister()
    {
        getSkinExecutor().clickRegister();
    }

    public String getEditorSyntax()
    {
        return getSkinExecutor().getEditorSyntax();
    }

    public void setEditorSyntax(String syntax)
    {
        getSkinExecutor().setEditorSyntax(syntax);
    }

    public void editInWikiEditor(String space, String page)
    {
        getSkinExecutor().editInWikiEditor(space, page);
    }

    public void editInWikiEditor(String space, String page, String syntax)
    {
        getSkinExecutor().editInWikiEditor(space, page, syntax);
    }

    public void editInWysiwyg(String space, String page)
    {
        getSkinExecutor().editInWysiwyg(space, page);
    }

    public void editInWysiwyg(String space, String page, String syntax)
    {
        getSkinExecutor().editInWysiwyg(space, page, syntax);
    }

    public void clearWysiwygContent()
    {
        getSkinExecutor().clearWysiwygContent();
    }

    public void keyPressAndWait(String element, String keycode) throws InterruptedException
    {
        getSelenium().keyPress(element, keycode);
        waitPage();
    }

    public void typeInWysiwyg(String text)
    {
        getSkinExecutor().typeInWysiwyg(text);
    }

    public void typeInWiki(String text)
    {
        getSkinExecutor().typeInWiki(text);
    }

    public void typeEnterInWysiwyg()
    {
        getSkinExecutor().typeEnterInWysiwyg();
    }

    public void typeShiftEnterInWysiwyg()
    {
        getSkinExecutor().typeShiftEnterInWysiwyg();
    }

    public void clickWysiwygUnorderedListButton()
    {
        getSkinExecutor().clickWysiwygUnorderedListButton();
    }

    public void clickWysiwygOrderedListButton()
    {
        getSkinExecutor().clickWysiwygOrderedListButton();
    }

    public void clickWysiwygIndentButton()
    {
        getSkinExecutor().clickWysiwygIndentButton();
    }

    public void clickWysiwygOutdentButton()
    {
        getSkinExecutor().clickWysiwygOutdentButton();
    }

    public void clickWikiBoldButton()
    {
        getSkinExecutor().clickWikiBoldButton();
    }

    public void clickWikiItalicsButton()
    {
        getSkinExecutor().clickWikiItalicsButton();
    }

    public void clickWikiUnderlineButton()
    {
        getSkinExecutor().clickWikiUnderlineButton();
    }

    public void clickWikiLinkButton()
    {
        getSkinExecutor().clickWikiLinkButton();
    }

    public void clickWikiHRButton()
    {
        getSkinExecutor().clickWikiHRButton();
    }

    public void clickWikiImageButton()
    {
        getSkinExecutor().clickWikiImageButton();
    }

    public void clickWikiSignatureButton()
    {
        getSkinExecutor().clickWikiSignatureButton();
    }

    public void assertWikiTextGeneratedByWysiwyg(String text)
    {
        getSkinExecutor().assertWikiTextGeneratedByWysiwyg(text);
    }

    public void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception
    {
        getSkinExecutor().assertHTMLGeneratedByWysiwyg(xpath);
    }

    public void assertGeneratedHTML(String xpath) throws Exception
    {
        getSkinExecutor().assertGeneratedHTML(xpath);
    }

    public void openAdministrationPage()
    {
        getSkinExecutor().openAdministrationPage();
    }

    public void openAdministrationSection(String section)
    {
        getSkinExecutor().openAdministrationSection(section);
    }

    public String getUrl(String space, String doc)
    {
        return getUrl(space, doc, "view");
    }

    public String getUrl(String space, String doc, String action)
    {
        return getUrl(space, doc, action, null);
    }

    public String getUrl(String space, String doc, String action, String queryString)
    {
        StringBuilder builder = new StringBuilder("/xwiki/bin/");
        builder.append(action);
        builder.append('/');
        builder.append(space);
        builder.append('/');
        builder.append(doc);

        boolean needToAddSecretToken = !("view".equals(action) || "register".equals(action));
        boolean needToAddQuery = queryString != null && queryString.length() > 0;
        if (needToAddSecretToken || needToAddQuery) {
            builder.append('?');
        }
        if (needToAddSecretToken) {
            builder.append("form_token=");
            builder.append(getSecretToken());
            builder.append('&');
        }
        if (needToAddQuery) {
            builder.append(queryString);
        }
        return builder.toString();
    }

    public void pressKeyboardShortcut(String shortcut, boolean withCtrlModifier, boolean withAltModifier,
        boolean withShiftModifier) throws InterruptedException
    {
        getSkinExecutor().pressKeyboardShortcut(shortcut, withCtrlModifier, withAltModifier, withShiftModifier);
    }

    /**
     * Set global xwiki configuration options (as if the xwiki.cfg file had been modified). This is useful for testing
     * configuration options.
     * 
     * @param configuration the configuration in {@link Properties} format. For example "param1=value2\nparam2=value2"
     * @throws IOException if an error occurs while parsing the configuration
     */
    public void setXWikiConfiguration(String configuration) throws IOException
    {
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(configuration.getBytes()));
        StringBuffer sb = new StringBuffer();

        // Since we don't have access to the XWiki object from Selenium tests and since we don't want to restart XWiki
        // with a different xwiki.cfg file for each test that requires a configuration change, we use the following
        // trick: We create a document and we access the XWiki object with a Velocity script inside that document.
        for (Entry<Object, Object> param : properties.entrySet()) {
            sb.append("{{velocity}}$xwiki.xWiki.config.setProperty('").append(param.getKey()).append("', '")
                .append(param.getValue()).append("')").append("\n{{/velocity}}");
        }
        editInWikiEditor("Test", "XWikiConfigurationPageForTest", "xwiki/2.1");
        setFieldValue("content", sb.toString());
        clickEditSaveAndView();
    }

    @Override
    public boolean copyPage(String spaceName, String pageName, String targetSpaceName, String targetPageName)
    {
        return getSkinExecutor().copyPage(spaceName, pageName, targetSpaceName, targetPageName);
    }

    /**
     * Waits for the specified live table to load.
     * 
     * @param id the live table id
     */
    public void waitForLiveTable(String id)
    {
        waitForElement("//*[@id = '" + id + "-ajax-loader' and @class = 'xwiki-livetable-loader hidden']");
    }

    /**
     * (Re)-cache the secret token used for CSRF protection. A user with edit rights on Main.WebHome must be logged in.
     * This method must be called before {@link #getSecretToken()} is called and after each re-login.
     * 
     * @since 3.2M1
     * @see #getSecretToken()
     */
    public void recacheSecretToken()
    {
        getUtil().recacheSecretToken();
    }

    /**
     * Get the secret token used for CSRF protection. Remember to call {@link #recacheSecretToken()} first.
     * 
     * @return anti-CSRF secret token, or empty string if the token is not cached
     * @since 3.2M1
     * @see #recacheSecretToken()
     */
    public String getSecretToken()
    {
        return getUtil().getSecretToken();
    }

    /**
     * Drags and drops the source element on top of the target element.
     * 
     * @param sourceLocator locates the element to be dragged
     * @param targetLocator locates the element where to drop the dragged element
     */
    public void dragAndDrop(By sourceLocator, By targetLocator)
    {
        WebDriver driver = getDriver();
        WebElement source = driver.findElement(sourceLocator);
        WebElement target = driver.findElement(targetLocator);
        new Actions(driver).dragAndDrop(source, target).build().perform();
    }

    /**
     * Makes sure the specified element is not covered by the floating menu which is displayed at the top of the window.
     * Use this method before clicking on an element that can end up beneath the floating menu.
     * 
     * @param locator an element locator
     */
    public void ensureElementIsNotCoveredByFloatingMenu(By locator)
    {
        WebDriver driver = getDriver();
        // First scroll the element into view, if needed, by moving the mouse to the top left corner of the element.
        new Actions(driver).moveToElement(driver.findElement(locator), 0, 0).perform();
        // Then scroll the page up a bit so that the element is not at the top of the window where the floating menu is.
        driver.findElement(By.xpath("//body")).sendKeys(Keys.ARROW_UP);
    }

    /**
     * Expands the object with the given number of the specified XClass. You need to call this method before editing any
     * of the properties of the specified object using the object editor because the form elements used to edit the
     * object properties have to be visible.
     * 
     * @param className the XClass name
     * @param objectNumber the object number
     */
    public void expandObject(String className, int objectNumber)
    {
        String objectContentId = String.format("xobject_%s_%s_content", className, objectNumber);
        if (!getDriver().findElement(By.id(objectContentId)).isDisplayed()) {
            // First make sure that the group of objects of the specified class is expanded.
            String objectTitleId = String.format("xobject_%s_%s_title", className, objectNumber);
            WebElement objectTitle = getDriver().findElement(By.id(objectTitleId));
            if (!objectTitle.isDisplayed()) {
                // Expand the group of objects of the specified type.
                getDriver().findElement(By.id(String.format("xclass_%s_title", className))).click();
            }
            // Expand the specified object.
            objectTitle.click();
        }
    }

    /**
     * @param elementLocator the locator used to get the desired element. e.g. By.id("someId")
     * @return true if the element is in the window's viewport, i.e. is scrolled to; false otherwise.
     * @since 6.2
     */
    public boolean isElementInView(By elementLocator)
    {
        Point elementLocation = getDriver().findElement(elementLocator).getLocation();

        int windowXLeft = Integer.parseInt(getSelenium().getEval("window.scrollX"));
        int windowYTop = Integer.parseInt(getSelenium().getEval("window.scrollY"));

        int width = Integer.parseInt(getSelenium().getEval("document.documentElement.clientWidth"));
        int height = Integer.parseInt(getSelenium().getEval("document.documentElement.clientHeight"));

        int windowXRight = windowXLeft + width;
        int windowYBottom = windowYTop + height;

        return (elementLocation.getX() >= windowXLeft && elementLocation.getX() <= windowXRight
            && elementLocation.getY() >= windowYTop && elementLocation.getY() <= windowYBottom);
    }

    /**
     * Convenience method.
     * @see #isElementInView(By)
     * @since 6.2
     */
    public void assertElementInView(By elementLocator)
    {
        Assert.assertTrue("[" + elementLocator + "] is not in view.", isElementInView(elementLocator));
    }

    public void clickAdministerWiki()
    {
        getSkinExecutor().clickAdministerWiki();
    }
}
