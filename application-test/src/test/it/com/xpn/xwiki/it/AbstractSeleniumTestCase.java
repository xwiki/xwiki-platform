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
package com.xpn.xwiki.it;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import org.openqa.selenium.server.SeleniumServer;

/**
 * Useful methods wrapping the Selenium API and making it even easier to user. All XWiki functional
 * tests must extend this class.
 *
 * @version $Id: $ 
 */
public abstract class AbstractSeleniumTestCase extends TestCase
{
    private static final String BASE_URL = "http://localhost:8080";

    private Selenium selenium;

    public void setUp()
        throws Exception
    {
        super.setUp();

        // Get the browser to test with from a System property set by the Maven2 build.
        String browser = System.getProperty("browser");
        if (browser.trim().length() == 0) {
            browser = "*firefox";
        }

        this.selenium =
            new DefaultSelenium("localhost", SeleniumServer.DEFAULT_PORT, browser, BASE_URL);
        this.selenium.start();
    }

    public void tearDown()
        throws Exception
    {
        getSelenium().stop();
    }

    public Selenium getSelenium()
    {
        return this.selenium;
    }

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

    public boolean isAuthenticated()
    {
        return !(isElementPresent("headerlogin") && isElementPresent("headerregister"));
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

    public void logout()
    {
        assertTrue("User wasn't authenticated.", isAuthenticated());
        clickLinkWithLocator("headerlogout");
        assertFalse("The user is always authenticated after a logout.", isAuthenticated());
    }

    public void goToLoginPage()
    {
        clickLinkWithLocator("headerlogin");
        assertLoginPage();
    }

    public boolean isChecked(String locator)
    {
        return getSelenium().isChecked(locator);
    }

    public void assertLoginPage()
    {
        assertElementPresent("loginForm");
        assertElementPresent("j_username");
        assertElementPresent("j_password");
        assertFalse(isChecked("rememberme"));
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
}
