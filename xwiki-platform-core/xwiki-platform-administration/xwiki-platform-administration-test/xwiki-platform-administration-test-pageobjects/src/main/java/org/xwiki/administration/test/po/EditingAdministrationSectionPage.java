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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the actions possible on the Editing administration section, where the wiki-level default editor (the
 * default edit mode used when editing a page) can be configured.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
public class EditingAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "Editing";

    /**
     * The dropdown used to select the default editor (i.e. the default edit mode). The empty value means "Default",
     * "Text" means the Wiki editor, and "Wysiwyg" means the WYSIWYG editor.
     */
    @FindBy(name = "XWiki.XWikiPreferences_0_editor")
    private WebElement defaultEditorSelect;

    /**
     * Open the Editing administration section.
     *
     * @return the Editing administration section
     */
    public static EditingAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new EditingAdministrationSectionPage();
    }

    public EditingAdministrationSectionPage()
    {
        super(SECTION_ID);
    }

    /**
     * @return the dropdown element used to select the default editor
     */
    public Select getDefaultEditorSelect()
    {
        return new Select(this.defaultEditorSelect);
    }

    /**
     * Set the default editor and save the changes.
     *
     * @param value the value of the editor to select ({@code ""} for "Default", {@code "Text"} for the Wiki editor and
     *            {@code "Wysiwyg"} for the WYSIWYG editor)
     */
    public void setDefaultEditor(String value)
    {
        getDefaultEditorSelect().selectByValue(value);
        clickSave();
    }

    /**
     * @return the value of the currently selected default editor ({@code ""} for "Default", {@code "Text"} for the Wiki
     *         editor and {@code "Wysiwyg"} for the WYSIWYG editor)
     */
    public String getDefaultEditor()
    {
        return getDefaultEditorSelect().getFirstSelectedOption().getAttribute("value");
    }
}
