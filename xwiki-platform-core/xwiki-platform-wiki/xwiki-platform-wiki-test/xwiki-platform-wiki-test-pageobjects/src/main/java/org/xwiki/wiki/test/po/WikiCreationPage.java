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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class WikiCreationPage extends ExtendedViewPage
{
    @FindBy(id = "finalize")
    private WebElement finalizeButton;

    @FindBy(xpath = "//div[@class='wizard-header']/h1/span")
    private WebElement stepTitle;

    @FindBy(id = "creation-log")
    private WebElement log;

    public static String getSpace()
    {
        return "WikiManager";
    }

    public static String getPage()
    {
        return "CreateNewWiki";
    }

    public WikiHomePage finalizeCreation()
    {
        // The finalize button is not visible until the provisioning is done, so we wait for it
        getDriver().waitUntilElementIsVisible(By.id("finalize"), 30);
        // Now we can click
        finalizeButton.click();
        // And wait for the page to be loaded
        waitUntilPageIsLoaded();
        // And now we are in the home page of the new created wiki
        return new WikiHomePage();
    }

    public String getStepTitle()
    {
        return stepTitle.getText();
    }

    public boolean isFinalizeButtonEnabled()
    {
        return finalizeButton.isEnabled();
    }

    public boolean isFinalizeButtonVisible()
    {
        return finalizeButton.isDisplayed();
    }

    public void waitForFinalizeButton(int timeout)
    {
        // The finalize button is not visible until the provisioning is done, so we wait for it
        getDriver().waitUntilElementIsVisible(By.id("finalize"), timeout);
    }

    public boolean hasLogError()
    {
        return !getDriver().findElementsWithoutWaiting(
                By.xpath("div[@id='creation-log']//li[contains(@class, 'log-item-error')]")).isEmpty();
    }
}
