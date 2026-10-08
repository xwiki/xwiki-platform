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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.editor.EditPage;

/** User profile, the preferences pane, edit mode. */
public class PreferencesEditPage extends EditPage
{
    @FindBy(id = "XWiki.XWikiUsers_0_editor")
    private WebElement defaultEditor;

    @FindBy(id = "XWiki.XWikiUsers_0_usertype")
    private WebElement userType;

    @FindBy(id = "XWiki.XWikiUsers_0_timezone")
    private WebElement timezone;

    @FindBy(id = "XWiki.XWikiUsers_0_shortcut_view_edit")
    private WebElement shortcutViewEdit;

    @FindBy(id = "XWiki.XWikiUsers_0_shortcut_view_information")
    private WebElement shortcutInformation;

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

    public void setShortcutViewEdit(String shortcutValue)
    {
        getDriver().scrollTo(this.shortcutViewEdit);
        this.shortcutViewEdit.clear();
        this.shortcutViewEdit.sendKeys(shortcutValue);
        if (!shortcutValue.equals("")) {
            getDriver().waitUntilElementHasNonEmptyAttributeValue(By.id("XWiki.XWikiUsers_0_shortcut_view_edit"),
                "value");
        }
    }

    public void setShortcutInformation(String shortcutValue)
    {
        getDriver().scrollTo(this.shortcutInformation);
        this.shortcutInformation.clear();
        this.shortcutInformation.sendKeys(shortcutValue);
        if (!shortcutValue.equals("")) {
            getDriver().waitUntilElementHasNonEmptyAttributeValue(By.id("XWiki.XWikiUsers_0_shortcut_view_information"),
                "value");
        }
    }
}
