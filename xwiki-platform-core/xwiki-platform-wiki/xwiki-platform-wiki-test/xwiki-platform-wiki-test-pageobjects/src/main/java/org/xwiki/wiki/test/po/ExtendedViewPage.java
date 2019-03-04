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
package org.xwiki.wiki.test.po;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

public class ExtendedViewPage extends ViewPage
{
    @FindBy(xpath = "//div[contains(@class, 'drawer-menu-item-text') and contains(text(), 'Wiki Index')]/..")
    private WebElement wikiIndex;

    public WikiIndexPage goToWikiIndex()
    {
        toggleDrawer();
        // Calling click() doesn't have the expected result: the mouse is moved over the link (we can see that the
        // link is hovered because it gets underlined and the link URL is displayed in the browser status bar) but
        // the link does not seems to be clicked. If we pause the test and click the link ourselves everything
        // is fine. So it seems this is a Selenium / WebDriver bug.
        // The workaround is to use the Enter / Return key.
        // TODO: try click() again when upgrading Selenium (we are quite late right now)
        this.wikiIndex.sendKeys(Keys.RETURN);
        return new WikiIndexPage();
    }

    public CreateWikiPage createWiki()
    {
        return goToWikiIndex().createWiki();
    }

    /**
     * @since 6.0M1
     */
    public DeleteWikiPage deleteWiki()
    {
        String currentWiki = getHTMLMetaDataValue("wiki");
        return goToWikiIndex().deleteWiki(currentWiki);
    }
}
