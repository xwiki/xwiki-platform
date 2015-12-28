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
package org.xwiki.extension.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Displays a log item.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class LogItemPane extends BaseElement
{
    /**
     * The log item container.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the log item container
     */
    public LogItemPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the log item level (info, error, loading)
     */
    public String getLevel()
    {
        String[] classNames = container.getAttribute("class").split("\\s+");
        if (classNames.length < 2) {
            return null;
        }
        return classNames[1].substring("log-item-".length());
    }

    /**
     * @return the log item message
     */
    public String getMessage()
    {
        return getDriver().findElementWithoutWaiting(container, By.tagName("DIV")).getText();
    }
}
