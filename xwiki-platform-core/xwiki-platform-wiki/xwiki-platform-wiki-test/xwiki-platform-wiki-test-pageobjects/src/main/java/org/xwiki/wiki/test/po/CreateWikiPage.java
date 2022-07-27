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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class CreateWikiPage extends ExtendedViewPage
{
    @FindBy(name = "wikiprettyname")
    private WebElement prettyNameField;

    @FindBy(name = "wikiname")
    private WebElement wikiNameField;

    @FindBy(name = "description")
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
