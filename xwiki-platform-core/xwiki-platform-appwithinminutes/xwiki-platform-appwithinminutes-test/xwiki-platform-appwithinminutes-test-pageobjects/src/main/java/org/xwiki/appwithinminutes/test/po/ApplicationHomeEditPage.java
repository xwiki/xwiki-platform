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
package org.xwiki.appwithinminutes.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions available when editing the application home page. This is also the forth step of the App
 * Within Minutes wizard, in which the presentation of the application home page is customized.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class ApplicationHomeEditPage extends ApplicationEditPage
{
    @FindBy(id = "availableColumns")
    private WebElement availableColumns;

    @FindBy(css = ".columnPicker a.addColumn")
    private WebElement addColumnButton;

    @FindBy(id = "applicationIcon")
    private WebElement applicationIconInput;

    /**
     * The text area used to input the application description.
     */
    @FindBy(id = "AppWithinMinutes.LiveTableClass_0_description")
    private WebElement descriptionTextArea;

    /**
     * The field used to input the application title.
     */
    @FindBy(id = "xwikidoctitleinput")
    private WebElement titleInput;

    /**
     * Default constructor which waits on xaction_save button.
     */
    public ApplicationHomeEditPage()
    {
        super(true, true);
    }

    /**
     * Clicks on the Previous Step button.
     *
     * @return the page that represents the previous step of the App Within Minutes wizard
     */
    public ApplicationTemplateProviderEditPage clickPreviousStep()
    {
        previousStepButton.click();
        return new ApplicationTemplateProviderEditPage();
    }

    /**
     * Clicks on the Finish button.
     *
     * @return the page that represents the application home page
     */
    public ApplicationHomePage clickFinish()
    {
        nextStepButton.click();
        return new ApplicationHomePage();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends ViewPage> T createViewPage()
    {
        return (T) new ApplicationHomePage();
    }

    /**
     * Sets the application title.
     * 
     * @param title the new application title
     */
    public void setTitle(String title)
    {
        this.titleInput.clear();
        this.titleInput.sendKeys(title);
    }

    /**
     * @return the value of the application title input
     */
    public String getTitle()
    {
        return this.titleInput.getAttribute("value");
    }

    /**
     * Sets the application description.
     *
     * @param description the new application description
     */
    public void setDescription(String description)
    {
        descriptionTextArea.clear();
        descriptionTextArea.sendKeys(description);
    }

    /**
     * Sets the application icon.
     *
     * @param icon the icon to set
     */
    public void setIcon(String icon)
    {
        applicationIconInput.clear();
        applicationIconInput.sendKeys(icon);
        // Send 'escape' to close the icon picker after having filled the input.
        applicationIconInput.sendKeys(Keys.ESCAPE);
    }

    /**
     * @return the application icon
     */
    public String getIcon()
    {
        return applicationIconInput.getAttribute("value");
    }

    /**
     * Adds a new live table column.
     *
     * @param columnLabel the label of the live table column to be added
     */
    public void addLiveTableColumn(String columnLabel)
    {
        Select select = new Select(availableColumns);
        select.selectByVisibleText(columnLabel);
        addColumnButton.click();
    }

    /**
     * Removes the live table column with the specified label.
     *
     * @param columnLabel the label of the live table column to be removed
     */
    public void removeLiveTableColumn(String columnLabel)
    {
        WebElement column = getLiveTableColumn(columnLabel);
        // FIXME: This doesn't trigger the :hover CSS pseudo class. The click still works because the delete X (text) is
        // not really hidden: it is displayed with white color (the page background-color).
        new Actions(getDriver().getWrappedDriver()).moveToElement(column).perform();
        getDriver().scrollTo(column.findElement(By.className("delete"))).click();
    }

    /**
     * Reorders the live table columns by moving one column before another.
     *
     * @param columnToMove the label of the live table column to be moved
     * @param beforeColumn the label of the reference column
     */
    public void moveLiveTableColumnBefore(String columnToMove, String beforeColumn)
    {
        WebElement columnToMoveElement = getLiveTableColumn(columnToMove);
        WebElement beforeColumnElement = getLiveTableColumn(beforeColumn);
        getDriver().scrollTo(columnToMoveElement);
        Actions actions = getDriver().createActions().clickAndHold(columnToMoveElement);

        // We move the column from the center, so we need to ensure that we actually move it with an offset of half its
        // width to be sure it's before the other column element.
        int offsetX = -(columnToMoveElement.getSize().getWidth() / 2);

        // Safety measure to ensure we remain on the same vertical alignment
        int offsetY = 2;
        getDriver().moveToTopLeftCornerOfTargetWithOffset(beforeColumnElement, offsetX, offsetY, actions)
            .release().perform();
    }

    /**
     * @param columnLabel the label of a live table column
     * @return the element that represents the specified live table column
     */
    private WebElement getLiveTableColumn(String columnLabel)
    {
        String escapedColumnLabel = columnLabel.replace("\\", "\\\\").replace("'", "\\'");
        String xpath = "//ul[@class = 'hList']/li[starts-with(., '" + escapedColumnLabel + "')]";
        return getDriver().findElementWithoutWaiting(getForm(), By.xpath(xpath));
    }

    /**
     * @param columnLabel the label of the live table column to check for
     * @return {@code true} if the specified column was selected (i.e. included in the live table), {@code false}
     *         otherwise
     */
    public boolean hasLiveTableColumn(String columnLabel)
    {
        String escapedColumnLabel = columnLabel.replace("\\", "\\\\").replace("'", "\\'");
        String xpath = "//ul[@class = 'hList']/li[starts-with(., '" + escapedColumnLabel + "')]";
        return getDriver().findElementsWithoutWaiting(getForm(), By.xpath(xpath)).size() > 0;
    }

    /**
     * @param columnLabel the label of a live table column
     * @return {@code true} if the specified column is displayed as deprecated in the list of selected live table
     *         columns, {@code false} otherwise
     */
    public boolean isLiveTableColumnDeprecated(String columnLabel)
    {
        return "deprecated".equals(getLiveTableColumn(columnLabel).getAttribute("class"));
    }

    /**
     * Removes all deprecated columns or simply hides the warning message based on the given boolean value.
     *
     * @param yes {@code true} to remove all deprecated columns, {@code false} to just hide the warning message
     */
    public void removeAllDeprecatedLiveTableColumns(boolean yes)
    {
        WebElement warningMessage = getDriver().findElementWithoutWaiting(getForm(), By.className("warningmessage"));
        getDriver().findElementWithoutWaiting(warningMessage, By.linkText(yes ? "Yes" : "No")).click();
    }

    /**
     * @return {@code true} if the warning message about deprecated live table columns is displayed, {@code false}
     *         otherwise
     */
    public boolean isDeprecatedLiveTableColumnsWarningDisplayed()
    {
        List<WebElement> warnings = getDriver().findElementsWithoutWaiting(getForm(), By.className("warningmessage"));
        return warnings.size() == 1 && warnings.get(0).isDisplayed();
    }
}
