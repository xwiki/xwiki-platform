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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * This is the old ConfirmationModal implementation based on Prototype.js.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class ConfirmationModal extends ViewPage
{
    @FindBy(xpath = "//div[@class='buttons']//input[@value='Yes']")
    private WebElement buttonOk;

    @FindBy(xpath = "//div[@class='buttons']//input[@value='No']")
    private WebElement buttonCancel;

    public void clickOk()
    {
        getDriver().waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        this.buttonOk.click();
    }

    public void clickCancel()
    {
        getDriver().waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        this.buttonCancel.click();
    }
}
