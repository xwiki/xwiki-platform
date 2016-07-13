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
package org.xwiki.blog.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The create blog post form.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class CreateBlogPostPane extends BaseElement
{
    /**
     * The title input field.
     */
    @FindBy(id = "entryTitle")
    private WebElement titleInput;

    /**
     * The create button.
     */
    @FindBy(xpath = "//form[@id = 'newBlogPost']//input[@type = 'submit' and @value = 'Create']")
    private WebElement createButton;

    /**
     * Fills the blog post title field with the passed value.
     * 
     * @param title the title to use for the new blog post
     */
    public void setTitle(String title)
    {
        titleInput.clear();
        titleInput.sendKeys(title);
    }

    /**
     * Clicks on the create buttons.
     * 
     * @return the "Inline form" edit mode page opened by the create button
     */
    public BlogPostInlinePage clickCreateButton()
    {
        createButton.click();
        return new BlogPostInlinePage();
    }
}
