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
package org.xwiki.extension.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The section that displays various information about an extension, like its license and web page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionDescriptionPane extends BaseElement
{
    /**
     * The XPath used to access a definition list item if we know the definition term.
     */
    private static final String DEFINITION_FOR_TERM_XPATH = ".//dd[preceding-sibling::dt[1][. = '%s']]";

    /**
     * The section container.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the section container
     */
    public ExtensionDescriptionPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the extension license
     */
    public String getLicense()
    {
        return getMetaData("License");
    }

    /**
     * @return the extension web site link
     */
    public WebElement getWebSite()
    {
        By xpath = By.xpath(String.format(DEFINITION_FOR_TERM_XPATH + "//a", "Website"));
        return getDriver().findElementWithoutWaiting(container, xpath);
    }

    /**
     * @return the extension id
     */
    public String getId()
    {
        return getMetaData("Id");
    }

    /**
     * @return the extension type
     */
    public String getType()
    {
        return getMetaData("Type");
    }

    /**
     * @param label the meta data label
     * @return the extension meta data for the specified label
     */
    private String getMetaData(String label)
    {
        By xpath = By.xpath(String.format(DEFINITION_FOR_TERM_XPATH, label));
        return getDriver().findElementWithoutWaiting(container, xpath).getText();
    }
}
