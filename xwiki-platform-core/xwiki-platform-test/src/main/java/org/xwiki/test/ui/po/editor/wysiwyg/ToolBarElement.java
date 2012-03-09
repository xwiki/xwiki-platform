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
package org.xwiki.test.ui.po.editor.wysiwyg;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the tool bar of the WYSIWYG content editor.
 * 
 * @version $Id$
 * @since 3.4RC1
 */
public class ToolBarElement extends BaseElement
{
    /**
     * The element that wraps the tool bar.
     */
    private final WebElement container;

    /**
     * Creates a new instance that can be used to control the tool bar inside the given container.
     * 
     * @param container the element that wraps the tool bar
     */
    public ToolBarElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * Clicks on the Undo tool bar button.
     */
    public void clickUndoButton()
    {
        clickToolBarButtonWithTitle("Undo (Ctrl+Z)");
    }

    /**
     * Clicks the tool bar button with the specified title.
     * 
     * @param title the title of the tool bar button to be clicked
     */
    protected void clickToolBarButtonWithTitle(String title)
    {
        // We have to wait for it to become enabled because the tool bar is updated with delay after each edit action.
        String xpath = String.format("div[@title='%s' and not(contains(@class, '-disabled'))]", title);
        this.container.findElement(By.xpath(xpath)).click();
    }
}
