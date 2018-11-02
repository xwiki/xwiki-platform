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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.Select;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents a template provider page in inline mode
 *
 * @version $Id$
 * @since 4.2M1
 */
public class TemplateProviderInlinePage extends InlinePage
{
    public static final String ACTION_SAVEANDEDIT = "saveandedit";

    public static final String ACTION_EDITONLY = "edit";

    @FindBy(name = "XWiki.TemplateProviderClass_0_template")
    private WebElement templateInput;

    @FindBy(name = "XWiki.TemplateProviderClass_0_name")
    private WebElement templateNameInput;

    @FindBy(name = "XWiki.TemplateProviderClass_0_type")
    private WebElement templateTypeSelect;

    @FindBy(name = "XWiki.TemplateProviderClass_0_terminal")
    private WebElement terminalSelect;

    @FindBy(name = "XWiki.TemplateProviderClass_0_action")
    private WebElement templateActionSelect;

    private LocationPicker creationRestrictionsPicker =
        new LocationPicker("XWiki.TemplateProviderClass_0_creationRestrictions");

    @FindBy(name = "XWiki.TemplateProviderClass_0_creationRestrictionsAreSuggestions")
    private WebElement creationRestrictionsAreSuggestionsCheckbox;

    private LocationPicker visibilityRestrictionsPicker =
        new LocationPicker("XWiki.TemplateProviderClass_0_visibilityRestrictions");

    private SuggestInputElement templateSuggestInput;

    public TemplateProviderInlinePage()
    {
        this.templateSuggestInput = new SuggestInputElement(this.templateInput);
    }

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
        List<String> values = this.templateSuggestInput.getValues();
        if (values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    public void setTemplate(String value)
    {
        this.templateSuggestInput.clearSelectedSuggestions();
        this.templateSuggestInput.sendKeys(value);
        this.templateSuggestInput.selectTypedText();
    }

    public boolean isPageTemplate()
    {
        return this.templateTypeSelect.findElement(By.xpath("//option[@value='page']")).isSelected();
    }

    public void setPageTemplate(boolean isPageTemplate)
    {
        Select select = new Select(this.templateTypeSelect);

        String value;
        if (isPageTemplate) {
            value = "page";
        } else {
            value = "space";
        }

        select.selectByValue(value);
    }

    public boolean isTerminal()
    {
        return this.terminalSelect.findElement(By.xpath("//option[@value='1']")).isSelected();
    }

    public void setTerminal(boolean isTerminal)
    {
        Select select = new Select(this.terminalSelect);

        String value;
        if (isTerminal) {
            value = "1";
        } else {
            value = "0";
        }

        select.selectByValue(value);
    }

    /**
     * @return the list of spaces
     * @since 8.2M3 (renamed from getSpaces)
     */
    public List<String> getVisibilityRestrictions()
    {
        return this.visibilityRestrictionsPicker.getValue();
    }

    /**
     * @param spaces the list of spaces
     * @since 8.2M3 (renamed from setSpaces)
     */
    public void setVisibilityRestrictions(List<String> spaces)
    {
        this.visibilityRestrictionsPicker.setValue(spaces);
    }

    /**
     * @param spaces the list of spaces to set
     * @since 8.3M2
     */
    public void setCreationRestrictions(List<String> spaces)
    {
        this.creationRestrictionsPicker.setValue(spaces);
    }

    /**
     * @return the list of spaces
     * @since 8.2M2
     */
    public List<String> getCreationRestrictions()
    {
        return this.creationRestrictionsPicker.getValue();
    }

    /**
     * The action to execute when the create button is pushed, you can configure here whether the new document is saved
     * before it is opened for edition or not.
     *
     * @param actionName the behavior to have on create; valid values are "saveandedit" and "edit". See
     *            {@link #ACTION_EDITONLY} and {@link #ACTION_SAVEANDEDIT}
     * @since 7.2M2
     */
    public void setActionOnCreate(String actionName)
    {
        this.templateActionSelect.findElement(By.xpath("//option[@value='" + actionName + "']")).click();
    }

    /**
     * @return true if the creationRestrictions are suggestions, false otherwise
     * @since 8.3M2
     */
    public boolean isCreationRestrictionsSuggestions()
    {
        return this.creationRestrictionsAreSuggestionsCheckbox.isSelected();
    }

    /**
     * @param selected true if the creationRestrictions should be suggestions, false otherwise
     * @since 8.3M2
     */
    public void setCreationRestrictionsSuggestions(boolean selected)
    {
        if (this.creationRestrictionsAreSuggestionsCheckbox.isSelected() != selected) {
            this.creationRestrictionsAreSuggestionsCheckbox.click();
        }
    }
}
