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
package org.xwiki.ckeditor.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;


/**
 * Models a Quick Action drop-down.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Unstable
public class QuickActionDropdown extends BaseElement
{
    
    /**
     * The associated RichTextArea instance.
     */
    private final RichTextAreaElement textArea;
    
    /**
     * The associated selected element id.
     */
    private String selectedId;
    
    /**
     * Creates a new Quick Action drop-down instance.
     * 
     * @param editor - CKEditor instance
     */
    public QuickActionDropdown(CKEditor editor)
    {
        this.textArea = editor.getRichTextArea();
    }
    
    /**
     * Finds the dropdown.
     * @return the dropdown element
     */
    public WebElement getDropdown()
    {
        try {
            return getDriver().findElement(By.cssSelector(
                ".cke_autocomplete_opened .cke_autocomplete_selected"));
        } catch (NoSuchElementException e) {
            return null;
        }
    }
    
    
    /**
     * Open the Quick Action drop-down.
     * 
     */
    public void open()
    {
        this.textArea.sendKeys("/");
        getDriver().waitUntilElementIsVisible(By.cssSelector(".cke_autocomplete_opened"));
        this.waitUpdate();
    }
    
    /**
     * Wait for the dropdown to update.
     */
    public void waitUpdate() 
    {
        if (this.getDropdown() != null) {
            if (this.selectedId != null) {
                getDriver().waitUntilElementDisappears(By.id(this.selectedId));
            }
            this.selectedId = this.getDropdown().getAttribute("id");
        } else {
            this.selectedId = null;
        }
        
    }
    
    /**
     * Add text to the current query, wait for the Quick Actions menu to update.
     * @param texts - text to add to the query
     */
    public void sendKeys(CharSequence... texts)
    {
        for (CharSequence text : texts)
        {
            for (int i = 0; i < text.length(); i++) {
                this.textArea.sendKeys(text.subSequence(i, i + 1));
                this.waitUpdate();
            }
        }
    }
    
    /**
     * Submits the selected Quick Action.
     */
    public void submit()
    {
        if (this.getDropdown() != null) {
            this.textArea.sendKeys(Keys.ENTER);
            this.waitUpdate();
        }
    }
    
    /**
     * Finds the element enclosing given text.
     * @param tag - search query
     * @param content - the text to be found in the element
     * @return the corresponding element
     */
    public boolean findElementsWithContent(String tag, String content)
    {
        try {
            this.textArea.getActiveElement();            
            for (WebElement element: this.getDriver().findElements(By.tagName(tag))) {
                if (element.getText().contains(content)) {
                    return true;
                }
            }
            return false;
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }
    
}
