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
package org.xwiki.realtime.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a document version listed in the history dropdown from the realtime editing toolbar.
 * 
 * @version $Id$
 * @since 17.9.0
 * @since 17.4.7
 * @since 16.10.13
 */
public class VersionElement extends BaseElement
{
    private WebElement container;

    /**
     * Creates a new instance based on the given version element.
     * 
     * @param container the WebElement used to display the version
     */
    public VersionElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the author of this version
     */
    public CoeditorElement getAuthor()
    {
        return new CoeditorElement(this.container.findElement(By.className("realtime-user")));
    }

    /**
     * @return the version number
     */
    public String getNumber()
    {
        return this.container.findElement(By.className("realtime-version-number")).getText();
    }

    /**
     * @return the version date
     */
    public String getDate()
    {
        return this.container.findElement(By.className("realtime-version-date")).getText();
    }
}
