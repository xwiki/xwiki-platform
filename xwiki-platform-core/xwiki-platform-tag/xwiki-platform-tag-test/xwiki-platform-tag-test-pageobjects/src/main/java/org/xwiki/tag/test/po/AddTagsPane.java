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
package org.xwiki.tag.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the panel that is displayed when we want to add new tags to a wiki page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class AddTagsPane extends BaseElement
{
    /**
     * The class name applied to the add tags HTML form.
     */
    private static final String FORM_CLASS_NAME = "tag-add-form";

    /**
     * The input field used to enter tags.
     */
    @FindBy(id = "tag")
    private WebElement tagsInput;

    /**
     * The HTML form used to add new tags.
     */
    @FindBy(className = FORM_CLASS_NAME)
    private WebElement addTagsForm;

    /**
     * The XPATH expression used to locate the add button inside the {@link #addTagsForm}.
     */
    private By addButtonLocator = By.xpath("//form[@class = 'tag-add-form']//input[@type = 'submit']");

    /**
     * @param tags comma separated list of tags to add
     */
    public void setTags(String tags)
    {
        tagsInput.clear();
        tagsInput.sendKeys(tags);
    }

    /**
     * Click on the add button to add the typed tags.
     * 
     * @return {@code true} if any of the typed tags have been added, {@code false} if an error occurs such as an
     *         existing tag is saved.
     */
    public boolean add()
    {
        // Wait for no error notifications to be displayed because after that we wait for one to show up.
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return getDriver().findElements(By.className("xnotification-error")).size() == 0;
            }
        });

        addTagsForm.findElement(addButtonLocator).click();

        // Wait until the add tags panel disappears or
        // an error notification is shown to indicate something is wrong and the tag cannot be saved.
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return getDriver().findElements(By.className(FORM_CLASS_NAME)).size() == 0
                    || getDriver().findElements(By.className("xnotification-error")).size() > 0;
            }
        });

        // If the add tags panel is still visible then there was a problem adding the tags.
        return getDriver().findElementsWithoutWaiting(By.className(FORM_CLASS_NAME)).size() == 0;
    }

    /**
     * Click on the cancel button to close the panel.
     */
    public void cancel()
    {
        addTagsForm.findElement(By.xpath("//form[@class = 'tag-add-form']//input[@type = 'reset']")).click();
    }
}
