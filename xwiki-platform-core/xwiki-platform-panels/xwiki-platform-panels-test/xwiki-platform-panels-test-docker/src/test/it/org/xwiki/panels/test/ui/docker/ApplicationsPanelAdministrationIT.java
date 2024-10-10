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
package org.xwiki.panels.test.ui.docker;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.panels.test.po.ApplicationsPanelAdministrationPage;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the ApplicationsPanel Administration
 *  
 * @version $Id$
 * @since 7.1M1
 * @since 7.0.1
 * @since 6.4.4
 */
@UITest
public class ApplicationsPanelAdministrationIT
{
    @AfterEach
    void cleanUp(TestUtils testUtils) throws Exception
    {
        testUtils.rest().delete(
            new LocalDocumentReference(Arrays.asList("Apps", "App1"), "WebHome"));
        testUtils.rest().delete(
            new LocalDocumentReference(Arrays.asList("Apps", "App2"), "WebHome"));
        testUtils.rest().delete(
            new LocalDocumentReference(Arrays.asList("Apps", "App3"), "WebHome"));
        testUtils.rest().delete(
            new LocalDocumentReference(Arrays.asList("PanelsCode"), "ApplicationsPanelConfiguration"));
    }

    @Test
    @Order(1)
    public void testApplicationsPanelAdministration(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        createApplicationUIX(testUtils, "App1");
        createApplicationUIX(testUtils, "App2");
        createApplicationUIX(testUtils, "App3");

        // First: check that the panel displays everything in the right order
        checkInitialState(ApplicationsPanel.gotoPage());

        // No go to the administration
        ApplicationsPanelAdministrationPage appPanelAdminPage = ApplicationsPanelAdministrationPage.gotoPage();

        // Check that everything is as expected
        checkInitialState(appPanelAdminPage);

        // Make changes and revert them, check that it works
        applyChanges(appPanelAdminPage);
        appPanelAdminPage.revert();
        checkInitialState(appPanelAdminPage);

        // Do the changes again, and save them
        applyChanges(appPanelAdminPage);
        appPanelAdminPage.save();
        assertTrue(appPanelAdminPage.hasSuccessNotification());

        // Go to the panel and see what are the new results
        ApplicationsPanel applicationsPanel = ApplicationsPanel.gotoPage();
        List<String> applications = applicationsPanel.getApplications();
        assertEquals(4, applications.size());
        Iterator<String> iterator = applications.iterator();
        assertEquals("App3", iterator.next());
        assertEquals("App1", iterator.next());
        assertEquals("App2", iterator.next());
        assertEquals("More applications", iterator.next());

        // Go back to the app panel admin page and verify that the settings are well displayed
        appPanelAdminPage = ApplicationsPanelAdministrationPage.gotoPage();
        assertFalse(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertTrue(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));
        iterator = appPanelAdminPage.getApplicationsInBar().iterator();
        assertEquals("App3", iterator.next());
        assertEquals("App1", iterator.next());
        assertEquals("App2", iterator.next());
        
        // Put the application back to the bar
        appPanelAdminPage.addApplicationInBar("Panels");
        assertTrue(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertFalse(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));

        // Put back the initial order
        appPanelAdminPage.moveAppBefore("App1", appPanelAdminPage.getApplicationsInBar().get(0));
        appPanelAdminPage.moveAppBefore("App2", appPanelAdminPage.getApplicationsInBar().get(1));
        appPanelAdminPage.moveAppBefore("App3", appPanelAdminPage.getApplicationsInBar().get(2));

        // Save again
        appPanelAdminPage.save();
        assertTrue(appPanelAdminPage.hasSuccessNotification());

        // Go back to the panel and check everything is good
        checkInitialState(ApplicationsPanel.gotoPage());

        // Verify that the settings have been saved
        checkInitialState(ApplicationsPanelAdministrationPage.gotoPage());
    }

    private void createApplicationUIX(TestUtils testUtils, String applicationName) throws Exception
    {
        LocalDocumentReference docRef =
            new LocalDocumentReference(Arrays.asList("Apps", applicationName), "WebHome");
        testUtils.deletePage(docRef);
        testUtils.createPage(docRef, "");
        Object object = testUtils.rest().object(docRef, "XWiki.UIExtensionClass");
        object.getProperties().add(TestUtils.RestTestUtils.property("extensionPointId", "org.xwiki.platform.panels"
            + ".Applications"));
        object.getProperties().add(TestUtils.RestTestUtils.property("name", applicationName));
        object.getProperties().add(TestUtils.RestTestUtils.property("parameters",
            String.format("label=%s\ntarget=Apps.%s\nicon=icon:home", applicationName, applicationName)));
        testUtils.rest().add(object);
    }

    private void checkInitialState(ApplicationsPanelAdministrationPage appPanelAdminPage) throws Exception
    {
        assertEquals(4, appPanelAdminPage.getApplicationsInBar().size());
        assertTrue(appPanelAdminPage.getApplicationsNotInBar().isEmpty());

        // Verify the order
        Iterator<String> iterator = appPanelAdminPage.getApplicationsInBar().iterator();
        assertEquals("App1", iterator.next());
        assertEquals("App2", iterator.next());
        assertEquals("App3", iterator.next());
        assertEquals("Panels", iterator.next());
    }

    private void checkInitialState(ApplicationsPanel applicationsPanel) throws Exception
    {
        List<String> applications = applicationsPanel.getApplications();
        assertEquals(5, applications.size());
        Iterator<String> iterator = applications.iterator();
        assertEquals("App1", iterator.next());
        assertEquals("App2", iterator.next());
        assertEquals("App3", iterator.next());
        assertEquals("Panels", iterator.next());
        assertEquals("More applications", iterator.next());
    }

    private void applyChanges(ApplicationsPanelAdministrationPage appPanelAdminPage) throws Exception
    {
        // Remove an app from the bar
        appPanelAdminPage.removeApplicationFromBar("Panels");
        assertFalse(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertTrue(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));

        // Change the order
        appPanelAdminPage.moveAppBefore("App3", "App1");
    }
}
