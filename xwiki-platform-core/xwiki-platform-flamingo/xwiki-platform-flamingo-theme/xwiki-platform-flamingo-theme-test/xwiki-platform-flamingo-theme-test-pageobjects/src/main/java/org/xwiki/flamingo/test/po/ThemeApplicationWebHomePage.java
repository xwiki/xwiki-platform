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
package org.xwiki.flamingo.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

public class ThemeApplicationWebHomePage extends ViewPage
{
    @FindBy(xpath = "//div[contains(@class, 'current-theme')]//div[@class='theme-info']//h3")
    private WebElement currentTheme;

    @FindBy(id = "newThemeName")
    private WebElement newThemeNameInput;

    @FindBy(xpath = "//form[contains(@class, 'theme-creation-form')]//input[@type='submit']")
    private WebElement createNewThemeButton;

    public static ThemeApplicationWebHomePage gotoPage()
    {
        getUtil().gotoPage("FlamingoThemes", "WebHome");
        return new ThemeApplicationWebHomePage();
    }

    public String getCurrentTheme()
    {
        return currentTheme.getText();
    }

    public List<String> getOtherThemes()
    {
        List<String> otherThemes = new ArrayList<>();
        List<WebElement> elements = getDriver().findElements(By.xpath("//div[contains(@class, 'theme')"
            + "and not(contains(@class, 'current-theme'))]/div[@class='theme-info']//h3"));
        for (WebElement elem: elements) {
            otherThemes.add(elem.getText());
        }
        return otherThemes;
    }

    public void useTheme(String themeName)
    {
        WebElement elem = getDriver().findElement(By.xpath("//div[@class='theme-info']"
            + "//h3/span/span/a[contains(text(), '"+themeName+"')]"
            + "/../../../..//a[contains(text(), 'Use this Theme')]"));
        getDriver().addPageNotYetReloadedMarker();
        elem.click();
        getDriver().waitUntilPageIsReloaded();
    }

    public EditThemePage createNewTheme(String themeName)
    {
        newThemeNameInput.sendKeys(themeName);
        createNewThemeButton.click();
        return new EditThemePage();
    }

    public ViewThemePage seeTheme(String themeName)
    {
        WebElement elem = getDriver().findElement(By.xpath("//div[@class='theme-info']"
            + "//h3/span/span/a[contains(text(), '"+themeName+"')]"));
        elem.click();
        return new ViewThemePage();
    }
}
