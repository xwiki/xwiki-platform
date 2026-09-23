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
package org.xwiki.test.ui.po.editor;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.FormContainerElement;

/**
 * Represents the common actions possible on all Pages when using the "edit" action with the "class" editor.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class ClassEditPage extends EditPage
{
    @FindBy(name = "action_propupdate")
    private WebElement saveandview;

    @FindBy(id = "propupdate")
    private WebElement propertyForm;

    @FindBy(id = "propname")
    private WebElement propertyNameField;

    @FindBy(id = "proptype")
    private WebElement propertyTypeField;

    @FindBy(name = "action_propadd")
    private WebElement propertySubmit;

    private FormContainerElement form;

    /**
     * Go to the passed page in object edit mode.
     */
    public static ClassEditPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page, "edit", "editor=class");
        return new ClassEditPage();
    }

    /**
     * Edit the specified class.
     *
     * @param classReference the reference of the class to edit
     * @return the class edit page
     * @since 16.10.6
     * @since 17.3.0RC1
     */
    public static ClassEditPage gotoPage(EntityReference classReference)
    {
        getUtil().gotoPage(classReference, "edit", "editor=class");
        return new ClassEditPage();
    }

    public ClassPropertyEditPane addProperty(String propertyName, String propertyType)
    {
        addPropertyWithoutWaiting(propertyName, propertyType);
        waitForNotificationSuccessMessage("Property added");

        // The following call waits for the element to appear since there's no page refresh.
        return getPropertyEditPane(propertyName);
    }

    /**
     * @since 3.2M3
     */
    public void addPropertyWithoutWaiting(String propertyName, String propertyType)
    {
        getForm().setFieldValue(this.propertyNameField, propertyName);
        getForm().setFieldValue(this.propertyTypeField, propertyType);
        this.propertySubmit.click();
    }

    public void deleteProperty(String propertyName)
    {
        final By propertyLocator = By.id("xproperty_" + propertyName);
        final WebElement propertyContainer = getDriver().findElement(propertyLocator);
        WebElement deleteLink = propertyContainer.findElement(By.className("delete"));
        deleteLink.click();

        // Expect a confirmation box
        getDriver().waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        getDriver().findElement(By.cssSelector(".xdialog-box-confirmation input[value='Yes']")).click();
        getDriver().waitUntilElementDisappears(propertyLocator);
    }

    /**
     * Test that a property is present in the editor.
     * @param propertyName the name of the property
     * @return {@code true} if the property can be found.
     * @since 17.7.0RC1
     */
    public boolean hasProperty(String propertyName)
    {
        return getDriver().hasElement(By.id("xproperty_" + propertyName));
    }

    /**
     *
     * @return the ordered list of property names.
     * @since 17.9.0RC1
     */
    public List<String> getProperties()
    {
        return getDriver().findElements(By.className("xproperty")).stream()
            .map(item -> item.getAttribute("id").substring("xproperty_".length()))
            .toList();
    }

    /**
     * Move one property above the other one.
     * @param propertyToMove the property to move
     * @param propertyBefore the property above which to place the first property
     * @since 17.9.0RC1
     */
    public void movePropertyBefore(String propertyToMove, String propertyBefore)
    {
        WebElement target = getDriver().findElementWithoutWaiting(By.id("xproperty_" + propertyBefore));
        new Actions(getDriver().getWrappedDriver()).dragAndDrop(getPropertyDragSource(propertyToMove), target)
            .perform();
    }

    /**
     * Clicks the move up button of the given property. The property keeps its position when it is already the first
     * one.
     *
     * @param propertyToMove the property to move one position up
     * @since 18.8.0RC1
     */
    public void movePropertyUp(String propertyToMove)
    {
        getMovePropertyUpButton(propertyToMove).click();
    }

    /**
     * Clicks the move down button of the given property. The property keeps its position when it is already the last
     * one.
     *
     * @param propertyToMove the property to move one position down
     * @since 18.8.0RC1
     */
    public void movePropertyDown(String propertyToMove)
    {
        getMovePropertyDownButton(propertyToMove).click();
    }

    /**
     * Presses the up arrow key on the move buttons of the given property, which both answer it. The property keeps its
     * position when it is already the first one.
     *
     * @param propertyToMove the property to move one position up
     * @since 18.8.0RC1
     */
    public void movePropertyUpWithKeyboard(String propertyToMove)
    {
        getMovePropertyUpButton(propertyToMove).sendKeys(Keys.ARROW_UP);
    }

    /**
     * Presses the down arrow key on the move buttons of the given property, which both answer it. The property keeps
     * its position when it is already the last one.
     *
     * @param propertyToMove the property to move one position down
     * @since 18.8.0RC1
     */
    public void movePropertyDownWithKeyboard(String propertyToMove)
    {
        getMovePropertyDownButton(propertyToMove).sendKeys(Keys.ARROW_DOWN);
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the button moving the given property one position up
     * @since 18.8.0RC1
     */
    public WebElement getMovePropertyUpButton(String propertyName)
    {
        return getPropertyContainer(propertyName).findElement(By.cssSelector(".reorder-control-up"));
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the button moving the given property one position down
     * @since 18.8.0RC1
     */
    public WebElement getMovePropertyDownButton(String propertyName)
    {
        return getPropertyContainer(propertyName).findElement(By.cssSelector(".reorder-control-down"));
    }

    /**
     * @param propertyName the name of a property of this class
     * @return whether the button moving the given property up currently holds the focus
     * @since 18.8.0RC1
     */
    public boolean isMovePropertyUpButtonFocused(String propertyName)
    {
        return getMovePropertyUpButton(propertyName).equals(getDriver().switchTo().activeElement());
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the accessible name of the button moving the given property up, which is displayed to screen readers
     *     only
     * @since 18.8.0RC1
     */
    public String getMovePropertyUpButtonName(String propertyName)
    {
        return getButtonName(getMovePropertyUpButton(propertyName));
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the accessible name of the button moving the given property down, which is displayed to screen readers
     *     only
     * @since 18.8.0RC1
     */
    public String getMovePropertyDownButtonName(String propertyName)
    {
        return getButtonName(getMovePropertyDownButton(propertyName));
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the tooltip of the button moving the given property up
     * @since 18.8.0RC1
     */
    public String getMovePropertyUpButtonTooltip(String propertyName)
    {
        return getMovePropertyUpButton(propertyName).getDomAttribute("title");
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the tooltip of the button moving the given property down
     * @since 18.8.0RC1
     */
    public String getMovePropertyDownButtonTooltip(String propertyName)
    {
        return getMovePropertyDownButton(propertyName).getDomAttribute("title");
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the number of move buttons displayed for the given property, which stays at two however many times
     *     properties are added to the editor
     * @since 18.8.0RC1
     */
    public int getMovePropertyButtonCount(String propertyName)
    {
        return getPropertyContainer(propertyName).findElements(By.cssSelector(".reorder-control")).size();
    }

    /**
     * @param button one of the move buttons
     * @return the accessible name of the given button
     */
    private String getButtonName(WebElement button)
    {
        return button.findElement(By.className("sr-only")).getDomProperty("textContent");
    }

    /**
     * Waits until the move buttons have announced the given text to the screen readers.
     *
     * @param announcement the expected announcement
     * @since 18.8.0RC1
     */
    public void waitForReorderAnnouncement(String announcement)
    {
        getDriver().waitUntilCondition(driver -> announcement.equals(getReorderAnnouncement()));
    }

    /**
     * Reads the announcement without waiting for it. The live region is emptied as soon as a move is performed and
     * filled again shortly after, so this returns an empty string when it is called right after a move. Prefer
     * {@link #waitForReorderAnnouncement(String)}, which waits for the announcement to be set.
     *
     * @return the text currently held by the live region the move buttons announce their moves in
     * @since 18.8.0RC1
     */
    public String getReorderAnnouncement()
    {
        return getDriver().findElementWithoutWaiting(By.id("reorder-controls-live-region"))
            .getDomProperty("textContent");
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the part of the property title a drag has to start from, the move buttons being operated by a click
     *     rather than by a drag
     */
    private WebElement getPropertyDragSource(String propertyName)
    {
        return getPropertyContainer(propertyName).findElement(By.cssSelector(".xproperty-title h2"));
    }

    /**
     * @param propertyName the name of a property of this class
     * @return the element holding the title and the content of the given property
     */
    private WebElement getPropertyContainer(String propertyName)
    {
        return getDriver().findElementWithoutWaiting(By.id("xproperty_" + propertyName));
    }

    private FormContainerElement getForm()
    {
        if (this.form == null) {
            this.form = new FormContainerElement(By.id("propupdate"));
        }
        return this.form;
    }

    /**
     * Use this method if you need to set generic meta-properties (common to all XClass property types). For specific
     * meta-properties use the methods dedicated to each XClass property type.
     *
     * @param propertyName the name of a property of this class
     * @return the pane used to edit the specified property
     * @since 4.5
     */
    public ClassPropertyEditPane getPropertyEditPane(String propertyName)
    {
        return new ClassPropertyEditPane(getForm(), propertyName).expand();
    }

    public DatabaseListClassEditElement getDatabaseListClassEditElement(String propertyName)
    {
        return (DatabaseListClassEditElement) new DatabaseListClassEditElement(getForm(), propertyName).expand();
    }

    public StaticListClassEditElement getStaticListClassEditElement(String propertyName)
    {
        return (StaticListClassEditElement) new StaticListClassEditElement(getForm(), propertyName).expand();
    }

    public NumberClassEditElement getNumberClassEditElement(String propertyName)
    {
        return (NumberClassEditElement) new NumberClassEditElement(getForm(), propertyName).expand();
    }

    @Override
    public WebElement getSaveAndViewButton()
    {
        return saveandview;
    }
}
