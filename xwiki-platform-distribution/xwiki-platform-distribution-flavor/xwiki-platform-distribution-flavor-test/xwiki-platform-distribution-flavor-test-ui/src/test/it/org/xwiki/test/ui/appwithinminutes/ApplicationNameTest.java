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
package org.xwiki.test.ui.appwithinminutes;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Tests the first step of the App Within Minutes wizard.
 *
 * @version $Id$
 * @since 3.4M1
 */
public class ApplicationNameTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    /**
     * The error message displayed when we try to create an application with an empty name.
     */
    private static final String EMPTY_APP_NAME_ERROR_MESSAGE = "Please enter the application name.";

    /**
     * The warning message displayed when we input the name of an existing application.
     */
    public static final String APP_NAME_USED_WARNING_MESSAGE = "This application already exists.";

    /**
     * Try to create an application with an empty name using the next step button.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testEmptyAppNameWithNextStepButton()
    {
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        Assert.assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Try to move to the next step without typing the application name.
        String urlBeforeClick = getDriver().getCurrentUrl();
        appCreatePage.clickNextStepButton();
        Assert.assertEquals(urlBeforeClick, getDriver().getCurrentUrl());

        // Type the application name.
        appCreatePage.setApplicationName("A");
        appCreatePage.waitForApplicationNamePreview();
        Assert.assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Clear the application name using the Backspace key.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.BACK_SPACE);
        appCreatePage.waitForApplicationNameError();
        Assert.assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Try to create the application even if the error message is displayed.
        urlBeforeClick = getDriver().getCurrentUrl();
        appCreatePage.clickNextStepButton();
        Assert.assertEquals(urlBeforeClick, getDriver().getCurrentUrl());

        // Fix the application name and move to the next step.
        appCreatePage.setApplicationName(getTestMethodName());
        appCreatePage.waitForApplicationNamePreview();
        Assert.assertEquals(getTestMethodName(), appCreatePage.clickNextStep().getDocumentTitle());
    }

    /**
     * Try to create an application with an empty name using the Enter key.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testEmptyAppNameWithEnter()
    {
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        Assert.assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Press Enter key without typing the application name.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.RETURN);
        appCreatePage.waitForApplicationNameError();
        Assert.assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Type the application name.
        appCreatePage.setApplicationName("B");
        appCreatePage.waitForApplicationNamePreview();
        Assert.assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Clear the application name using the Backspace key.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.BACK_SPACE);
        appCreatePage.waitForApplicationNameError();
        Assert.assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Try to create the application even if the error message is displayed.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.RETURN);
        Assert.assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Fix the application name and move to the next step using the Enter key.
        appCreatePage.setApplicationName(getTestMethodName());
        appCreatePage.waitForApplicationNamePreview();
        appCreatePage.getApplicationNameInput().sendKeys(Keys.RETURN);
        Assert.assertEquals(getTestMethodName(), new ViewPage().getDocumentTitle());
    }

    /**
     * Try to input the name of an existing application.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testExistingAppName()
    {
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        Assert.assertFalse(appCreatePage.getContent().contains(APP_NAME_USED_WARNING_MESSAGE));

        // Type the name of an existing space.
        appCreatePage.setApplicationName("Help");
        appCreatePage.waitForApplicationNamePreview();
        Assert.assertTrue(appCreatePage.getContent().contains(APP_NAME_USED_WARNING_MESSAGE));

        // Proceed to the next step.
        Assert.assertEquals("/Help/Code/HelpClass", appCreatePage.clickNextStep().getBreadcrumbContent());
    }
}
