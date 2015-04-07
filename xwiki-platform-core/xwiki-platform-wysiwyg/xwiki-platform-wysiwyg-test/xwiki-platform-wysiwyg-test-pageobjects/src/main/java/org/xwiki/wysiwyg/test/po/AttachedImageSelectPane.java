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
package org.xwiki.wysiwyg.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Models the image selection wizard step that is accessible when inserting or editing an attached image with the
 * WYSIWYG content editor.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class AttachedImageSelectPane extends WizardStepElement
{
    @FindBy(xpath = "//div[@class = 'xSelectorAggregatorStep']//div[. = 'Current page']")
    private WebElement currentPageTab;

    @Override
    public AttachedImageSelectPane waitToLoad()
    {
        super.waitToLoad();
        getDriver().waitUntilElementIsVisible(By.className("xSelectorAggregatorStep"));
        return this;
    }

    /**
     * Selects the tab that lists the images attached to the current page.
     * 
     * @return the pane used to select an image from those attached to the edited page
     */
    public CurrentPageImageSelectPane selectFromCurrentPage()
    {
        currentPageTab.click();
        return new CurrentPageImageSelectPane().waitToLoad();
    }
}
