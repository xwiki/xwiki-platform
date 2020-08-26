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
package org.xwiki.flamingo.skin.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ConfirmationPage;

/**
 * Represent the form the offer the choice between removing a document permanently
 * or sending it to the recyclebin.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class DeleteConfirmationPage extends ConfirmationPage
{
    @FindBy(css = "input[name='toRecyclebin'][value='true']")
    private WebElement optionToReyclebin;

    @FindBy(css = "input[name='toRecyclebin'][value='false']")
    private WebElement optionSkipReyclebin;

    /**
     * Click on the option to put the document in the recyclebin.
     */
    public void selectOptionToRecycleBin()
    {
        this.optionToReyclebin.click();
    }

    /**
     * Click on the option to remove permanently the document.
     */
    public void selectOptionSkipRecycleBin()
    {
        this.optionSkipReyclebin.click();
    }
}
