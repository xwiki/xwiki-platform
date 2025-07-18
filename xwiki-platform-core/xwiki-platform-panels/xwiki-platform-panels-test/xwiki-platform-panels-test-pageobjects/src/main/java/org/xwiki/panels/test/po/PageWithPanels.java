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
package org.xwiki.panels.test.po;

import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BasePage;

/**
 * Represents a page that has panels.
 * 
 * @version $Id$
 * @since 4.3.2
 */
public class PageWithPanels extends BasePage
{
    @FindBy(id = "rightPanels")
    private WebElement rightPanels;

    @FindBy(id = "leftPanels")
    private WebElement leftPanels;

    @FindBy(id = "rightPanelsToggle")
    private WebElement rightPanelsToggle;

    @FindBy(id = "leftPanelsToggle")
    private WebElement leftPanelsToggle;

    @FindBy(css = "#rightPanels .ui-resizable-handle")
    private WebElement rightPanelsResizeHandle;

    @FindBy(css = "#leftPanels .ui-resizable-handle")
    private WebElement leftPanelsResizeHandle;
    public static String RIGHT = "right";
    public static String LEFT = "left";
    
    /**
     * @param panelTitle the panel title
     * @return {@code true} if this page has the specified panel, {@code false} otherwise
     */
    public boolean hasPanel(String panelTitle)
    {
        String xpath = String.format("//h2[@class = 'xwikipaneltitle' and . = '%s']", panelTitle);
        return getDriver().findElementsWithoutWaiting(By.xpath(xpath)).size() > 0;
    }

    public boolean hasRightPanels()
    {
        return getDriver().hasElementWithoutWaiting(By.id("rightPanels"));
    }

    public boolean hasLeftPanels()
    {
        return getDriver().hasElementWithoutWaiting(By.id("leftPanels"));
    }

    public boolean hasPanelInRightColumn(String panelName)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//div[@id = 'rightPanels']/div[contains(@class, '"+panelName+"')]"));
    }

    public boolean hasPanelInLeftColumn(String panelName)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//div[@id = 'leftPanels']/div[contains(@class, '"+panelName+"')]"));
    }
    
    public boolean panelIsToggled(String panelSide) {
        WebElement panelToggle = (Objects.equals(panelSide, RIGHT)) ? rightPanelsToggle : leftPanelsToggle;
        return Objects.equals(panelToggle.getDomAttribute("aria-expanded"), "true") 
            || Objects.equals(panelToggle.getDomAttribute("aria-expanded"), null);
    }
    
    public void togglePanel(String panelSide) {
        WebElement panelToggle = (Objects.equals(panelSide, RIGHT)) ? rightPanelsToggle : leftPanelsToggle;
        panelToggle.click();
    }
    public int getPanelWidth(String panelSide) {
        WebElement panels = (Objects.equals(panelSide, RIGHT)) ? 
            rightPanels : leftPanels;
        return panels.getSize().getWidth();
    }
    public void resizePanel(String panelSide, int panelSizeDiff) {
        WebElement panelResizeHandle = (Objects.equals(panelSide, RIGHT)) ?
            rightPanelsResizeHandle : leftPanelsResizeHandle;
        // Define the drag and drop action
        Actions action = new Actions(this.getDriver().getWrappedDriver());
        action.clickAndHold(panelResizeHandle);
        int panelSideInvert = Objects.equals(panelSide, RIGHT)? 1 : -1;
        // We need to correct a bit the shift induced by the exact place where the handled is taken.
        action.moveByOffset((panelSizeDiff + 6) * panelSideInvert, 0);
        action.release();
        action.perform();
    }
}
