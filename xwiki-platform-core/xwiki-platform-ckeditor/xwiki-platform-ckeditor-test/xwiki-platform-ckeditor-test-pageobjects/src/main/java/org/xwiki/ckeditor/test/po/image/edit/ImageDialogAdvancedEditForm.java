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
package org.xwiki.ckeditor.test.po.image.edit;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Gives access to the operations to interact with the advanced tab of the image edit step.
 *
 * @version $Id$
 * @since 15.2
 * @since 14.10.8
 */
public class ImageDialogAdvancedEditForm extends BaseElement
{
    /**
     * Select the centered alignment.
     *
     * @return the current page object
     */
    public ImageDialogAdvancedEditForm selectCenterAlignment()
    {
        getDriver().findElement(By.cssSelector("#advanced [name='alignment'][value='center']")).click();
        return this;
    }

    /**
     * @return the currently selected alignment value
     * @since 14.10.13
     * @since 15.5RC1
     */
    public String getAlignment()
    {
        return getDriver().findElement(By.cssSelector("#advanced [name='alignment']:checked")).getAttribute("value");
    }
}
