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

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BootstrapSelect;

/**
 * Represents the actions possible on the Localization Administration Page.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class LocalizationAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(xpath = "//div[contains(@class, 'bootstrap-select')"
        + " and ./select[@id='XWiki.XWikiPreferences_0_multilingual']]")
    private WebElement multiLingualSelect;

    @FindBy(xpath = "//div[contains(@class, 'bootstrap-select')"
        + " and ./select[@id='XWiki.XWikiPreferences_0_default_language']]")
    private WebElement defaultLanguageSelect;

    @FindBy(xpath = "//div[contains(@class, 'bootstrap-select')"
        + " and ../input[@id='XWiki.XWikiPreferences_0_languages']]")
    private WebElement supportedLanguagesSelect;

    @FindBy(id = "XWiki.XWikiPreferences_0_dateformat")
    private WebElement dateFormatInput;

    // Timezone bootstrap select XWiki.XWikiPreferences_0_timezone
    @FindBy(xpath = "//div[contains(@class, 'bootstrap-select')"
        + " and ./select[@id='XWiki.XWikiPreferences_0_timezone']]")
    private WebElement timezoneSelect;

    public LocalizationAdministrationSectionPage()
    {
        super("Localization");
        waitUntilActionButtonIsLoaded();
        // Wait for asynchronous widgets to be loaded
        getDriver().waitUntilCondition(driver -> multiLingualSelect.isDisplayed() && defaultLanguageSelect.isDisplayed()
            && (multiLingualSelect.findElement(By.xpath(".//option[@value='0']")).isSelected()
            || supportedLanguagesSelect.isDisplayed()));
    }

    public void setMultiLingual(boolean isMultiLingual)
    {
        BootstrapSelect select = new BootstrapSelect(this.multiLingualSelect, getDriver());
        select.selectByValue(isMultiLingual ? "1" : "0");
    }

    public void setDefaultLanguage(String defaultLanguage)
    {
        BootstrapSelect select = new BootstrapSelect(this.defaultLanguageSelect, getDriver());
        select.selectByValue(defaultLanguage);
    }

    public void setSupportedLanguages(String supportedLanguages)
    {
        setSupportedLanguages(Arrays.asList(supportedLanguages.split(",")));
    }

    public void setSupportedLanguages(List<String> supportedLanguages)
    {
        BootstrapSelect select = new BootstrapSelect(this.supportedLanguagesSelect, getDriver());
        select.selectByValues(supportedLanguages);
    }

    /**
     * @return the configured date format, or an empty string if not set
     * @since 17.10.4
     * @since 18.2.0RC1
     */
    public String getDateFormat()
    {
        return this.dateFormatInput.getAttribute("value");
    }

    /**
     * Sets the date format to be used in the wiki. The date format should follow the patterns defined in the Java
     * SimpleDateFormat class (e.g., "yyyy-MM-dd" for a date format like 2024-06-30).
     *
     * @param dateFormat the desired date format to be set (e.g., "yyyy-MM-dd")
     * @since 17.10.4
     * @since 18.2.0RC1
     */
    public void setDateFormat(String dateFormat)
    {
        this.dateFormatInput.clear();
        this.dateFormatInput.sendKeys(dateFormat);
    }

    /**
     * Sets the timezone to be used in the wiki. The timezone should be specified using a valid timezone ID (e.g.,
     * "UTC" for Coordinated Universal Time or "America/New_York" for Eastern Time in the United States).
     *
     * @param timezone the desired timezone to be set (e.g., "UTC" or "America/New_York")
     * @since 18.2.0RC1
     * @since 17.10.4
     */
    public void setTimezone(String timezone)
    {
        BootstrapSelect select = new BootstrapSelect(this.timezoneSelect, getDriver());
        select.selectByValue(timezone);
    }

    /**
     * @return the configured timezone, or "System Default" if the default timezone is used
     * @since 18.2.0RC1
     * @since 17.10.4
     */
    public String getTimezone()
    {
        BootstrapSelect select = new BootstrapSelect(this.timezoneSelect, getDriver());
        return select.getDisplayedText();
    }
}
