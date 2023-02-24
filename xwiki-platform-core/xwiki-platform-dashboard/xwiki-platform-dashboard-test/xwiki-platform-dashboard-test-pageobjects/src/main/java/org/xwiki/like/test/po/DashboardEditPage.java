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
package org.xwiki.like.test.po;

import org.openqa.selenium.By;
import org.xwiki.ckeditor.test.po.MacroDialogSelectModal;
import org.xwiki.test.ui.po.BaseElement;

/**
 * @version $Id$
 * @since 14.9
 */
public class DashboardEditPage extends BaseElement
{
    /**
     * @return the number of existing gadgets
     */
    public int countGadgets()
    {
        return getDriver().findElements(By.cssSelector(".dashboard .gadget")).size();
    }

    /**
     * Click on the "Add gadget" button.
     *
     * @return a page object instance for the macro dialog selection modal opened after the click
     */
    public MacroDialogSelectModal clickAddGadget()
    {
        getDriver().findElement(By.cssSelector(".dashboard .addgadget")).click();
        return new MacroDialogSelectModal().waitUntilReady();
    }

    /**
     * Wait until the number of dashboard matches the expected count.
     *
     * @param expectedCount the expected number of dashboards
     * @since 15.1
     * @since 14.10.6
     */
    public void waitForDashboardsCount(int expectedCount)
    {
        getDriver().waitUntilCondition(webDriver -> countGadgets() == expectedCount);
    }
}
