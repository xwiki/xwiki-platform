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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * The page object used to interact with the CKEditor table properties dialog.
 *
 * @since 16.8.0RC1
 * @since 16.4.4
 * @since 15.10.13
 */
public class TablePropertiesDialog extends CKEditorDialog
{
    /**
     * Update the table header setting. Values can be `None`, `First Row` (default), `First column` or `Both`.
     */
    public TablePropertiesDialog setHeader(String value)
    {
        // The CKEditor table dialog does not have much semantics in its form, we use the label text to find the 
        // select we're looking for.
        WebElement headerLabel = getContainer().findElement(
            By.xpath("//label[contains(text(), 'Headers')]"));
        WebElement headerSelect = getDriver().findElement(By.id(headerLabel.getAttribute("for")));
        Select select = new Select(headerSelect);
        select.selectByVisibleText(value);
        return this;
    }
}
