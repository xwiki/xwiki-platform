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
package org.xwiki.wiki.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class CreateWikiPage extends ExtendedViewPage
{
    @FindBy(name = "wikiprettyname")
    private WebElement prettyNameField;

    @FindBy(name = "wikiname")
    private WebElement wikiNameField;

    @FindBy(css = "form textarea[name='description']")
    private WebElement descriptionField;

    @FindBy(name = "template")
    private WebElement templateField;

    @FindBy(name = "set_as_template")
    private WebElement setAsTemplateField;

    @FindBy(id = "wizard-next")
    private WebElement nextStepButton;

    public void setPrettyName(String prettyName)
    {
        prettyNameField.clear();
        prettyNameField.sendKeys(prettyName);
    }

    /**
     * @param wikiName the wiki identifier to type in the identifier field, which stops the identifier from being
     *     computed from the pretty name
     * @since 18.8.0RC1
     */
    public void setWikiName(String wikiName)
    {
        this.wikiNameField.clear();
        this.wikiNameField.sendKeys(wikiName);
    }

    /**
     * Empty the wiki pretty name field.
     *
     * @since 18.8.0RC1
     */
    public void clearPrettyName()
    {
        clearField(this.prettyNameField);
    }

    /**
     * Empty the wiki identifier field.
     *
     * @since 18.8.0RC1
     */
    public void clearWikiName()
    {
        clearField(this.wikiNameField);
    }

    /**
     * @return {@code true} if the button leading to the next step of the wizard is enabled
     * @since 18.8.0RC1
     */
    public boolean isNextStepEnabled()
    {
        return this.nextStepButton.isEnabled();
    }

    /**
     * Wait until the button leading to the next step of the wizard is enabled. The identifier is validated on the
     * server, so the button can stay disabled for a while after the form has been filled.
     *
     * @since 18.8.0RC1
     */
    public void waitUntilNextStepIsEnabled()
    {
        getDriver().waitUntilElementIsEnabled(this.nextStepButton);
    }

    /**
     * Wait until the button leading to the next step of the wizard is disabled.
     *
     * @since 18.8.0RC1
     */
    public void waitUntilNextStepIsDisabled()
    {
        getDriver().waitUntilElementIsDisabled(this.nextStepButton);
    }

    /**
     * Wait until the identifier field reports the given validation message.
     *
     * @param message the expected validation message
     * @since 18.8.0RC1
     */
    public void waitForWikiNameValidationMessage(String message)
    {
        getDriver().waitUntilElementHasTextContent(By.id("wikinamevalidation"), message);
    }

    /**
     * @return the validation message currently displayed for the pretty name field
     * @since 18.8.0RC1
     */
    public String getPrettyNameValidationMessage()
    {
        return getDriver().findElement(By.id("wikiprettynamevalidation")).getText();
    }

    private void clearField(WebElement field)
    {
        field.clear();
        // clear() only fires a change event, so also send a keystroke to make sure the validation runs whatever the
        // browser does with an already empty field.
        field.sendKeys(Keys.BACK_SPACE);
    }

    public String getName()
    {
        return wikiNameField.getAttribute("value");
    }

    /**
     * @since 6.0M1
     */
    public String getComputedName()
    {
        getDriver().waitUntilElementHasNonEmptyAttributeValue(By.name("wikiname"), "value");
        return getName();
    }

    public void setDescription(String description)
    {
        descriptionField.clear();
        descriptionField.sendKeys(description);
    }

    public void setIsTemplate(boolean template)
    {
        if (template != setAsTemplateField.isSelected()) {
            setAsTemplateField.click();
        }
    }

    public void setTemplate(String templateId)
    {
        List<WebElement> elements = templateField.findElements(By.tagName("option"));
        for (WebElement element : elements) {
            if (element.getAttribute("value").equals(templateId)) {
                element.click();
            }
        }
    }

    public List<String> getTemplateList()
    {
        List<String> list = new ArrayList<>();
        List<WebElement> elements = templateField.findElements(By.tagName("option"));
        for (WebElement element : elements) {
            list.add(element.getAttribute("value"));
        }
        return list;
    }

    public CreateWikiPageStepUser goUserStep()
    {
        goNextStep();
        return new CreateWikiPageStepUser();
    }

    public void goNextStep()
    {
        // Wait for the button to ne enabled since by default it's disabled and becomes enabled only after some
        // fields have been set on the first step UI (using JS), and thus there could be some small delay before it
        // becomes enabled.
        getDriver().waitUntilElementIsEnabled(this.nextStepButton);
        this.nextStepButton.click();
    }
}
