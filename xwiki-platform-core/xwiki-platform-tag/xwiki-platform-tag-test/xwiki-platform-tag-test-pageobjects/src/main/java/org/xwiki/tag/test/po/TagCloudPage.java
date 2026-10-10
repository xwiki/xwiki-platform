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
package org.xwiki.tag.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Models the {@code Main.Tags} page, which displays the tag cloud of the wiki when no action is requested.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class TagCloudPage extends ViewPage
{
    private static final By TAG_LINKS = By.cssSelector("ol.tagCloud li a");

    /**
     * Opens the {@code Main.Tags} page.
     *
     * @return the tag cloud page
     */
    public static TagCloudPage gotoPage()
    {
        getUtil().gotoPage("Main", "Tags");
        return new TagCloudPage();
    }

    /**
     * @return the tags listed in the tag cloud
     */
    public List<String> getTags()
    {
        return getDriver().findElementsWithoutWaiting(TAG_LINKS).stream().map(WebElement::getText).toList();
    }

    /**
     * Clicks on a tag of the tag cloud.
     *
     * @param tagName the tag to click on
     * @return the page listing the pages tagged with the clicked tag
     */
    public TagPage clickTag(String tagName)
    {
        getDriver().findElementsWithoutWaiting(TAG_LINKS).stream()
            .filter(link -> tagName.equals(link.getText()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("No tag [%s] in the tag cloud", tagName)))
            .click();
        getDriver().waitUntilCondition(driver -> driver.getCurrentUrl().contains("do=viewTag"));
        return new TagPage(tagName);
    }
}
