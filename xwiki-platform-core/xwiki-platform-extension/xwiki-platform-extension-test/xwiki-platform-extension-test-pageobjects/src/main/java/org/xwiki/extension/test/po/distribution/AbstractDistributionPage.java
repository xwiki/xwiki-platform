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
package org.xwiki.extension.test.po.distribution;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The Distribution Wizard based page.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class AbstractDistributionPage extends BaseElement
{
    @FindBy(className = "steps")
    private WebElement icons;

    @FindBy(xpath = "//button[@value='COMPLETE_STEP']")
    private WebElement completeStepButton;

    public List<DistributionStepIcon> getIcons()
    {
        List<WebElement> listItems = getDriver().findElementsWithoutWaiting(this.icons, By.tagName("li"));

        List<DistributionStepIcon> result = new ArrayList<DistributionStepIcon>(listItems.size());

        for (WebElement listItem : listItems) {
            result.add(new DistributionStepIcon(listItem));
        }

        return result;
    }

    public boolean isCompleteStepDisabled()
    {
        return this.completeStepButton.isDisplayed();
    }

    public void clickCompleteStep()
    {
        this.completeStepButton.click();
    }
}
