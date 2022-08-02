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
package org.xwiki.image.style.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.editor.EditPage;

/**
 * Page object for the image style configuration form.
 *
 * @version $Id$
 * @since 14.3RC1
 */
public class ImageStyleConfigurationForm extends EditPage
{
    /**
     * Fill the pretty name field.
     *
     * @param prettyName the pretty name (e.g., "Borderless")
     * @return the current page object
     */
    public ImageStyleConfigurationForm setPrettyName(String prettyName)
    {
        getDriver().findElement(By.name("Image.Style.Code.ImageStyleClass_0_prettyName")).sendKeys(prettyName);
        return this;
    }

    /**
     * Fill the type field.
     *
     * @param type the type of the field (e.g., "borderless-class")
     * @return the current page object
     */
    public ImageStyleConfigurationForm setType(String type)
    {
        getDriver().findElement(By.name("Image.Style.Code.ImageStyleClass_0_type")).sendKeys(type);
        return this;
    }

    /**
     * Goes back to the administration by following the dedicated link.
     *
     * @return the administration page object
     */
    public ImageStyleAdministrationPage clickBackToTheAdministration()
    {
        getDriver().findElement(By.className("admin-link")).click();
        return new ImageStyleAdministrationPage();
    }
}
