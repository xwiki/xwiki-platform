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
import org.xwiki.text.StringUtils;

/**
 * Represents the actions possible on the Themes administration section.
 *
 * @version $Id$
 * @since 6.3M1
 */
public class ThemesAdministrationSectionPage extends AdministrationSectionPage
{
    /**
     * The select input to set the color theme.
     */
    @FindBy(id = "XWiki.XWikiPreferences_0_colorTheme")
    private WebElement iconThemeInput;

    @FindBy(xpath = "//label[@class='colorTheme']//a[contains(text(), 'Customize')]")
    private WebElement customizeButton;

    @FindBy(xpath = "//a[contains(text(), 'Manage color themes')]")
    private WebElement manageColorThemesButton;

    /**
     * Default constructor.
     */
    public ThemesAdministrationSectionPage()
    {
        super("Themes");
    }

    private List<WebElement> getColorThemeOptions()
    {
        return iconThemeInput.findElements(By.tagName("option"));
    }

    private List<WebElement> getColibriThemeOptions()
    {
        return iconThemeInput.findElements(By.xpath("//optgroup[@label='Colibri Themes']//option"));
    }
    private List<WebElement> getFlamingoThemeOptions()
    {
        return iconThemeInput.findElements(By.xpath("//optgroup[@label='Flamingo Themes']//option"));
    }

    /**
     * @return the list of available color themes
     */
    public List<String> getColorThemes()
    {
        List<String> results = new ArrayList<>();
        for (WebElement option : getColorThemeOptions()) {
            results.add(option.getText());
        }
        return results;
    }

    /**
     * Select the specified color theme.
     * @param colorThemeName name of the color theme to select
     */
    public void setColorTheme(String colorThemeName)
    {
        boolean exist = false;
        for (WebElement option : getColorThemeOptions()) {
            if (colorThemeName.equals(option.getText())) {
                option.click();
                exist = true;
                break;
            }
        }

        if (exist) {
            // Waiting to be sure the change is effective
            getDriver().waitUntilCondition(driver -> StringUtils.equals(getCurrentColorTheme(), colorThemeName));
        } else {
            throw new RuntimeException(String.format("Couldn't find color theme [%s] in the select", colorThemeName));
        }
    }

    /**
     * @return the current color theme
     */
    public String getCurrentColorTheme()
    {
        for (WebElement option : getColorThemeOptions()) {
            if (option.isSelected()) {
                return option.getText();
            }
        }
        return null;
    }

    /**
     * @return the list of colibri themes
     */
    public List<String> getColibriColorThemes()
    {
        List<String> results = new ArrayList<>();
        for (WebElement option : getColibriThemeOptions()) {
            results.add(option.getText());
        }
        return results;
    }

    /**
     * @return the list of flamingo themes
     */
    public List<String> getFlamingoThemes()
    {
        List<String> results = new ArrayList<>();
        for (WebElement option : getFlamingoThemeOptions()) {
            results.add(option.getText());
        }
        return results;
    }

    /**
     * Click on the 'customize' button
     */
    public void clickOnCustomize()
    {
        getDriver().waitUntilElementIsVisible(
                By.xpath("//label[@class='colorTheme']//a[contains(text(), 'Customize')]"));
        customizeButton.click();
    }

    /**
     * Click on "manage color theme".
     *
     * @since 6.3RC1
     */
    public void manageColorThemes()
    {
        manageColorThemesButton.click();
    }
}
