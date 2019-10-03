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
package org.xwiki.flamingo.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ViewPage;

public abstract class CSSGetterPage extends ViewPage
{
    protected abstract String getElementCSSValue(final By locator, String attribute);

    public String getTextColor()
    {
        return getElementCSSValue(By.xpath("//div[@class='main']"), "color");
    }

    public String getPageBackgroundColor()
    {
        return getElementCSSValue(By.id("mainContentArea"), "background-color");
    }

    public String getFontFamily()
    {
        return getElementCSSValue(By.xpath("//div[@class='main']"), "font-family");
    }
}
