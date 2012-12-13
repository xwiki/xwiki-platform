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

/**
 * Represents the actions possible on the Page Elements administration section.
 * 
 * @version $Id$
 * @since 4.3.1
 */
public class PageElementsAdministrationSectionPage extends AdministrationSectionPage
{
    /**
     * The text input used to specify the list of panels to be displayed on the right column.
     */
    @FindBy(id = "XWiki.XWikiPreferences_0_rightPanels")
    private WebElement rightPanelsInput;

    /**
     * Default constructor.
     */
    public PageElementsAdministrationSectionPage()
    {
        super("Elements");
    }

    /**
     * Open the "Page Elements" administration section.
     * 
     * @return the "Page Elements" administration section
     */
    public static PageElementsAdministrationSectionPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=Elements");
        return new PageElementsAdministrationSectionPage();
    }

    /**
     * Sets the list of the panels to be displayed on the right column
     * 
     * @param rightPanels a comma-separated list of panel document names
     */
    public void setRightPanels(String rightPanels)
    {
        rightPanelsInput.clear();
        rightPanelsInput.sendKeys(rightPanels);
    }

    /**
     * @return the list of panels that are displayed on the right column
     */
    public String getRightPanels()
    {
        return rightPanelsInput.getAttribute("value");
    }
}
