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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the actions possible on the WYSIWYG Editor administration section.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class WYSIWYGEditorAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(xpath = "//input[@type = 'submit' and @name = 'action_saveandcontinue']")
    private WebElement saveButton;

    /**
     * The text input used to enable a WYSIWYG editor plugin.
     */
    @FindBy(xpath = "//div[@class = 'hSortableWrapper'][1]//input[@type = 'text']")
    private WebElement pluginInput;

    /**
     * The button used to enable a WYSIWYG editor plugin.
     */
    @FindBy(xpath = "//div[@class = 'hSortableWrapper'][1]//input[@type = 'image']")
    private WebElement enablePluginButton;

    public WYSIWYGEditorAdministrationSectionPage()
    {
        super("WYSIWYG");
    }

    /**
     * @return the list of WYSIWYG editor plugins that are enabled
     */
    public List<String> getEnabledPlugins()
    {
        List<String> plugins = new ArrayList<String>();
        for (WebElement value : getDriver().findElements(
            By.xpath("//div[@class = 'hSortableWrapper'][1]//span[@class = 'value']"))) {
            plugins.add(value.getText());
        }
        return plugins;
    }

    /**
     * Enable the specified WYSIWYG editor plugin.
     * 
     * @param name the name of the WYSIWYG editor plugin to enable
     */
    public void enablePlugin(String name)
    {
        pluginInput.clear();
        pluginInput.sendKeys(name);
        enablePluginButton.click();
    }

    /**
     * Overwrite because the WYSIWYG Editor administration section uses a different Save button.
     */
    @Override
    public void clickSave()
    {
        this.saveButton.click();
        // The configuration is saved without reloading the page. Wait until the page is really saved.
        waitForNotificationSuccessMessage("Saved");
    }
}
