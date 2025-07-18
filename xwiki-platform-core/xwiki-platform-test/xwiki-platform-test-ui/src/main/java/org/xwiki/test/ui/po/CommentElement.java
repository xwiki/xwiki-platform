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

import org.codehaus.plexus.util.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Describe a unique comment in the UI.
 *
 * @version $Id$
 * @since 17.3.0RC1
 * @since 16.10.6
 * @since 16.4.8
 */
public class CommentElement extends BaseElement
{
    private final WebElement container;

    /**
     * Default constructor.
     *
     * @param container the container element of this specific comment.
     */
    public CommentElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the displayed author name.
     */
    public String getAuthor()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector("span.commentauthor")).getText();
    }

    /**
     * @return the displayed date.
     */
    public String getDate()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector("span.commentdate")).getText();
    }

    /**
     * @return te displayed content.
     */
    public String getContent()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector("div.commentcontent")).getText();
    }

    /**
     * @return {@code true} iff the comment is a reply to another comment.
     */
    public boolean isReply()
    {
        return !StringUtils.isEmpty(this.container.getDomAttribute("data-replyto"));
    }
}
