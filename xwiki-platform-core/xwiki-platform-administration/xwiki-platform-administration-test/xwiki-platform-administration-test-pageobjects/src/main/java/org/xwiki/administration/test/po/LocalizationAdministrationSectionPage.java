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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents the actions possible on the Localization Administration Page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class LocalizationAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(id = "XWiki.XWikiPreferences_0_multilingual")
    private WebElement multiLingualSelect;

    @FindBy(id = "XWiki.XWikiPreferences_0_default_language")
    private WebElement defaultLanguageInput;

    @FindBy(id = "XWiki.XWikiPreferences_0_languages")
    private WebElement supportedLanguagesInput;

    public LocalizationAdministrationSectionPage()
    {
        super("Localization");
    }

    public void setMultiLingual(boolean isMultiLingual)
    {
        Select select = new Select(this.multiLingualSelect);
        if (isMultiLingual) {
            select.selectByIndex(1);
        } else {
            select.selectByIndex(2);
        }
    }

    public void setDefaultLanguage(String defaultLanguage)
    {
        this.defaultLanguageInput.clear();
        this.defaultLanguageInput.sendKeys(defaultLanguage);
    }

    public void setSupportedLanguages(String supportedLanguages)
    {
        this.supportedLanguagesInput.clear();
        this.supportedLanguagesInput.sendKeys(supportedLanguages);
    }
}
