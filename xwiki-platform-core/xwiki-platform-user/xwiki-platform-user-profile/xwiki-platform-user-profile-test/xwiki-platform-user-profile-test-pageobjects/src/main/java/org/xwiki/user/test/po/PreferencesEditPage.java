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
package org.xwiki.user.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

/** User profile, the preferences pane, edit mode. */
public class PreferencesEditPage extends EditPage
{
    private static final String SHORTCUT_SAVED_MESSAGE = "Updated shortcut preference";

    private static final String SHORTCUT_RESET_MESSAGE = "Reset shortcut preference";

    private static final String INFORMATION_SHORTCUT = "core.shortcuts.view.information";

    @FindBy(id = "XWiki.XWikiUsers_0_editor")
    private WebElement defaultEditor;

    @FindBy(id = "XWiki.XWikiUsers_0_usertype")
    private WebElement userType;

    @FindBy(id = "XWiki.XWikiUsers_0_timezone")
    private WebElement timezone;

    public void setSimpleUserType()
    {
        Select select = new Select(this.userType);
        select.selectByIndex(1);
    }

    public void setAdvancedUserType()
    {
        Select select = new Select(this.userType);
        select.selectByIndex(2);
    }

    public void setDefaultEditorDefault()
    {
        Select select = new Select(this.defaultEditor);
        select.selectByIndex(0);
    }

    public void setDefaultEditorWysiwyg()
    {
        Select select = new Select(this.defaultEditor);
        select.selectByIndex(2);
    }

    public void setDefaultEditorText()
    {
        Select select = new Select(this.defaultEditor);
        select.selectByIndex(1);
    }

    public String getDefaultEditor()
    {
        return new Select(this.defaultEditor).getFirstSelectedOption().getText();
    }

    public void setTimezone(String value)
    {
        // See https://jira.xwiki.org/browse/XWIKI-8905
        // When it's fixed use instead:
        //   Select select = new Select(this.timezone);
        //   select.selectByValue(value);
        getDriver().scrollTo(this.timezone);
        this.timezone.clear();
        this.timezone.sendKeys(value);
    }

    /**
     * Sets the shortcut used to edit the current page with the default editor, and waits for it to be saved.
     *
     * @param shortcutValue the new shortcut, must be different from the current one
     */
    public void setShortcutViewEdit(String shortcutValue)
    {
        setShortcut("core.shortcuts.view.edit", shortcutValue);
    }

    /**
     * Sets the shortcut used to open the information tab of the current page, and waits for it to be saved.
     *
     * @param shortcutValue the new shortcut, must be different from the current one, empty to unbind the shortcut
     */
    public void setShortcutInformation(String shortcutValue)
    {
        setShortcut(INFORMATION_SHORTCUT, shortcutValue);
    }

    /**
     * Resets the shortcut used to open the information tab of the current page to its default value, and waits for
     * the shortcut preference to be removed.
     */
    public void resetShortcutInformation()
    {
        WebElement resetButton = getShortcutInput(INFORMATION_SHORTCUT)
            .findElement(By.xpath("following-sibling::button[contains(@class, 'reset-shortcut-preference')]"));
        getDriver().scrollTo(resetButton);
        resetButton.click();
        waitForNotificationSuccessMessage(SHORTCUT_RESET_MESSAGE);
    }

    /**
     * Sets the shortcut used to cancel the edition, and waits for it to be saved.
     *
     * @param shortcutValue the new shortcut, must be different from the current one
     */
    public void setShortcutEditCancel(String shortcutValue)
    {
        setShortcut("core.shortcuts.edit.cancel", shortcutValue);
    }

    /**
     * Cancel the edition by using a keyboard shortcut made of the Alt key and the given key.
     *
     * @param key the key to press along with the Alt key
     * @return the page that was being edited
     */
    public ViewPage useShortcutKeyForCancellingEdition(String key)
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().createActions().keyDown(Keys.ALT).sendKeys(key).keyUp(Keys.ALT).perform();
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }

    private void setShortcut(String translationKey, String shortcutValue)
    {
        WebElement shortcutInput = getShortcutInput(translationKey);
        getDriver().scrollTo(shortcutInput);
        // Clear the input with key presses, so that the shortcut preference is saved even when it's unbound.
        shortcutInput.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        shortcutInput.sendKeys(shortcutValue);
        // The shortcut preference is saved in the background as soon as it's typed.
        waitForNotificationSuccessMessage(SHORTCUT_SAVED_MESSAGE);
    }

    private WebElement getShortcutInput(String translationKey)
    {
        return getDriver().findElement(
            By.cssSelector(String.format("input.shortcutPreference[data-translate-key='%s']", translationKey)));
    }
}
