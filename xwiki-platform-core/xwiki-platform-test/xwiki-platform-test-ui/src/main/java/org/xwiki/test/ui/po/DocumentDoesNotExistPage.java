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
 * Page to handle the "Document does not exist" page.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class DocumentDoesNotExistPage extends BasePage
{
    @FindBy(css = "#contentmenu #tmEdit > a.btn")
    private WebElement createButton;

    /**
     * Clicks the button used to create the missing page.
     *
     * @since 19.0.0RC1
     */
    public void clickCreate()
    {
        this.createButton.click();
    }

    /**
     * Clicks the button used to create the missing page.
     *
     * @deprecated since 19.0.0RC1, use {@link #clickCreate()} instead, the missing page doesn't have an "edit this
     *             page" link anymore
     */
    @Deprecated(since = "19.0.0RC1")
    public void clickEditThisPageToCreate()
    {
        clickCreate();
    }
}
