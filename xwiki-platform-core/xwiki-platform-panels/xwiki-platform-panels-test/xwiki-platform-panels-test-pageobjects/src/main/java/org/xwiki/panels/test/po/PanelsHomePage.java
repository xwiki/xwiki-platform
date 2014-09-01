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
package org.xwiki.panels.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the Panels.WebHome page.
 * 
 * @version $Id$
 * @since 4.3.1
 */
public class PanelsHomePage extends ViewPage
{
    /**
     * The text input used to specify the title of the panel to create.
     */
    @FindBy(id = "panelTitle")
    private WebElement panelTitleInput;

    /**
     * Go to the home page of the Panels application.
     */
    public static PanelsHomePage gotoPage()
    {
        getUtil().gotoPage(getSpace(), getPage());
        return new PanelsHomePage();
    }

    public static String getSpace()
    {
        return "Panels";
    }

    public static String getPage()
    {
        return "WebHome";
    }

    /**
     * Creates a panel with the specified title.
     * 
     * @param title the name of the new panel
     * @return the edit mode for the specified panel
     */
    public PanelEditPage createPanel(String title)
    {
        panelTitleInput.clear();
        panelTitleInput.sendKeys(title);
        panelTitleInput.submit();
        return new PanelEditPage();
    }
}
