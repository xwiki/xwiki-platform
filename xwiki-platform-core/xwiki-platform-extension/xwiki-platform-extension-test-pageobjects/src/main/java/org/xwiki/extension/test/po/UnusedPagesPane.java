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
 * The confirmation to delete the unused wiki pages after uninstalling or upgrading a XAR extension.
 * 
 * @version $Id$
 * @since 5.4.3
 */
public class UnusedPagesPane extends BaseElement
{
    /**
     * The tree of unused wiki pages.
     */
    private WebElement documentTree;

    /**
     * Wraps the document tree found inside the given job question container.
     * 
     * @param question the job question that contains this confirmation
     */
    public UnusedPagesPane(WebElement question)
    {
        this.documentTree = getDriver().findElementWithoutWaiting(question, By.className("document-tree"));
    }

    /**
     * @param space the space name
     * @param page the page name
     * @return {@code true} if the tree of unused pages contains the specified document
     */
    public boolean contains(String space, String page)
    {
        String reference = String.format("xwiki:%s.%s", space, page);
        String xpath = "//input[@type = 'checkbox' and @name = '" + reference + "' and @value = '']";
        return getDriver().findElementsWithoutWaiting(this.documentTree, By.xpath(xpath)).size() > 0;
    }
}
