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
package org.xwiki.dashboard.test.ui;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.like.test.po.DashboardEditPage;
import org.xwiki.like.test.po.DashboardElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;
import org.xwiki.wysiwyg.test.po.MacroDialogSelectModal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Main docker test suite for the Dashboard extension.
 *
 * @version $Id$
 * @since 14.9
 */
@UITest(extraJARs = {
    // The macro service uses the extension index script service to get the list of uninstalled macros (from
    // extensions) which expects an implementation of the extension index. The extension index script service is a
    // core extension so we need to make the extension index also core.
    "org.xwiki.platform:xwiki-platform-extension-index"
})
class DashboardIT
{
    private static final String INFO_GADGET = "Info Message";

    private static final String WARNING_GADGET = "Warning Message";

    @Test
    @Order(1)
    void editDashboard(TestUtils setup)
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference("Dashboard", "WebHome"));
        viewPage.edit();
        viewPage.waitUntilPageIsReady();
        DashboardEditPage dashboardEditPage = new DashboardEditPage();
        int initialWidgetCount = dashboardEditPage.countGadgets();
        MacroDialogSelectModal macroDialogSelectModal = dashboardEditPage.clickAddGadget();
        macroDialogSelectModal.filterByText("Page Tree", 1);
        WebElement macroEntry = macroDialogSelectModal.getFirstMacro().orElseThrow();
        // We check that some css property are correctly applied on the listed macros (see XWIKI-20235). 
        String cssValue = macroEntry.getCssValue("color");
        // Implicit opacity is not always removed by browsers, so we need to check on both cases.
        assertThat(cssValue, anyOf(equalTo("rgb(34, 34, 34)"), equalTo("rgba(34, 34, 34, 1)")));
        macroEntry.click();
        MacroDialogEditModal macroDialogEditModal = macroDialogSelectModal.clickSelect();
        macroDialogEditModal.clickSubmit();
        dashboardEditPage.waitForDashboardsCount(initialWidgetCount + 1);
    }

    /**
     * Creates a nested page and a terminal page holding a dashboard macro and, for each of them, adds gadgets and a
     * column in the dashboard editor, moves a gadget to the new column and checks the saved layout. The pages are
     * created by a user without script right, so that the gadget titles, which call the translation macro by default,
     * must be rendered without script right (see XWIKI-22817).
     */
    @Test
    @Order(2)
    void createDashboardWithGadgetsAndColumns(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        // Users don't have script right by default.
        setup.createUserAndLogin("DashboardUser", "DashboardPassword");

        DocumentReference terminalPage = new DocumentReference("Terminal", testReference.getLastSpaceReference());
        for (DocumentReference page : List.of(testReference, terminalPage)) {
            setup.createPage(page, "{{dashboard/}}");

            DashboardEditPage dashboardEditPage = DashboardEditPage.gotoPage(page);

            // Keep the default gadget titles, which call the translation macro to display the macro name.
            dashboardEditPage.addGadget(INFO_GADGET, "Info gadget content");
            dashboardEditPage.addGadget(WARNING_GADGET, "Warning gadget content");
            assertEquals(List.of(List.of(INFO_GADGET, WARNING_GADGET)), dashboardEditPage.getGadgetTitles());

            dashboardEditPage.addColumn();
            assertEquals(2, dashboardEditPage.getColumnCount());
            dashboardEditPage.moveGadgetToColumn(WARNING_GADGET, 2);
            List<List<String>> expectedLayout = List.of(List.of(INFO_GADGET), List.of(WARNING_GADGET));
            assertEquals(expectedLayout, dashboardEditPage.getGadgetTitles());

            ViewPage viewPage = dashboardEditPage.clickSaveAndView();
            assertEquals(expectedLayout, new DashboardElement().getGadgetTitles());
            assertThat(viewPage.getContent(), containsString("Warning gadget content"));

            // Check that the layout is persisted.
            setup.gotoPage(page);
            assertEquals(expectedLayout, new DashboardElement().getGadgetTitles());
        }
    }

}
