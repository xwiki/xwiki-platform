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
package org.xwiki.panels.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BasePage;

/**
 * Represents a page that has panels.
 * 
 * @version $Id$
 * @since 4.3.2
 */
public class PageWithPanels extends BasePage
{
    /**
     * @param panelTitle the panel title
     * @return {@code true} if this page has the specified panel, {@code false} otherwise
     */
    public boolean hasPanel(String panelTitle)
    {
        String xpath = String.format("//h1[@class = 'xwikipaneltitle' and . = '%s']", panelTitle);
        return getDriver().findElementsWithoutWaiting(By.xpath(xpath)).size() > 0;
    }
}
