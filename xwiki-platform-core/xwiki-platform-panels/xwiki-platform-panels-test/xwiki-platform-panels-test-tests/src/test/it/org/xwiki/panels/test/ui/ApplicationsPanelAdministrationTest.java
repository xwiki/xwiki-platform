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
package org.xwiki.panels.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.panels.test.po.ApplicationsPanelAdministrationPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to the ApplicationsPanel Administration
 *  
 * @version $Id$
 * @since 7.1M1
 * @since 7.0.1
 * @since 6.4.4
 */
public class ApplicationsPanelAdministrationTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void testApplicationsPanelAdministration()
    {
        goToAppPanelAdminPage();

        ApplicationsPanelAdministrationPage appPanelAdminPage = new ApplicationsPanelAdministrationPage();
        assertTrue(appPanelAdminPage.getApplicationsInBar().isEmpty());
        assertFalse(appPanelAdminPage.getApplicationsNotInBar().isEmpty());

        // Add an app from the bar
        appPanelAdminPage.addApplicationInBar("Panels");
        assertTrue(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertFalse(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));
        
        // Remove an app from the bar
        appPanelAdminPage.removeApplicationFromBar("Panels");
        assertFalse(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertTrue(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));
        
        // Try the "revert" action
        appPanelAdminPage.revert();
        assertTrue(appPanelAdminPage.getApplicationsInBar().isEmpty());
        assertFalse(appPanelAdminPage.getApplicationsNotInBar().isEmpty());
        
        // Save
        appPanelAdminPage.save();
        assertTrue(appPanelAdminPage.hasSuccessNotification());

        // Go back to the app panel admin page (refresh the page)
        goToAppPanelAdminPage();

        // Verify that the setting have been saved        
        assertFalse(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertTrue(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));
        
        // Put the application back to the bar
        appPanelAdminPage.addApplicationInBar("Panels");
        assertTrue(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertFalse(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));

        // Save again
        appPanelAdminPage.save();
        assertTrue(appPanelAdminPage.hasSuccessNotification());

        // Go back to the app panel admin page (refresh the page)
        goToAppPanelAdminPage();

        // Verify that the setting have been saved
        assertTrue(appPanelAdminPage.getApplicationsInBar().contains("Panels"));
        assertFalse(appPanelAdminPage.getApplicationsNotInBar().contains("Panels"));
        
    }
    
    private void goToAppPanelAdminPage()
    {
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasSection("panels.applications"));
        administrationPage.clickSection("Applications", "Applications Panel");
    }
    
}
