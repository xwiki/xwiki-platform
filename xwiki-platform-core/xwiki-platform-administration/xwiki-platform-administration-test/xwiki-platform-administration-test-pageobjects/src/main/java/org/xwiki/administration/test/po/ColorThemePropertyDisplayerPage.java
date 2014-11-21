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
import org.xwiki.test.ui.po.ViewPage;

/**
 * Class that represents the page which hold the displayer for the 'color theme' property of the Administration.
 *
 * @since 6.3M2
 * @version $Id$
 */
public class ColorThemePropertyDisplayerPage extends ViewPage
{
    /**
     * The select input to set the color theme.
     */
    @FindBy(id = "${prefix}${name}")
    private WebElement iconThemeInput;

    public static ColorThemePropertyDisplayerPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "ColorThemePropertyDisplayer");
        return new ColorThemePropertyDisplayerPage();
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
}
