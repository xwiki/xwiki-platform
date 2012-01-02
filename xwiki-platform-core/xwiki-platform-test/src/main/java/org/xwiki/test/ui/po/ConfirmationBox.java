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
import org.openqa.selenium.support.FindBys;

/**
 * Represents a confirmation modal box.
 * 
 * @version $Id$
 * @since 3.4M1
 */
public class ConfirmationBox extends BaseElement
{
    @FindBys({
        @FindBy(className = "xdialog-box-confirmation"),
        @FindBy(xpath = "//input[@type = 'button' and @value = 'Yes']")
    })
    private WebElement yesButton;

    @FindBys({
        @FindBy(className = "xdialog-box-confirmation"),
        @FindBy(xpath = "//input[@type = 'button' and @value = 'No']")
    })
    private WebElement noButton;

    @FindBys({ @FindBy(className = "xdialog-box-confirmation"), @FindBy(className = "question") })
    private WebElement question;

    /**
     * Click on the Yes button.
     */
    public void clickYes()
    {
        yesButton.click();
    }

    /**
     * Click on the No button.
     */
    public void clickNo()
    {
        noButton.click();
    }

    /**
     * @return the confirmation message
     */
    public String getQuestion()
    {
        return question.getText();
    }
}
