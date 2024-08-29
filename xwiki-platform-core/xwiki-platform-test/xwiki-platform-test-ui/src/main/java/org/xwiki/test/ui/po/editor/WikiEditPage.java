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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.po.LoginPage;

/**
 * Represents the common actions possible on all Pages when using the "edit" action with "wiki" editor
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class WikiEditPage extends PreviewableEditPage
{
    @FindBy(id = "xwikidoctitleinput")
    private WebElement titleInput;

    @FindBy(id = "xwikidocparentinput")
    private WebElement parentInput;

    /**
     * The hierarchy icon that needs to be clicked to reveal the parent field.
     */
    @FindBy(id = "editParentTrigger")
    private WebElement editParentTrigger;

    @FindBy(name = "minorEdit")
    private WebElement minorEditCheckBox;

    @FindBy(name = "comment")
    private WebElement commentInput;

    @FindBy(className = "modal-popup")
    private WebElement modal;

    @FindBy(id = "content")
    protected WebElement contentText;

    /**
     * Go to the passed page in wiki edit mode.
     */
    public static WikiEditPage gotoPage(String space, String page)
    {
        return gotoPage(new LocalDocumentReference(space, page));
    }

    /**
     * Open the specified page in Wiki edit mode.
     * 
     * @param pageReference the page to open
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public static WikiEditPage gotoPage(EntityReference pageReference)
    {
        getUtil().gotoPage(pageReference, "edit", "editor=wiki");
        return new WikiEditPage();
    }

    /**
     * Get the <code>title</code> of the page.
     */
    public String getTitle()
    {
        return this.titleInput.getAttribute("value");
    }

    /**
     * Set the <code>title</code> of the page.
     */
    public void setTitle(String title)
    {
        this.titleInput.clear();
        this.titleInput.sendKeys(title);
    }

    /**
     * @return the value of the parent field.
     */
    public String getParent()
    {
        return this.parentInput.getAttribute("value");
    }

    /**
     * Set the {@code parent} of the page
     * 
     * @param parent the value for the new parent to set
     */
    public void setParent(String parent)
    {
        if (!this.parentInput.isDisplayed()) {
            this.editParentTrigger.click();
        }
        this.parentInput.clear();
        this.parentInput.sendKeys(parent);
    }

    /**
     * Get the <code>content</code> of the page.
     */
    public String getContent()
    {
        return this.contentText.getText();
    }

    /**
     * Get the <code>content</code> of the page without removing leading or trailing whitespaces.
     */
    public String getExactContent()
    {
        return this.contentText.getAttribute("value");
    }

    /**
     * Set the <code>content</code> of the page.
     */
    public void setContent(String content)
    {
        this.contentText.clear();
        this.contentText.sendKeys(content);
    }

    /**
     * Set the minor edit check box value.
     */
    public void setMinorEdit(boolean value)
    {
        if ((this.minorEditCheckBox.isSelected() && !value)
            || (!this.minorEditCheckBox.isSelected() && value))
        {
            this.minorEditCheckBox.click();
        }
    }

    /**
     * Set <code>comment</code> for this change.
     */
    public void setEditComment(String comment)
    {
        this.commentInput.clear();
        this.commentInput.sendKeys(comment);
    }

    /**
     * @return true if the edit comment input field is displayed
     */
    public boolean isEditCommentDisplayed()
    {
        return this.commentInput.isDisplayed();
    }

    public boolean loginModalDisplayed() {
        if (!this.modal.isDisplayed()) {
            return false;
        }

        return this.modal.findElement(By.cssSelector("a[title=login]")).isDisplayed();
    }

    public void closeLoginModal()
    {
        this.modal.findElement(By.cssSelector("button.btn-primary")).click();
        getDriver().waitUntilElementDisappears(By.className("modal-popup"));
    }

    public LoginPage clickModalLoginLink()
    {
        String currentWindow = getUtil().getCurrentTabHandle();
        String newWindow = getUtil().openLinkInTab(By.cssSelector("a[title=login]"), currentWindow);
        getUtil().switchTab(newWindow);
        getDriver().waitUntilElementIsVisible(By.id("j_username"));
        return new LoginPage();
    }

    public void clickToolbarButton(String buttonTitle)
    {
        String buttonLocator = "//img[@title = '" + buttonTitle + "']";
        getDriver().findElement(By.xpath(buttonLocator)).click();
    }

    public void clearContent()
    {
        this.contentText.clear();
    }

    public void sendKeys(CharSequence... keys)
    {
        this.contentText.sendKeys(keys);
    }

    /**
     * Allows to send a key combination with Selenium 3.
     * We created this method to avoid some issues while using {@link Keys#chord(CharSequence...)} in Selenium 3.
     * In order to trigger the actions on the specific cursor place, you need to first place your cursor inside the
     * content area by using {@link #sendKeys(CharSequence...)} with arrows.
     *
     * @param modifierKey the meta key to be called for the shortcut (SHIFT/ALT/META)
     * @param keys the key sequence to call during the shortcut.
     */
    public void sendKeysWithAction(Keys modifierKey, CharSequence... keys)
    {
        Actions actions = new Actions(getDriver().getWrappedDriver());
        Method keyUp, keyDown;

        // Actions#keysUp and Actions#keysDown have different signatures in Selenium 2.44 and Selenium 3.14
        // On 2.44 the methods take a Key in argument, in 3.14 they take a CharSequence.
        // As we need to be able to support both version for now, we rely on reflexivity to get the proper methods.
        // TODO: this should be removed when we get rid of Selenium 2.
        try {
            keyDown = Actions.class.getDeclaredMethod("keyDown", Keys.class);
            keyUp = Actions.class.getDeclaredMethod("keyUp", Keys.class);
        } catch (NoSuchMethodException e) {
            try {
                keyDown = Actions.class.getDeclaredMethod("keyDown", CharSequence.class);
                keyUp = Actions.class.getDeclaredMethod("keyUp", CharSequence.class);
            } catch (NoSuchMethodException ex) {
                // this should not happen
                throw new RuntimeException(ex);
            }
        }

        try {
            actions = (Actions) keyDown.invoke(actions, modifierKey);
            actions.sendKeys(keys);
            actions = (Actions) keyUp.invoke(actions, modifierKey);
            actions.build().perform();
        } catch (IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEnabled()
    {
        return this.contentText.isEnabled();
    }
}
