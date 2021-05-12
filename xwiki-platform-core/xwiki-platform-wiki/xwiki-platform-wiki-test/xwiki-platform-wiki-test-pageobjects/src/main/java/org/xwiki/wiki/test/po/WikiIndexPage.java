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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;

/**
 * Represents actions that can be done on the WikiManager.WebHome page.
 *
 * @version $Id$
 */
public class WikiIndexPage extends ExtendedViewPage
{
    private static final String WIKI_NAME_COLUMN_LABEL = "Wiki pretty name";

    @FindBy(id = "wikis")
    private WebElement wikisTable;

    @FindBy(id = "tmCreateWiki")
    private WebElement createWikiMenuButton;

    /**
     * The wiki index live table.
     */
    private final LiveDataElement liveData = new LiveDataElement("wikis");

    /**
     * Opens the home page.
     */
    public static WikiIndexPage gotoPage()
    {
        getUtil().gotoPage("WikiManager", "WebHome");
        return new WikiIndexPage();
    }

    /**
     * Get a wiki link from its name.
     *
     * @param wikiName the name of the wiki
     * @return {@code null} if the wiki is not found, a {@link WikiLink} of the link of the wiki otherwise
     * @since 6.0M1
     */
    public WikiLink getWikiLink(String wikiName)
    {
        TableLayoutElement tableLayout = this.liveData.getTableLayout();
        tableLayout.filterColumn(WIKI_NAME_COLUMN_LABEL, wikiName);
        if (tableLayout.countRows() == 0) {
            return null;
        } else {
            return new WikiLink(tableLayout.getCell(WIKI_NAME_COLUMN_LABEL, 1).findElement(By.tagName("a")));
        }
    }

    @Override
    public WikiIndexPage waitUntilPageIsLoaded()
    {
        this.liveData.getTableLayout().waitUntilReady();
        return this;
    }

    /**
     * @since 8.4.3
     */
    public CreateWikiPage createWiki()
    {
        this.createWikiMenuButton.click();
        return new CreateWikiPage();
    }

    /**
     * Click on the delete action of a wiki.
     *
     * @param wikiName the name of the wiki to remove
     * @return the delete wiki page of the requested wiki
     * @since 8.4.3
     */
    public DeleteWikiPage deleteWiki(String wikiName)
    {
        TableLayoutElement tableLayout = this.liveData.getTableLayout();
        tableLayout.filterColumn(WIKI_NAME_COLUMN_LABEL, wikiName);
        tableLayout.findElementInRow(1, By.cssSelector("a.action_delete")).click();
        return new DeleteWikiPage();
    }
}
