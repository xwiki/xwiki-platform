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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the preview iframe of when customizing a theme.
 * Be careful when using it: the context is switched to be inside the iframe when the box is created. You need to call
 * explicitely {@link #switchToDefaultContent()} to come back on the main frame.
 *
 * @version $Id$
 */
public class PreviewBox extends CSSGetterPage
{
    /**
     * Default constructor: it switches automatically inside the iframe (see {@link #switchToPreviewBox()}).
     */
    public PreviewBox()
    {
        super();
        switchToPreviewBox();
    }

    /**
     * Switch the selenium context inside the iframe.
     * Call {@link #switchToDefaultContent()} to switch out.
     */
    public void switchToPreviewBox()
    {
        getDriver().switchTo().frame("iframe");
    }

    /**
     * Switch to the main frame.
     */
    public void switchToDefaultContent()
    {
        getDriver().switchTo().defaultContent();
    }

    @Override
    protected String getElementCSSValue(final By locator, String attribute)
    {
        return getDriver().findElement(locator).getCssValue(attribute);
    }

    public boolean hasError()
    {
        return hasError(false);
    }

    /**
     * @param switchBack set to true to automatically call {@link #switchToDefaultContent()} after the return.
     * @return {@code true} if the box contain less errors.
     */
    public boolean hasError(boolean switchBack)
    {
        try {
            List<WebElement> errors = getDriver().findElementsWithoutWaiting(By.className("less-error-message"));
            return !errors.isEmpty();
        } finally {
            if (switchBack) {
                switchToDefaultContent();
            }
        }
    }
}
