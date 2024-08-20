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
package org.xwiki.test.ui.po.diff;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the rendered changes tab in the changes pane.
 *
 * @version $Id$
 * @since 14.10.15
 * @since 15.5.1
 * @since 15.6
 */
public class RenderedChanges extends BaseElement
{
    private final WebElement container;

    /**
     * @param container the container element of the rendered changes tab
     */
    public RenderedChanges(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return {@code true} if the "No changes" message is displayed and there are no diffs displayed, {@code false}
     */
    public boolean hasNoChanges()
    {
        return !getDriver().findElementsWithoutWaiting(this.container,
            By.xpath("//div[@class = 'infomessage' and contains(text(), 'No changes')]")).isEmpty()
            && getChangedBlocks().isEmpty();
    }

    /**
     * @return the list of changed blocks
     */
    public List<WebElement> getChangedBlocks()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.cssSelector("[data-xwiki-html-diff-block]"));
    }
}
