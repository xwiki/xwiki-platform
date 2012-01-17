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
 * Represents the common actions possible after a Page has been deleted.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class DeleteConfirmationPage extends ViewPage
{
    @FindBy(xpath = "//table[@class='centered']//span[@class='wikilink']")
    private WebElement deletingUser;

    @FindBy(xpath = "//p[@class='xwikimessage']")
    private WebElement confirmationMessage;

    /**
     * @since 3.2M3
     */
    public String getPageDeleter()
    {
        return this.deletingUser.getText();
    }

    /**
     * @since 3.2M3
     */
    public String getConfirmationMessage()
    {
        return this.confirmationMessage.getText();
    }

}
