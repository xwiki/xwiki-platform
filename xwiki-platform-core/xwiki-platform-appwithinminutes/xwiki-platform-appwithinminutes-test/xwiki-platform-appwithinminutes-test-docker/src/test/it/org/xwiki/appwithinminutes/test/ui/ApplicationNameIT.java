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
package org.xwiki.appwithinminutes.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationTemplateProviderEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.appwithinminutes.test.po.ApplicationCreatePage.APP_NAME_USED_WARNING_MESSAGE;
import static org.xwiki.appwithinminutes.test.po.ApplicationCreatePage.EMPTY_APP_NAME_ERROR_MESSAGE;

/**
 * Tests the first step of the App Within Minutes wizard.
 *
 * @version $Id$
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since 
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class ApplicationNameIT
{
    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Try to create an application with an empty name using the next step button.
     */
    @Order(1)
    @Test
    void testEmptyAppNameWithNextStepButton(TestUtils testUtils, TestReference testReference)
    {
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Try to move to the next step without typing the application name.
        String urlBeforeClick = testUtils.getDriver().getCurrentUrl();
        appCreatePage.clickNextStepButton();
        assertEquals(urlBeforeClick, testUtils.getDriver().getCurrentUrl());

        // Type the application name.
        appCreatePage.setApplicationName("A");
        assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Clear the application name using the Backspace key.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.BACK_SPACE);
        appCreatePage.waitForApplicationNameError();
        assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Try to create the application even if the error message is displayed.
        urlBeforeClick = testUtils.getDriver().getCurrentUrl();
        appCreatePage.clickNextStepButton();
        assertEquals(urlBeforeClick, testUtils.getDriver().getCurrentUrl());

        // Fix the application name and move to the next step.
        String appName = testReference.getLastSpaceReference().getName();
        appCreatePage.setApplicationName(appName);
        assertEquals(appName, appCreatePage.clickNextStep().getDocumentTitle());
    }

    /**
     * Try to create an application with an empty name using the Enter key.
     */
    @Order(2)
    @Test
    void testEmptyAppNameWithEnter(TestUtils testUtils, TestReference testReference)
    {
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Press Enter key without typing the application name.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.RETURN);
        appCreatePage.waitForApplicationNameError();
        assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Type the application name.
        appCreatePage.setApplicationName("B");
        assertFalse(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Clear the application name using the Backspace key.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.BACK_SPACE);
        appCreatePage.waitForApplicationNameError();
        assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Try to create the application even if the error message is displayed.
        appCreatePage.getApplicationNameInput().sendKeys(Keys.RETURN);
        assertTrue(appCreatePage.getContent().contains(EMPTY_APP_NAME_ERROR_MESSAGE));

        // Fix the application name and move to the next step using the Enter key.
        String appName = testReference.getLastSpaceReference().getName();
        appCreatePage.setApplicationName(appName);
        testUtils.getDriver().addPageNotYetReloadedMarker();
        appCreatePage.getApplicationNameInput().sendKeys(Keys.RETURN);
        testUtils.getDriver().waitUntilPageIsReloaded();
        assertEquals(appName, new ViewPage().getDocumentTitle());
    }

    /**
     * Try to input the name of an existing application.
     */
    @Order(3)
    @Test
    void testExistingAppName(TestUtils testUtils, TestReference testReference)
    {
        String appName = testReference.getLastSpaceReference().getName();

        // Create a new application
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        appCreatePage.setApplicationName(appName);
        ApplicationClassEditPage appClassPage = appCreatePage.clickNextStep();
        ApplicationTemplateProviderEditPage appTemplatePage = appClassPage.clickNextStep();
        ApplicationHomeEditPage appHomePage = appTemplatePage.clickNextStep();
        appHomePage.clickFinish();

        // Try to create the same application again
        appCreatePage = ApplicationCreatePage.gotoPage();
        assertFalse(appCreatePage.getContent().contains(APP_NAME_USED_WARNING_MESSAGE));

        // Type the name of an existing space.
        appCreatePage.setApplicationName(appName);
        assertTrue(appCreatePage.getContent().contains(APP_NAME_USED_WARNING_MESSAGE));

        // Proceed to the next step.
        assertEquals("/" + appName + "/Code/" + appName + "Class",
            appCreatePage.clickNextStep().getBreadcrumbContent());
    }
}
