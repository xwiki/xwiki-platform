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
package org.xwiki.administration.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents a template provider page in view mode.
 *
 * @since 18.3.0RC1
 */
public class TemplateProviderViewPage extends ViewPage
{
    /**
     * @return the icon name displayed on the template provider view page, extracted from the {@code <img>} element's
     *         {@code src} attribute (e.g., {@code "page"} for {@code icons/silk/page.png}), or an empty string if no
     *         icon is displayed
     */
    public String getIcon()
    {
        List<WebElement> iconImages = getDriver().findElementsWithoutWaiting(
            By.xpath("//dt[contains(., 'Icon')]/following-sibling::dd[1]//img"));
        if (iconImages.isEmpty()) {
            return "";
        }
        String src = iconImages.getFirst().getAttribute("src");
        String fileName = src.substring(src.lastIndexOf('/') + 1);
        return fileName.contains(".") ? fileName.substring(0, fileName.indexOf('.')) : fileName;
    }

    /**
     * @return the description text displayed on the template provider view page, or an empty string if none
     */
    public String getDescription()
    {
        List<WebElement> descriptionElements = getDriver().findElementsWithoutWaiting(
            By.xpath("//dt[contains(., 'Description')]/following-sibling::dd[1]"));
        if (descriptionElements.isEmpty()) {
            return "";
        }
        return descriptionElements.getFirst().getText().trim();
    }
}
