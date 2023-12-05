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
package org.xwiki.skin.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.InlinePage;

/**
 * Extends the InlinePage to add skin fields.
 *
 * @version $Id$
 * @since 15.9RC1
 */
public class SkinInlinePage extends InlinePage
{
    @FindBy(id = "newPath")
    private WebElement templatePathInput;

    @FindBy(id = "newPathAdd")
    private WebElement okButton;

    @FindBy(id = "overrideSection")
    private WebElement overrideSection;

    public SkinTemplateElement addTemplate(String templatePath)
    {
        this.templatePathInput.clear();
        this.templatePathInput.sendKeys(templatePath);
        this.okButton.click();

        return getTemplate(templatePath, false);
    }

    public SkinTemplateElement getTemplate(String templatePath, boolean open)
    {
        WebElement overrideObject = this.overrideSection.findElement(
            By.xpath("//div[@class = 'overrideObject'][h3[@id= 'H" + templatePath + "']]"));

        if (open) {
            overrideObject.click();
        }

        return new SkinTemplateElement(overrideObject.findElement(By.xpath("div[@class = 'overrideProperties']")));
    }
}
