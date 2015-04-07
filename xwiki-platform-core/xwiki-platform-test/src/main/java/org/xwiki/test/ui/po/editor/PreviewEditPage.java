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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Models the preview edit page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class PreviewEditPage extends EditPage
{
    /**
     * The edited page.
     */
    private PreviewableEditPage editedPage;

    @FindBy(id = "xwikicontent")
    private WebElement content;

    /**
     * The button used to return to edit. We don't use the top button because its position in preview mode coincides
     * with the position of the edit menu in edit mode and since the mouse remains in the same position the edit menu is
     * opened as soon as we get back to edit (because it opens on hover).
     */
    @FindBy(xpath = "//*[@class = 'bottombuttons']//*[@name = 'action_edit']")
    private WebElement backToEdit;

    /**
     * Creates a new preview page that holds a reference to the edited page.
     * 
     * @param editedPage the edited page
     */
    public PreviewEditPage(PreviewableEditPage editedPage)
    {
        this.editedPage = editedPage;
    }

    /**
     * Clicks on the back to edit button.
     * 
     * @return the edited page
     */
    public PreviewableEditPage clickBackToEdit()
    {
        backToEdit.click();
        return editedPage;
    }

    /**
     * @return the page's main content as text (no HTML)
     */
    public String getContent()
    {
        return this.content.getText();
    }
}
