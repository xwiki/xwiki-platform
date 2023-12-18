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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the content of the Page Layout tab from the Panels administration section.
 * 
 * @version $Id$
 * @since 9.2RC1
 */
public class PageLayoutTabContent extends BaseElement
{
    public enum Column {
        LEFT, RIGHT
    }

    public enum Layout {
        NOCOLUMN("nosidecolumn"),
        LEFT("leftcolumn"),
        RIGHT("rightcolumn"),
        BOTH("bothcolumns");

        public String id;

        Layout(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    @FindBy(css = "#XWiki\\.XWikiPreferences_0_rightPanels")
    private WebElement rightPanelsInput;

    @FindBy(id = "rightPanels")
    private WebElement rightPanels;

    @FindBy(id = "leftPanels")
    private WebElement leftPanels;

    @FindBy(xpath = "//a[@href='#PanelListSection']")
    private WebElement panelListSection;

    public WebElement getLayout(Layout layout)
    {
        return getDriver().findElementWithoutWaiting(By.id(layout.getId()));
    }

    public PageLayoutTabContent selectNoSideColumnLayout()
    {
        getLayout(Layout.NOCOLUMN).click();
        getDriver().waitUntilElementDisappears(By.id("rightPanels"));
        getDriver().waitUntilElementDisappears(By.id("leftPanels"));
        return this;
    }

    public PageLayoutTabContent selectRightColumnLayout()
    {
        getLayout(Layout.RIGHT).click();
        getDriver().waitUntilElementIsVisible(By.id("rightPanels"));
        return this;
    }

    public PageLayoutTabContent selectBothColumnsLayout()
    {
        getLayout(Layout.BOTH).click();
        getDriver().waitUntilElementIsVisible(By.id("rightPanels"));
        getDriver().waitUntilElementIsVisible(By.id("leftPanels"));
        return this;
    }

    public PageLayoutTabContent selectLeftColumnLayout()
    {
        getLayout(Layout.LEFT).click();
        getDriver().waitUntilElementIsVisible(By.id("leftPanels"));
        return this;
    }

    public boolean isRightPanelVisible()
    {
        return this.rightPanels.isDisplayed();
    }

    public boolean isLeftPanelVisible()
    {
        return this.leftPanels.isDisplayed();
    }

    public String getRightPanels()
    {
        return this.rightPanelsInput.getText();
    }

    public PageLayoutTabContent setRightPanels(String rightPanels)
    {
        this.rightPanelsInput.clear();
        this.rightPanelsInput.sendKeys(rightPanels);
        return this;
    }

    public PageLayoutTabContent openPanelListSection()
    {
        this.panelListSection.click();
        return this;
    }

    public void dragPanelToColumn(String panelName, Column column)
    {
        String cssSelector = String.format(".panel.%s h2", panelName);
        WebElement element = getDriver().findElementWithoutWaiting(By.cssSelector(cssSelector));
        if (column == Column.RIGHT) {
            getDriver().dragAndDrop(element, rightPanels);
        } else {
            getDriver().dragAndDrop(element, leftPanels);
        }
    }

    public void removePanelFromColumn(String panelName, Column column)
    {
        String cssSelector = String.format("#%%s .panel.%s h2", panelName);

        if (column == Column.RIGHT) {
            cssSelector = String.format(cssSelector, "rightPanels");
        } else {
            cssSelector = String.format(cssSelector, "leftPanels");
        }
        // We avoid to scroll for not doing a drag&drop outside the viewport.
        WebElement element = getDriver().findElementWithoutWaitingWithoutScrolling(By.cssSelector(cssSelector));
        WebElement allViewPanels = getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector("#allviewpanels .accordionTabContentBox")
        );
        getDriver().dragAndDrop(element, allViewPanels);
    }

    public boolean isLayoutSelected(Layout layout)
    {
        WebElement selectedoption = getDriver().findElementWithoutWaiting(By.id("selectedoption"));
        return getDriver().hasElementWithoutWaiting(selectedoption, By.id(layout.getId()));
    }
}
