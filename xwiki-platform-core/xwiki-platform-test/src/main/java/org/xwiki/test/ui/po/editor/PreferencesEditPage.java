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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/** User profile, the preferences pane, edit mode. */
public class PreferencesEditPage extends EditPage
{
    private static final String SIMPLE_USER = "Simple";

    private static final String ADVANCED_USER = "Advanced";

    private static final String EDITOR_DEFAULT = "---";

    private static final String EDITOR_WYSIWYG = "Wysiwyg";

    private static final String EDITOR_TEXT = "Text";

    @FindBy(xpath = "//select[@id='XWiki.XWikiUsers_0_editor']")
    private WebElement defaultEditor;

    @FindBy(xpath = "//select[@id='XWiki.XWikiUsers_0_usertype']")
    private WebElement userType;

    public void setSimpleUserType()
    {
        Select select = new Select(this.userType);
        select.selectByValue(SIMPLE_USER);
    }

    public void setAdvancedUserType()
    {
        Select select = new Select(this.userType);
        select.selectByValue(ADVANCED_USER);
    }

    public void setDefaultEditorDefault()
    {
        Select select = new Select(this.defaultEditor);
        select.selectByValue(EDITOR_DEFAULT);
    }

    public void setDefaultEditorWysiwyg()
    {
        Select select = new Select(this.defaultEditor);
        select.selectByValue(EDITOR_WYSIWYG);
    }

    public void setDefaultEditorText()
    {
        Select select = new Select(this.defaultEditor);
        select.selectByValue(EDITOR_TEXT);
    }
}
