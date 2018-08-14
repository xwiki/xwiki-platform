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

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Displays a flavor picker.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class FlavorPicker extends BaseElement
{
    private static final String INSTALL_FLAVOR_BUTTON_XPATH = ".//input[@name = 'installFlavor']";

    /**
     * The element that wraps the extension display.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the element that wraps the extension display
     */
    public FlavorPicker(WebElement container)
    {
        this.container = container;
    }

    private WebElement getInstallFlavorEnabled()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.xpath(INSTALL_FLAVOR_BUTTON_XPATH));
    }

    public boolean isInstallFlavorEnabled()
    {
        return getInstallFlavorEnabled().isEnabled();
    }

    public FlavorPickerInstallStep installSelectedFlavor()
    {
        getInstallFlavorEnabled().click();

        return new FlavorPickerInstallStep();
    }

    public List<FlavorPane> getFlavors()
    {
        List<WebElement> elements = getDriver().findElementsWithoutWaiting(this.container, By.tagName("li"));

        return elements.stream().map(element -> new FlavorPane(element)).collect(Collectors.toList());
    }
}
