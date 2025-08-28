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
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Models a view page that can be tagged.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class TaggablePage extends ViewPage
{
    /**
     * The element that contains all the page tags.
     */
    @FindBy(id = "xdocTags")
    private WebElement tagsContainer;

    public static TaggablePage gotoPage(EntityReference entityReference)
    {
        getUtil().gotoPage(entityReference);
        return new TaggablePage();
    }

    /**
     * @return {@code true} if this page has the specified tag, {@code false} otherwise
     */
    public boolean hasTag(String tagName)
    {
        return getDriver().findElementsWithoutWaiting(this.tagsContainer, getTagLocator(tagName)).size() > 0;
    }

    /**
     * @param tagName a tag name
     * @return the XPATH expression locating the specified tag
     */
    private By getTagLocator(String tagName)
    {
        return By.xpath("//*[@class = 'tag' and . = '" + tagName + "']");
    }

    /**
     * Removes the specified tag from this page.
     * 
     * @param tagName the name of the tag to remove
     * @return {@code true} if the tag was found, {@code false} otherwise
     */
    public boolean removeTag(String tagName)
    {
        List<WebElement> toDelete = getDriver().findElementsWithoutWaiting(this.tagsContainer,
            By.xpath("//a[contains(@href, '&tag=" + tagName + "') and . = 'X']"));
        if (toDelete.size() > 0) {
            toDelete.get(0).click();
            getDriver().waitUntilElementDisappears(getTagLocator(tagName));
            return true;
        }
        return false;
    }

    /**
     * Opens the panel to add new tags to this page.
     * 
     * @return the panel that can be used to add new tags
     */
    public AddTagsPane addTags()
    {
        this.tagsContainer.findElement(By.linkText("[+]")).click();
        getDriver().waitUntilElementDisappears(By.xpath("//*[@id = 'xdocTags']//a[. = '[+]']"));
        return new AddTagsPane();
    }
    
    public TagPage clickOnTag(String tagName)
    {
        WebElement tag = getDriver().findElementWithoutWaiting(
            By.xpath("//span[@class='tag']/a[. = '" + tagName + "' ]"));
        tag.click();
        return new TagPage(tagName);
    }
}
