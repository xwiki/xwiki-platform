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

/**
 * Models the CKEditor dialog used to insert special characters.
 *
 * @version $Id$
 * @since 17.10.14
 * @since 18.4.7
 * @since 18.9.0RC1
 */
@Unstable
public class SpecialCharacterDialog extends CKEditorDialog
{
    /**
     * Click the given character in order to insert it. The dialog is closed afterwards.
     *
     * @param character the special character to insert (e.g. "©")
     */
    public void insert(String character)
    {
        // The character is the text of the first span, the second one holds its (localized) description.
        getContainer().findElement(By.xpath(
            String.format(".//a[contains(@class, 'cke_specialchar')][span[1] = '%s']", character))).click();
        getDriver().waitUntilElementDisappears(By.className("cke_dialog_contents"));
    }
}
