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
package org.xwiki.index.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the AllDocs Page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class AllDocsPage extends ViewPage
{
    @FindBy(xpath = "//li[@id='xwikiindex']/a")
    private WebElement indexTab;

    @FindBy(xpath = "//li[@id='xwikitreeview']/a")
    private WebElement treeTab;

    @FindBy(xpath = "//li[@id='xwikiattachments']/a")
    private WebElement attachmentsTab;

    @FindBy(xpath = "//li[@id='xwikideletedDocs']/a")
    private WebElement deletedDocsTab;

    @FindBy(xpath = "//li[@id='xwikideletedAttachments']/a")
    private WebElement deletedAttachmentsTab;

    @FindBy(className = "xtree")
    private WebElement treeElement;

    public static AllDocsPage gotoPage()
    {
        getUtil().gotoPage("Main", "AllDocs");
        return new AllDocsPage();
    }

    public static String getURL()
    {
        return getUtil().getURL("Main", "AllDocs");
    }

    public LiveTableElement clickIndexTab()
    {
        this.indexTab.click();

        LiveTableElement lt = new LiveTableElement("alldocs");
        lt.waitUntilReady();

        return lt;
    }

    public DocumentTreeElement clickTreeTab()
    {
        this.treeTab.click();
        return new DocumentTreeElement(this.treeElement).waitForIt();
    }

    public LiveTableElement clickAttachmentsTab()
    {
        this.attachmentsTab.click();

        LiveTableElement lt = new LiveTableElement("allattachments");
        lt.waitUntilReady();

        return lt;
    }

    public boolean hasDeletedDocsTab()
    {
        return getDriver().findElementsWithoutWaiting(By.xpath("//li[@id='xwikideletedDocs']/a")).size() > 0;
    }

    public boolean hasDeletedAttachmentsTab()
    {
        return getDriver().findElementsWithoutWaiting(By.xpath("//li[@id='xwikideletedAttachments']/a")).size() > 0;
    }

    public LiveTableElement clickDeletedDocsTab()
    {
        this.deletedDocsTab.click();

        LiveTableElement lt = new LiveTableElement("documentsTrash");
        lt.waitUntilReady();

        return lt;
    }

    public LiveTableElement clickDeletedAttachmentsTab()
    {
        this.deletedAttachmentsTab.click();

        LiveTableElement lt = new LiveTableElement("attachmentTrash");
        lt.waitUntilReady();

        return lt;
    }
}
