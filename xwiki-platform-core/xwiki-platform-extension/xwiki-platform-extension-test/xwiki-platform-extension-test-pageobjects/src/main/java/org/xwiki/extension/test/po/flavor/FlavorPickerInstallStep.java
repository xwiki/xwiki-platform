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
package org.xwiki.extension.test.po.flavor;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.extension.test.po.ExtensionPane;
import org.xwiki.extension.test.po.distribution.AbstractDistributionPage;

/**
 * The Flavor step UI.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class FlavorPickerInstallStep extends AbstractDistributionPage
{
    @FindBy(xpath = "//*[@id = 'extension.flavor']//form[contains(@class, 'extension-item')]")
    private WebElement flavorExtension;

    public ExtensionPane getFlavorExtensionPane()
    {
        return new ExtensionPane(this.flavorExtension);
    }
}
