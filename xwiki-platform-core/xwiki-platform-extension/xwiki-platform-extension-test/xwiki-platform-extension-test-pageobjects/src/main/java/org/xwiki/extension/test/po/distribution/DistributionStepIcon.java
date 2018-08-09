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
package org.xwiki.extension.test.po.distribution;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * A step in the DW header.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class DistributionStepIcon extends BaseElement
{
    private final WebElement container;

    public DistributionStepIcon(WebElement container)
    {
        this.container = container;
    }

    private WebElement getNumberElement()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.className("number"));
    }

    public boolean isDone()
    {
        return getNumberElement().getAttribute("class").contains(" done");
    }

    public boolean isActive()
    {
        return getNumberElement().getAttribute("class").contains(" active");
    }

    public int getNumber()
    {
        return Integer.valueOf(getNumberElement().getText());
    }

    public String getName()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.className("name")).getText();
    }
}
