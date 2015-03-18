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
package org.xwiki.index.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents the actions that can be done on the Spaces Macro when executed.
 *
 * @version $Id$
 * @since 7.0RC1
 */
public class SpacesMacroPane extends BaseElement
{
    /**
     * The element that needs to be clicked to show/hide the space creation form.
     */
    @FindBy(linkText = "Create a new space")
    private WebElement spaceCreateFormToggleSwitch;

    /**
     * The text field used to input the space name.
     */
    @FindBy(id = "spSpaceCreateTextInput")
    private WebElement spaceNameTextField;

    /**
     * Shows the space creation form, fills the space name text field with the given space name and submits the form.
     *
     * @param spaceName the name of the space to create
     * @return the WYSIWYG edit page for the space home page
     */
    public WYSIWYGEditPage createSpace(String spaceName)
    {
        this.spaceCreateFormToggleSwitch.click();
        this.spaceNameTextField.clear();
        this.spaceNameTextField.sendKeys(spaceName);
        this.spaceNameTextField.submit();
        return new WYSIWYGEditPage();
    }

    public SpaceIndexPage clickSpaceIndex(String spaceName)
    {
        String escapedSpaceName = getUtil().escapeURL(spaceName);

        // Start by finding all li elements with 'xitem' class
        for (WebElement liElement : getDriver().findElementsWithoutWaiting(By.xpath("//li[contains(@class, 'xitem')]")))
        {
            List<WebElement> elements = getDriver().findElementsWithoutWaiting(liElement,
                By.xpath(".//a[contains(@href, 'SpaceIndex?space=" + escapedSpaceName + "')]"));
            if (!elements.isEmpty()) {

                // Make sure we hover before we click since the link is hidden by default.
                // TODO: However since I wasn't able to perform a hover, I'm cheating by removing the class
                // attribute that makes the element hidden.
                getDriver().executeJavascript("arguments[0].setAttribute('class', '')", liElement);

                // Click
                elements.get(0).click();
                return new SpaceIndexPage();
            }
        }

        throw new RuntimeException("Was unable to click on space index for [" + spaceName + "]");
    }

    /**
     * Clicks on the link to the space home page.
     *
     * @param spaceName the space name
     * @return the page object corresponding to the space home page
     */
    public ViewPage clickSpaceHome(String spaceName)
    {
        getDriver().findElement(By.xpath("//div[@class = 'spSpaceName']/a[. = '" + spaceName + "']")).click();
        return new ViewPage();
    }
}
