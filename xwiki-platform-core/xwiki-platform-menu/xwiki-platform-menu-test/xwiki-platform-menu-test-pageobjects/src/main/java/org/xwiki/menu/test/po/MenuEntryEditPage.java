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
package org.xwiki.menu.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;

/**
 * Represents a Menu entry page in edit mode.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
public class MenuEntryEditPage extends EntryEditPage
{
    @FindBy(id = "XWiki.UIExtensionClass_0_extensionPointId")
    private WebElement extensionPointIdSelectElement;

    @FindBy(id = "XWiki.UIExtensionClass_0_scope")
    private WebElement scopeSelectElement;

    public void setLocation(String value)
    {
        Select select = new Select(this.extensionPointIdSelectElement);
        select.selectByVisibleText(value);
    }

    public void setVisibility(String value)
    {
        Select select = new Select(this.scopeSelectElement);
        select.selectByVisibleText(value);
    }
}
