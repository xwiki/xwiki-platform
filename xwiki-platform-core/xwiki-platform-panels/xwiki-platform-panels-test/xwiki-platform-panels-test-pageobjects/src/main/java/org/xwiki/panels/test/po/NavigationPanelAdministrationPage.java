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

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertTrue;

/**
 * Represents the navigation panel administration page (PanelsCode.NavigationConfiguration).
 * 
 * @version $Id$
 * @since 10.5RC1
 */
public class NavigationPanelAdministrationPage extends ViewPage
{
    @FindBy(css = ".codeToExecute .panel .xtree")
    private WebElement treeElement;

    @FindBy(css = ".navigationPanelConfiguration .panel.panel-info")
    private WebElement excludedPagesPane;

    @FindBy(css = ".exclusion-filter.topLevelExtensionPages input[type=\"checkbox\"]")
    private WebElement excludeTopLevelExtensionPagesCheckbox;

    @FindBy(name = "action_saveandcontinue")
    private WebElement saveButton;

    public static NavigationPanelAdministrationPage gotoPage()
    {
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasSection("panels.navigation"));
        administrationPage.clickSection("Look & Feel", "Navigation Panel");
        return new NavigationPanelAdministrationPage().waitUntilPageIsLoaded();
    }

    public List<String> getExclusions()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".exclusion-filter.otherPages .page")).stream()
            .map(element -> element.getText()).collect(Collectors.toList());
    }

    public void exclude(String... pages)
    {
        WebElement source = null;
        for (String page : pages) {
            source = getDriver().findElementWithoutWaiting(this.treeElement,
                By.xpath(".//*[@class = 'jstree-anchor' and . = '" + page + "']"));
            // If there is more than one page to include we need to select all of them first.
            if (pages.length > 1) {
                new Actions(getDriver()).keyDown(Keys.LEFT_CONTROL).click(source).keyUp(Keys.LEFT_CONTROL).build()
                    .perform();
            }
        }
        if (source != null) {
            new Actions(getDriver()).dragAndDrop(source, this.excludedPagesPane).build().perform();
        }
    }

    public List<String> getInclusions()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".exclusion-filter .page.included")).stream()
            .map(element -> element.getText()).collect(Collectors.toList());
    }

    public void include(String... pages)
    {
        WebElement source = null;
        for (String page : pages) {
            // We need to get the last occurrence because the page can be in both "Top Level Extension Pages" and "Other
            // Pages", the later being always visible.
            source = getDriver().findElementWithoutWaiting(this.excludedPagesPane,
                By.xpath("(.//li[contains(@class, 'page')]/a[. = '" + page + "'])[last()]"));
            // If there is more than one page to include we need to select all of them first.
            if (pages.length > 1) {
                source.click();
            }
        }
        if (source != null) {
            new Actions(getDriver()).dragAndDrop(source, this.treeElement).build().perform();
        }
    }

    public boolean isExcludingTopLevelExtensionPages()
    {
        return this.excludeTopLevelExtensionPagesCheckbox.isSelected();
    }

    public void excludeTopLevelExtensionPages(boolean exclude)
    {
        boolean excluded = isExcludingTopLevelExtensionPages();
        if (exclude != excluded) {
            this.excludeTopLevelExtensionPagesCheckbox.click();
        }
    }

    public NavigationTreeElement getNavigationTree()
    {
        return (NavigationTreeElement) new NavigationTreeElement(this.treeElement).waitForIt();
    }

    public void save()
    {
        this.saveButton.click();
        waitForNotificationSuccessMessage("Saved");
    }

    @Override
    public NavigationPanelAdministrationPage waitUntilPageIsLoaded()
    {
        getNavigationTree();

        return this;
    }
}
