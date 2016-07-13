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
package org.xwiki.repository.test.po.editor;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.repository.test.po.ExtensionPage;
import org.xwiki.test.ui.po.InlinePage;

/**
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionInlinePage extends InlinePage
{
    @FindBy(id = "ExtensionCode.ExtensionClass_0_name")
    private WebElement nameInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_type")
    private WebElement typeInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_summary")
    private WebElement summaryInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_authors")
    private WebElement authorsInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_licenseName")
    private WebElement licenseNameList;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_source")
    private WebElement sourceInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_icon")
    private WebElement iconInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_description")
    private WebElement descriptionInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_customInstallationOnly")
    private WebElement customInstallationOnlyCheckBox;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_installation")
    private WebElement installationInput;

    public void setName(String name)
    {
        this.nameInput.clear();
        this.nameInput.sendKeys(name);
    }

    public String getName()
    {
        return this.nameInput.getAttribute("value");
    }

    public void setType(String type)
    {
        Select select = new Select(this.typeInput);
        select.selectByValue(type);
    }

    public void setSummary(String summary)
    {
        this.summaryInput.clear();
        this.summaryInput.sendKeys(summary);
    }

    public void setAuthors(String author)
    {
        this.authorsInput.clear();
        this.authorsInput.sendKeys(author);
    }

    public void setLicenseName(String licenseName)
    {
        Select select = new Select(this.licenseNameList);
        select.selectByValue(licenseName);
    }

    public void setSource(String source)
    {
        this.sourceInput.clear();
        this.sourceInput.sendKeys(source);
    }

    public void setIcon(String icon)
    {
        this.iconInput.clear();
        this.iconInput.sendKeys(icon);
    }

    public void setDescription(String description)
    {
        this.descriptionInput.clear();
        this.descriptionInput.sendKeys(description);
    }

    public void setCustomInstallationOnly(boolean customInstallationOnly)
    {
        Select select = new Select(this.customInstallationOnlyCheckBox);
        select.selectByValue(customInstallationOnly ? "1" : "0");
    }

    public void setInstallation(String installation)
    {
        this.installationInput.clear();
        this.installationInput.sendKeys(installation);
    }

    @Override
    protected ExtensionPage createViewPage()
    {
        return new ExtensionPage();
    }
}
