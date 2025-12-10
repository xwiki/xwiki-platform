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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the content of a document extra tab.
 *
 * @version $Id$
 * @since 17.9.0RC1
 * @since 17.4.6
 * @since 16.10.13
 */
public class DocExtraPane extends BaseElement
{
    private WebElement container;

    /**
     * @param id the tab identifier
     */
    public DocExtraPane(String id)
    {
        By containerId = By.id(id + "pane");
        getDriver().waitUntilElementIsVisible(containerId);
        this.container = getDriver().findElementWithoutWaiting(containerId);
    }

    /**
     * @return the text displayed in this tab
     */
    public String getText()
    {
        return this.container.getText();
    }
}
