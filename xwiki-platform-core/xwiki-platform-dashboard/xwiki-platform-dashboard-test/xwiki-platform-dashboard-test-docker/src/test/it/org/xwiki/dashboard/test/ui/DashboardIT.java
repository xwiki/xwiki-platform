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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.ckeditor.test.po.MacroDialogSelectModal;
import org.xwiki.like.test.po.DashboardEditPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

/**
 * Main docker test suite for the Dashboard extension.
 *
 * @version $Id$
 * @since 14.9
 */
@UITest
class DashboardIT
{
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
}
