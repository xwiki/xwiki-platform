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
package org.xwiki.test.ui.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible on the Copy Page page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class CopyPage extends ViewPage
{
    /**
     * The select box containing the list of available spaces.
     */
    @FindBy(id = "targetSpaceName")
    private WebElement targetSpaceName;

    /**
     * The text input field to enter the name of the target page.
     */
    @FindBy(id = "targetPageName")
    private WebElement targetPageName;

    /**
     * The copy button.
     */
    @FindBy(xpath = "//input[@class = 'button' and @value = 'Copy']")
    private WebElement copyButton;

    /**
     * Sets the name of the space where the page should be copied.
     * 
     * @param targetSpaceName the name of the space where the page should be copied
     */
    public void setTargetSpaceName(String targetSpaceName)
    {
        this.targetSpaceName.clear();
        this.targetSpaceName.sendKeys(targetSpaceName);
    }

    /**
     * Sets the name of the target page.
     * 
     * @param targetPageName the name of the target page
     */
    public void setTargetPageName(String targetPageName)
    {
        this.targetPageName.clear();
        this.targetPageName.sendKeys(targetPageName);
    }

    /**
     * Submit the copy page form.
     * 
     * @return the confirmation page
     */
    public CopyConfirmationPage clickCopyButton()
    {
        this.copyButton.submit();
        return new CopyConfirmationPage();
    }
}
