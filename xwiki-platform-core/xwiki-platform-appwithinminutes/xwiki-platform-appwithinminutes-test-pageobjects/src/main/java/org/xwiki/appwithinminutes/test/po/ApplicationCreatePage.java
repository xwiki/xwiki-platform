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
package org.xwiki.appwithinminutes.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.DocumentPicker;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the first step of the App Within Minutes wizard.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class ApplicationCreatePage extends ViewPage
{
    /**
     * The widget used to select the application location.
     */
    private DocumentPicker locationPicker = new DocumentPicker();

    @FindBy(id = "wizard-next")
    private WebElement nextStepButton;

    /**
     * Loads the first step of the App Within Minutes wizard
     *
     * @return the page that represents the first step of the App Within Minutes wizard
     */
    public static ApplicationCreatePage gotoPage()
    {
        getUtil().gotoPage("AppWithinMinutes", "CreateApplication", "view", "wizard=true");
        return new ApplicationCreatePage();
    }

    /**
     * Types the given string into the application name input.
     *
     * @param appName the application name
     */
    public void setApplicationName(String appName)
    {
        this.locationPicker.setTitle(appName);
    }

    /**
     * @return the text input where the application name is typed
     */
    public WebElement getApplicationNameInput()
    {
        return this.locationPicker.getTitleInput();
    }

    /**
     * Waits until the preview for the currently inputed application name is displayed.
     */
    public void waitForApplicationNamePreview()
    {
        final String appName = this.locationPicker.getTitle();
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                List<WebElement> previews = driver.findElements(By.className("appName-preview"));
                return previews.size() == 1 && previews.get(0).getText().contains(appName);
            }
        });
    }

    /**
     * Waits until the application name input has an error message.
     */
    public void waitForApplicationNameError()
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector("#appTitle.xErrorField"));
    }

    /**
     * Sets the location where to create the application.
     *
     * @param location the location where to create the application
     * @since 7.3RC1
     */
    public void setLocation(String location)
    {
        this.locationPicker.setParent(location);
    }

    /**
     * @return the application location picker
     * @since 7.4.1
     * @since 8.0M1
     */
    public DocumentPicker getLocationPicker()
    {
        return this.locationPicker;
    }

    /**
     * Clicks on the Next Step button.
     *
     * @return the page that represents the next step of the App Within Minutes wizard
     */
    public ApplicationClassEditPage clickNextStep()
    {
        clickNextStepButton();
        return new ApplicationClassEditPage();
    }

    /**
     * Simply clicks on the Next Stept button, nothing more.
     * <p>
     * You should generally use {@link #clickNextStep()} instead if you are not expecting an error or something outside
     * the normal flow.
     */
    public void clickNextStepButton()
    {
        nextStepButton.click();
    }
}
