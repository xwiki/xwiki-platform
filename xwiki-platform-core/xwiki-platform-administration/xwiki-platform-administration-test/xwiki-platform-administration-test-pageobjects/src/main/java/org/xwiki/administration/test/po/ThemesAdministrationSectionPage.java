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
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.SuggestInputElement;
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
    private WebElement colorThemeInput;

    /**
     * The select input to set the icon theme.
     */
    @FindBy(id = "XWiki.XWikiPreferences_0_iconTheme")
    private WebElement iconThemeInput;

    @FindBy(xpath = "//label[@class='colorTheme']//a[contains(text(), 'Customize')]")
    private WebElement customizeColorThemeButton;

    @FindBy(xpath = "//a[contains(text(), 'Manage color themes')]")
    private WebElement manageColorThemesButton;

    @FindBy(xpath = "//select[@id='XWiki.XWikiPreferences_0_skin']")
    private WebElement skinInput;

    @FindBy(xpath = "//label[@class='skin']//a[contains(text(), 'Customize')]")
    private WebElement customizeSkinButton;

    /**
     * Default constructor.
     */
    public ThemesAdministrationSectionPage()
    {
        super("Themes");
    }

    private List<WebElement> getThemeOptions(WebElement themeInput) {
        return themeInput.findElements(By.tagName("option"));
    }
    private List<WebElement> getColorThemeOptions()
    {
        return getThemeOptions(colorThemeInput);
    }
    private List<WebElement> getIconThemeOptions()
    {
        return getThemeOptions(iconThemeInput);
    }

    private List<WebElement> getColibriThemeOptions()
    {
        return colorThemeInput.findElements(By.xpath("//optgroup[@label='Colibri Themes']//option"));
    }
    private List<WebElement> getFlamingoThemeOptions()
    {
        return colorThemeInput.findElements(By.xpath("//optgroup[@label='Flamingo Themes']//option"));
    }

    
    private List<String> getThemes(WebElement themeInput)
    {
        List<String> results = new ArrayList<>();
        for (WebElement option : getThemeOptions(themeInput)) {
            results.add(option.getText());
        }
        return results;
    }
    
    /**
     * @return the list of available color themes
     */
    public List<String> getColorThemes() {
        return getThemes(colorThemeInput);
    }

    /**
     * @return the list of available color themes
     */
    public List<String> getIconThemes() {
        return getThemes(iconThemeInput);
    }
    
    private void setTheme(String themeName, WebElement themeInput) {
        // Make sure the theme that we want to set is available from the list first
        try {
            getDriver().waitUntilCondition(driver -> getThemeOptionElement(themeName, themeInput) != null);
        } catch (TimeoutException e) {
            // Collect all available options to display a nice message
            List<String> options = new ArrayList<>();
            for (WebElement option : getThemeOptions(themeInput)) {
                options.add(option.getText());
            }
            throw new TimeoutException(String.format("Theme [%s] wasn't found among [%s]", themeName,
                    StringUtils.join(options, ',')), e);
        }
    
        // Click on it to set the theme
        getThemeOptionElement(themeName, themeInput).click();
    
        // Waiting to be sure the change is effective
        getDriver().waitUntilCondition(driver -> StringUtils.equals(getCurrentTheme(themeInput), themeName));
    }

    /**
     * Select the specified color theme.
     * @param colorThemeName name of the color theme to select
     */
    public void setColorTheme(String colorThemeName)
    {
        setTheme(colorThemeName, colorThemeInput);
    }

    /**
     * Select the specified icon theme.
     * @param colorThemeName name of the color theme to select
     */
    public void setIconTheme(String iconThemeName)
    {
        setTheme(iconThemeName, iconThemeInput);
    }

    private WebElement getThemeOptionElement(String themeName, WebElement themeInput) {
        WebElement element = null;
        for (WebElement option : getThemeOptions(themeInput)) {
            if (themeName.equals(option.getText())) {
                element = option;
                break;
            }
        }
        return element;
    }
    
    private WebElement getColorThemeOptionElement(String colorThemeName)
    {
        return getThemeOptionElement(colorThemeName, colorThemeInput);
    }

    private WebElement getIconThemeOptionElement(String iconThemeName)
    {
        return getThemeOptionElement(iconThemeName, iconThemeInput);
    }

    private String getCurrentTheme(WebElement themeInput) {
        for (WebElement option : getThemeOptions(themeInput)) {
            if (option.isSelected()) {
                return option.getText();
            }
        }
        return null;
    }
    /**
     * @return the current color theme
     */
    public String getCurrentColorTheme()
    {
        return getCurrentTheme(colorThemeInput);
    }

    /**
     * @return the current icon theme
     */
    public String getCurrentIconTheme()
    {
        return getCurrentTheme(iconThemeInput);
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
    public void clickOnCustomizeColorTheme()
    {
        getDriver().waitUntilElementIsVisible(
                By.xpath("//label[@class='colorTheme']//a[contains(text(), 'Customize')]"));
        customizeColorThemeButton.click();
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

    /**
     * @since 15.9RC1
     */
    public void setSkin(String skinReference)
    {
        new SuggestInputElement(this.skinInput).sendKeys(skinReference).waitForSuggestions().sendKeys(Keys.ENTER);
    }

    /**
     * @since 15.9RC1
     */
    public void clickOnCustomizeSkin()
    {
        this.customizeSkinButton.click();
    }
}
