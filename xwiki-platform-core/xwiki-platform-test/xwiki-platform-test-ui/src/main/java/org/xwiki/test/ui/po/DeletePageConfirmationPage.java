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

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Specialized confirmation page, dedicated to pages deletion.
 * Allows to interact with the form that offers the choice between removing a document permanently
 * or sending it to the recycle bin, in addition to the interactions already allowed by {@link ConfirmationPage}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class DeletePageConfirmationPage extends ConfirmationPage
{
    @FindBy(css = "input[name='shouldSkipRecycleBin'][value='false']")
    private WebElement optionToRecycleBin;

    @FindBy(css = "input[name='shouldSkipRecycleBin'][value='true']")
    private WebElement optionSkipRecycleBin;

    /**
     * Click on the option to put the document in the recycle bin.
     */
    public void selectOptionToRecycleBin()
    {
        this.optionToRecycleBin.click();
    }

    /**
     * Click on the option to remove permanently the document.
     */
    public void selectOptionSkipRecycleBin()
    {
        this.optionSkipRecycleBin.click();
    }

    /**
     * @return {@code true} if the form proposing to skip the recycle bin is displayed, {@code false} otherwise
     */
    public boolean isRecycleBinOptionsDisplayed()
    {
        try {
            return this.optionSkipRecycleBin.isDisplayed() && this.optionSkipRecycleBin.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Confirm the deletion of the page.
     * @return an object representing the UI displayed when a page is deleted
     */
    public DeletingPage confirmDeletePage()
    {
        clickYes();
        return new DeletingPage();
    }
}
