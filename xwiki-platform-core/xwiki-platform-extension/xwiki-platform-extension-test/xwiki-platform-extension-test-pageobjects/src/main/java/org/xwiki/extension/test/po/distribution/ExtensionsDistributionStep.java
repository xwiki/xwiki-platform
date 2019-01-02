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
import org.openqa.selenium.support.FindBy;

/**
 * The Report step UI.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class ExtensionsDistributionStep extends AbstractDistributionPage
{
    // TODO: move that in a dedicated PO since that's a EM UI element
    @FindBy(xpath = "//button[@value='checkForUpdates']")
    private WebElement checkForUpdates;

    // TODO: move that in a dedicated PO since that's actually a EM UI element
    public ExtensionsDistributionStep checkForUpdates()
    {
        // Click the button
        this.checkForUpdates.click();

        // Wait for the result
        getDriver()
            .waitUntilElementIsVisible(By.xpath("//div[@class='extensionUpdater']/p[@class='xHint']"));

        return new ExtensionsDistributionStep();
    }
}
