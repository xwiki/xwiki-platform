/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.it.framework;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import org.custommonkey.xmlunit.XMLTestCase;
import org.openqa.selenium.server.SeleniumServer;

public class AbstractXWikiTestCase extends XMLTestCase implements SkinExecutor
{
    private SkinExecutor skinExecutor;

    private static final String BASE_URL = "http://localhost:8080";

    private Selenium selenium;

    public void setSkinExecutor(SkinExecutor skinExecutor)
    {
        this.skinExecutor = skinExecutor;
    }

    public SkinExecutor getSkinExecutor()
    {
        if (this.skinExecutor == null) {
            throw new RuntimeException("Skin executor hasn't been initialized. Make sure to wrap "
                + "your test in a " + XWikiTestSuite.class.getName() + " class and call "
                + " addTestSuite(Class testClass, SkinExecutor skinExecutor).");
        }
        return this.skinExecutor;
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        // Get the browser to test with from a System property set by the Maven2 build.
        // Defaults to Firefox.
        String browser = System.getProperty("browser", "*firefox");

        this.selenium =
            new DefaultSelenium("localhost", SeleniumServer.DEFAULT_PORT, browser, BASE_URL);
        this.selenium.start();
    }

    public void tearDown() throws Exception
    {
        getSelenium().stop();
    }

    public Selenium getSelenium()
    {
        return this.selenium;
    }

    // Convenience methods wrapping Selenium

    public void open(String url)
    {
        getSelenium().open(url);
    }

    public String getTitle()
    {
        return getSelenium().getTitle();
    }

    public void assertPage(String title)
    {
        assertEquals(title, getTitle());
    }

    public boolean isElementPresent(String locator)
    {
        return getSelenium().isElementPresent(locator);
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
        assertTrue("[" + text + "] isn't present.", getSelenium().isTextPresent(text));
    }

    public void assertElementPresent(String elementLocator)
    {
        assertTrue("[" + elementLocator + "] isn't present.", isElementPresent(elementLocator));
    }

    public void waitPage()
    {
        waitPage(180000);
    }

    public void waitPage(int nbMillisecond)
    {
        getSelenium().waitForPageToLoad(String.valueOf(nbMillisecond));
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
            "selenium.browserbot.getCurrentWindow().document.getElementById(\"" + fieldName
                + "\").value");
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

    // SkinExecutor methods

    public void clickDeletePage()
    {
        getSkinExecutor().clickDeletePage();
    }

    public void clickEditPreview()
    {
        getSkinExecutor().clickEditPreview();
    }

    public void clickEditSaveAndContinue()
    {
        getSkinExecutor().clickEditSaveAndContinue();
    }

    public void clickEditCancelEdition()
    {
        getSkinExecutor().clickEditCancelEdition();
    }

    public void clickEditSaveAndView()
    {
        getSkinExecutor().clickEditSaveAndView();
    }

    public boolean isAuthenticated()
    {
        return getSkinExecutor().isAuthenticated();
    }

    public void logout()
    {
        getSkinExecutor().logout();
    }

    public void login(String username, String password, boolean rememberme)
    {
        getSkinExecutor().login(username, password, rememberme);
    }

    public void loginAsAdmin()
    {
        getSkinExecutor().loginAsAdmin();
    }

    public void clickLogin()
    {
        getSkinExecutor().clickLogin();
    }

    public void editInWysiwyg(String space, String page)
    {
        getSkinExecutor().editInWysiwyg(space, page);
    }

    public void clearWysiwygContent()
    {
        getSkinExecutor().clearWysiwygContent();
    }

    public void typeInWysiwyg(String text)
    {
        getSkinExecutor().typeInWysiwyg(text);
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

    public void assertWikiTextGeneratedByWysiwyg(String text)
    {
        getSkinExecutor().assertWikiTextGeneratedByWysiwyg(text);
    }

    public void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception
    {
        getSkinExecutor().assertHTMLGeneratedByWysiwyg(xpath);
    }
}
