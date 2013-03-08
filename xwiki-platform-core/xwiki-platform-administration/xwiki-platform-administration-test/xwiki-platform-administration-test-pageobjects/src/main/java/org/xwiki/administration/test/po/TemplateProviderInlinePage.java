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
import org.xwiki.test.ui.po.InlinePage;

/**
 * Represents a template provider page in inline mode
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class TemplateProviderInlinePage extends InlinePage
{
    @FindBy(name = "XWiki.TemplateProviderClass_0_template")
    private WebElement templateInput;

    @FindBy(name = "XWiki.TemplateProviderClass_0_name")
    private WebElement templateNameInput;

    @FindBy(name = "XWiki.TemplateProviderClass_0_type")
    private WebElement templateTypeSelect;

    @FindBy(name = "XWiki.TemplateProviderClass_0_action")
    private WebElement templateActionSelect;

    public String getTemplateName()
    {
        return this.templateNameInput.getAttribute("value");
    }

    public void setTemplateName(String value)
    {
        this.templateNameInput.clear();
        this.templateNameInput.sendKeys(value);
    }

    public String getTemplate()
    {
        return this.templateInput.getAttribute("value");
    }

    public void setTemplate(String value)
    {
        this.templateInput.clear();
        this.templateInput.sendKeys(value);
    }

    public boolean isPageTemplate()
    {
        return this.templateTypeSelect.findElement(By.xpath("//option[@value='page']")).isSelected();
    }

    private List<WebElement> getSpacesInput()
    {
        return getDriver().findElements(By.name("XWiki.TemplateProviderClass_0_spaces"));
    }

    public List<String> getSpaces()
    {
        List<String> spaces = new ArrayList<String>();

        for (WebElement input : getSpacesInput()) {
            spaces.add(input.getAttribute("value"));
        }

        return spaces;
    }

    public void setSpaces(List<String> spaces)
    {
        for (WebElement input : getSpacesInput()) {
            if (input.isSelected()) {
                input.click();
            }
            if (spaces.contains(input.getAttribute("value"))) {
                input.click();
            }
        }
    }

    /**
     * @return the list of _actually_ checked spaces. If none is checked it actually means that the template is
     *         available in all spaces
     */
    public List<String> getSelectedSpaces()
    {
        List<String> selectedSpaces = new ArrayList<String>();

        for (WebElement input : getSpacesInput()) {
            if (input.isSelected()) {
                selectedSpaces.add(input.getAttribute("value"));
            }
        }

        return selectedSpaces;
    }

    /**
     * Sets all spaces besides the ones passed in the list.
     * 
     * @param spaces the spaces to exclude
     */
    public void excludeSpaces(List<String> spaces)
    {
        List<String> selectedSpaces = getSelectedSpaces();
        // if there is no selected space or all the selected spaces will be unchecked upon excluding them
        if (selectedSpaces.size() == 0 || spaces.containsAll(selectedSpaces)) {
            // go through all the spaces and select them, and unselect the ones in the list to exclude
            for (WebElement input : getSpacesInput()) {
                // prevent checking the hidden fields with empty value
                if (!input.isSelected() && input.getAttribute("value").length() > 0) {
                    input.click();
                }
                if (spaces.contains(input.getAttribute("value"))) {
                    input.click();
                }
            }
        } else {
            // go through the spaces and make sure the exclude list is unselected
            for (WebElement input : getSpacesInput()) {
                if (spaces.contains(input.getAttribute("value")) && input.isSelected()) {
                    input.click();
                }
            }
        }
    }

    public void setSaveAndEdit()
    {
        this.templateActionSelect.findElement(By.xpath("//option[@value='saveandedit']")).click();
    }
}
