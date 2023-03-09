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
package org.xwiki.ckeditor.test.po.image.select;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object for the icon selection form of the image dialog.
 *
 * @version $Id$
 * @since 15.2RC1
 * @since 14.10.7
 */
public class ImageDialogIconSelectForm extends BaseElement
{
    /**
     * Set the value of the icon field.
     *
     * @param iconName the icon name to set
     */
    public void setIconValue(String iconName)
    {
        WebElement iconField = getDriver()
            .findElement(By.cssSelector(".image-selector-modal .image-selector .iconTab input[name='iconField']"));
        iconField.clear();
        iconField.sendKeys(iconName);
    }
}
