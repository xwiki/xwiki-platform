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
package org.xwiki.repository.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.repository.test.po.editor.ExtensionInlinePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionsPage extends ViewPage
{
    @FindBy(id = "browseSubmit")
    private WebElement browseSubmit;

    @FindBy(id = "searchTextInput")
    private WebElement searchInput;

    @FindBy(id = "contributeNameInput")
    private WebElement contributeNameInput;

    @FindBy(id = "contributeSubmit")
    private WebElement contributeSubmit;

    @FindBy(linkText = "Import")
    private WebElement contributeImportLink;

    private ExtensionsLiveTableElement liveTable;

    public static ExtensionsPage gotoPage()
    {
        getUtil().gotoPage("Extension", "WebHome");
        return new ExtensionsPage();
    }

    public ExtensionsLiveTableElement getLiveTable()
    {
        if (this.liveTable == null) {
            this.liveTable = new ExtensionsLiveTableElement();
            this.liveTable.waitUntilReady();
            
        }

        return this.liveTable;
    }

    public ExtensionInlinePage contributeExtension(String extensionName)
    {
        this.contributeNameInput.clear();
        this.contributeNameInput.sendKeys(extensionName);
        this.contributeSubmit.click();

        return new ExtensionInlinePage();
    }
    
    public ExtensionImportPage clickImport()
    {
        this.contributeImportLink.click();

        return new ExtensionImportPage();
    }

    /**
     * @since 8.3RC1
     */
    public ExtensionsPage clickBrowse()
    {
        this.browseSubmit.click();

        return new ExtensionsPage();
    }
}
