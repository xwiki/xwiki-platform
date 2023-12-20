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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the actions possible on the WYSIWYG Editor administration section.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class WYSIWYGEditorAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "WYSIWYG";

    /**
     * The drop down used to select the default WYSIWYG editor.
     */
    @FindBy(name = "XWiki.EditorBindingClass_0_roleHint")
    private WebElement defaultWYSIWYGEditorSelect;

    @FindBy(css = "form#wysiwyg input[name='formactionsac'][type='submit']")
    private WebElement saveButton;

    /**
     * Open the WYSIWYG editor administration section.
     * 
     * @return the WYSIWYG editor administration section
     */
    public static WYSIWYGEditorAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new WYSIWYGEditorAdministrationSectionPage();
    }

    public WYSIWYGEditorAdministrationSectionPage()
    {
        super(SECTION_ID);
    }

    /**
     * Use this method to set or get the default WYSIWYG editor.
     * 
     * @return the drop down element used to select the default WYSIWYG editor
     */
    public Select getDefaultWYSIWYGEditorSelect()
    {
        return new Select(this.defaultWYSIWYGEditorSelect);
    }

    public WYSIWYGEditorAdministrationSectionPage setDefaultWYSIWYGEditor(String editorName)
    {
        getDefaultWYSIWYGEditorSelect().selectByVisibleText(editorName);
        // The save action reloads the page.
        this.saveButton.click();
        return new WYSIWYGEditorAdministrationSectionPage();
    }

    /**
     * The configuration properties for each WYSIWYG editor are displayed on separate tabs. Use this method to select
     * the tab that corresponds to the WYSIWYG editor you want to configure.
     * 
     * @param editorId the id of a WYSIWYG editor
     * @return the tab header that corresponds to the specified WYSIWYG editor
     */
    public WebElement getConfigurationTab(String editorId)
    {
        return getDriver().findElement(By.cssSelector("a[role='tab'][data-editorid='" + editorId + "']"));
    }
}
