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
package org.xwiki.test.ui.po.editor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Edit page with a preview button.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class PreviewableEditPage extends EditPage
{
    @FindBy(name = "action_preview")
    private WebElement preview;

    /**
     * @return the preview button.
     */
    public WebElement getPreviewButton()
    {
        return this.preview;
    }

    /**
     * @return {@code true} if the preview button is present and displayed, {@code false} otherwise.
     */
    public boolean hasPreviewButton()
    {
        return getDriver().findElementsWithoutWaiting(By.name("action_preview")).stream()
            .anyMatch(WebElement::isDisplayed);
    }

    /**
     * Clicks on the preview button.
     * 
     * @return the preview edit page
     */
    public PreviewEditPage clickPreview()
    {
        preview.click();
        return new PreviewEditPage(this);
    }
}
