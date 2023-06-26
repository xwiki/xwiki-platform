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
package org.xwiki.ckeditor.test.po;

import org.openqa.selenium.By;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;


/**
 * Models an auto-complete drop-down.
 *
 * @version $Id$
 * @since 15.5
 */
@Unstable
public class AutocompleteDropdown extends BaseElement
{

    /**
     * Waits for the given item to be selected.
     * @param label - The name of the item
     */
    public void waitForItemSelected(String label)
    {
        getDriver().findElement(By.xpath("//*[contains(@class, 'cke_autocomplete_selected')]//*[. = '" + label + "']"));
    }
    
    /**
     * Waits for the auto-complete drop-down to disappear.
     */
    public void waitForItemSubmitted()
    {
        getDriver().waitUntilElementDisappears(By.cssSelector(
                ".cke_autocomplete_opened .cke_autocomplete_selected"));
    }
}
