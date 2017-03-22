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
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the content of the Page Layout tab from the Panels administration section.
 * 
 * @version $Id$
 * @since 9.2RC1
 */
public class PageLayoutTabContent extends BaseElement
{
    @FindBy(css = "#XWiki\\.XWikiPreferences_0_rightPanels")
    private WebElement rightPanelsInput;

    @FindBy(id = "rightcolumn")
    private WebElement rightColumnLayout;

    public PageLayoutTabContent selectRightColumnLayout()
    {
        this.rightColumnLayout.click();
        return this;
    }

    public String getRightPanels()
    {
        return this.rightPanelsInput.getText();
    }

    public PageLayoutTabContent setRightPanels(String rightPanels)
    {
        this.rightPanelsInput.clear();
        this.rightPanelsInput.sendKeys(rightPanels);
        return this;
    }
}
