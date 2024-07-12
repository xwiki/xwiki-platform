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
package org.xwiki.ckeditor.test.po;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.WYSIWYGEditorAdministrationSectionPage;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.BootstrapSelect;

/**
 * Represents the CKEditor administration pane.
 * 
 * @version $Id$
 * @since 15.9RC1
 */
public class CKEditorConfigurationPane extends BaseElement
{
    private static final String EDITOR_ID = "ckeditor";

    private final WebElement container;

    @FindBy(css = "#CKEditor\\.ConfigClass_0_removePlugins + .bootstrap-select")
    private WebElement disabledPluginsSelect;

    @FindBy(id = "CKEditor.ConfigClass_0_loadJavaScriptSkinExtensions")
    private WebElement loadJavaScriptCheckBox;

    @FindBy(xpath = "//input[@type='submit'][@name='action_saveandcontinue']")
    private WebElement saveButton;

    /**
     * Navigates to the WYSIWYG editor administration section and opens the CKEditor configuration pane.
     * 
     * @return the CKEditor configuration pane
     */
    public static CKEditorConfigurationPane open()
    {
        return new CKEditorConfigurationPane(
            WYSIWYGEditorAdministrationSectionPage.gotoPage().getConfigurationTab(EDITOR_ID));
    }

    /**
     * Default constructor.
     */
    public CKEditorConfigurationPane()
    {
        this(new WYSIWYGEditorAdministrationSectionPage().getConfigurationTab(EDITOR_ID));
    }

    private CKEditorConfigurationPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the list of disabled CKEditor plugins
     * @since 16.2.0RC1
     */
    public List<String> getDisabledPlugins()
    {
        BootstrapSelect select = new BootstrapSelect(this.disabledPluginsSelect, getDriver());
        return select.getSelectedValues();
    }

    /**
     * Sets the list of disabled CKEditor plugins.
     * 
     * @param pluginNames the list of plugin names to disable
     * @return this object
     */
    public CKEditorConfigurationPane setDisabledPlugins(List<String> pluginNames)
    {
        BootstrapSelect select = new BootstrapSelect(this.disabledPluginsSelect, getDriver());
        select.selectByValues(pluginNames);
        return this;
    }

    /**
     * Sets whether the JavaScript skin extensions should be loaded or not by the standalone WYSIWYG editor.
     * 
     * @param loadJavaScriptSkinExtensions {@code true} to load the JavaScript skin extensions, {@code false} otherwise
     */
    public CKEditorConfigurationPane setLoadJavaScriptSkinExtensions(boolean loadJavaScriptSkinExtensions)
    {
        if (loadJavaScriptSkinExtensions != this.loadJavaScriptCheckBox.isSelected()) {
            this.loadJavaScriptCheckBox.click();
        }
        return this;
    }

    /**
     * Clicks the save button and waits for the save confirmation message.
     */
    public CKEditorConfigurationPane clickSave()
    {
        return clickSave(true);
    }

    public CKEditorConfigurationPane clickSave(boolean wait)
    {
        this.saveButton.click();

        if (wait) {
            // Wait until the page is really saved.
            waitForNotificationSuccessMessage("Saved");
        }

        return this;
    }
}
