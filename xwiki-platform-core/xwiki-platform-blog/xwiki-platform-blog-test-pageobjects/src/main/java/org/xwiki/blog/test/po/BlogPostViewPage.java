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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The page that displays a blog post.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class BlogPostViewPage extends ViewPage
{
    /**
     * The blog post content.
     */
    @FindBy(className = "entry-content")
    private WebElement content;

    /**
     * The blog post footer.
     */
    @FindBy(className = "entry-footer")
    private WebElement footer;

    /**
     * The container for the blog post action buttons.
     */
    @FindBy(className = "blog-entry-toolbox")
    private WebElement toolBox;

    /**
     * The edit blog post icon.
     */
    @FindBy(xpath = "//*[@class = 'blog-entry-toolbox']//img[@alt = 'edit ']")
    private WebElement editIcon;

    @Override
    public String getContent()
    {
        return content.getText();
    }

    /**
     * @return the list of categories the displayed blog post is part of
     */
    public List<String> getCategories()
    {
        List<String> categories = new ArrayList<String>();
        for (WebElement categoryLink : footer.findElements(By.xpath("//a[@rel = 'tag']"))) {
            categories.add(categoryLink.getText());
        }
        return categories;
    }

    /**
     * @return {@code true} if the displayed blog post is published, {@code false} otherwise
     */
    public boolean isPublished()
    {
        return toolBox.findElements(By.xpath("//img[@alt = 'publish ']")).isEmpty();
    }

    /**
     * @return {@code true} if the displayed blog post is hidden from the rest of the users, {@code false} otherwise
     */
    public boolean isHidden()
    {
        return !toolBox.findElements(By.xpath("//img[@alt = 'show ']")).isEmpty();
    }

    /**
     * Clicks on the edit blog post icon.
     * 
     * @return the "Inline form" edit page used for editing the blog post
     */
    public BlogPostInlinePage clickEditBlogPostIcon()
    {
        editIcon.click();
        return new BlogPostInlinePage();
    }
}
