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
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;
import org.xwiki.wysiwyg.test.po.MacroDialogSelectModal;

/**
 * @version $Id$
 * @since 14.9
 */
public class DashboardEditPage extends DashboardElement
{
    private static final By ADD_GADGET_BUTTON = By.cssSelector(".dashboard .addgadget");

    private static final By ADD_COLUMN_BUTTON = By.cssSelector(".dashboard .addcontainer");

    /**
     * The number of pixels moved at each step of a drag, so that the drag and drop library notices the moves.
     */
    private static final int DRAG_STEP = 10;

    /**
     * Edits the given page with the inline form, where the dashboard macro displays its editor.
     *
     * @param reference the reference of a page holding a dashboard macro
     * @return the dashboard editor, once ready
     * @since 18.9.0RC1
     */
    public static DashboardEditPage gotoPage(EntityReference reference)
    {
        getUtil().gotoPage(reference, "edit", "editor=inline");
        return new DashboardEditPage().waitUntilReady();
    }

    /**
     * Waits until the dashboard editor is initialized, i.e. until its add buttons are displayed.
     *
     * @return this page object
     * @since 18.9.0RC1
     */
    public DashboardEditPage waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(ADD_GADGET_BUTTON);
        getDriver().waitUntilElementIsVisible(ADD_COLUMN_BUTTON);
        return this;
    }

    /**
     * Click on the "Add column" button and wait for the new (empty) column to be added at the end of the dashboard.
     * The new column is only saved once it holds a gadget.
     *
     * @since 18.9.0RC1
     */
    public void addColumn()
    {
        int columnCount = getColumnCount();
        getDriver().findElement(ADD_COLUMN_BUTTON).click();
        getDriver().waitUntilCondition(driver -> getColumnCount() == columnCount + 1);
    }

    /**
     * Drags a gadget by its title bar and drops it at the end of the given column.
     *
     * @param gadgetTitle the title of the gadget to move
     * @param column the target column, starting at 1
     * @since 18.9.0RC1
     */
    public void moveGadgetToColumn(String gadgetTitle, int column)
    {
        WebElement title = getDriver().findElementsWithoutWaiting(By.cssSelector(".dashboard .gadget .gadget-title"))
            .stream().filter(element -> gadgetTitle.equals(getTitle(element))).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("No gadget titled [%s]", gadgetTitle)));
        // Start the drag with a few small moves: the drag and drop library starts dragging only after the mouse moved,
        // and only then adds a placeholder to the empty columns, which gives them a height so that they accept drops.
        getDriver().createActions().clickAndHold(title).moveByOffset(DRAG_STEP, DRAG_STEP)
            .moveByOffset(DRAG_STEP, DRAG_STEP).perform();
        WebElement targetColumn = getDriver().findElementsWithoutWaiting(COLUMNS).get(column - 1);
        // Drop at the bottom of the target column, below its last gadget or on its placeholder.
        int offset = targetColumn.getSize().getHeight() / 2 - DRAG_STEP;
        getDriver().createActions().moveToElement(targetColumn, 0, offset - DRAG_STEP)
            .moveToElement(targetColumn, 0, offset).release().perform();
        getDriver().waitUntilCondition(driver -> getGadgetTitles().get(column - 1).contains(gadgetTitle));
    }

    /**
     * Click on "Save &amp; View": the dashboard saves the new gadget positions first, then the inline form is
     * submitted.
     *
     * @return the view page of the dashboard
     * @since 18.9.0RC1
     */
    public ViewPage clickSaveAndView()
    {
        return new InlinePage().clickSaveAndView();
    }

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
        getDriver().findElement(ADD_GADGET_BUTTON).click();
        return new MacroDialogSelectModal().waitUntilReady();
    }

    /**
     * Adds a gadget for the given macro, with the given content and the default gadget title, and waits for the
     * dashboard editor to be ready again, since the page is reloaded once the gadget is added.
     *
     * @param macroName the name of the macro to add as a gadget, as listed in the macro selection modal
     * @param content the content of the macro
     * @return this page object, once the gadget is added
     * @since 18.9.0RC1
     */
    public DashboardEditPage addGadget(String macroName, String content)
    {
        int gadgetCount = countGadgets();
        MacroDialogSelectModal macroDialogSelectModal = clickAddGadget();
        macroDialogSelectModal.filterByText(macroName, 1);
        macroDialogSelectModal.getFirstMacro().orElseThrow().click();
        MacroDialogEditModal macroDialogEditModal = macroDialogSelectModal.clickSelect();
        macroDialogEditModal.setMacroContent(content);
        macroDialogEditModal.clickSubmit();
        waitForDashboardsCount(gadgetCount + 1);
        return waitUntilReady();
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
