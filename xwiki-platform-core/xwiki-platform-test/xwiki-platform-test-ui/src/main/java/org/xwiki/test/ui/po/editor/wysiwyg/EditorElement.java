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
package org.xwiki.test.ui.po.editor.wysiwyg;

import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models a WYSIWYG editor instance.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class EditorElement extends BaseElement
{
    /**
     * The id of the form field field replaced the WYSIWYG editor.
     */
    private final String fieldId;

    /**
     * Creates a new instance that can be used to control the WYSIWYG editor that replaced the specified form field.
     * 
     * @param fieldId the id of the text area field that was replaced by the WYSIWYG editor.
     */
    public EditorElement(String fieldId)
    {
        this.fieldId = fieldId;
    }

    /**
     * @return the rich text area
     */
    public RichTextAreaElement getRichTextArea()
    {
        // The in-line frame element is renewed while editing so we can't cache it.
        return new RichTextAreaElement(getContainer().findElement(By.className("gwt-RichTextArea")));
    }

    /**
     * Waits for the WYSIWYG content editor to load.
     */
    public EditorElement waitToLoad()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                try {
                    // Source tab in WYSIWYG editor 2.x syntax
                    getContainer(driver).findElement(
                        By.xpath("//div[@class = 'gwt-TabBarItem gwt-TabBarItem-selected']/div[. = 'Source']"));
                    WebElement sourceTextArea = getContainer(driver).findElement(By.className("xPlainTextEditor"));
                    return sourceTextArea.isEnabled() ? sourceTextArea : null;
                } catch (NotFoundException sourceNotFound) {
                    try {
                        // WYSIWYG editor 2.x syntax
                        WebElement richTextEditor = getContainer(driver).findElement(By.className("xRichTextEditor"));
                        try {
                            richTextEditor.findElement(By.className("loading"));
                            return null;
                        } catch (NotFoundException loadingNotFound) {
                            return richTextEditor;
                        }
                    } catch (NotFoundException xRichTextEditorNotFound) {
                        try {
                            // TinyMCE editor 1.x syntax
                            WebElement mceEditor = driver.findElement(By.className("mceEditor"));
                            return mceEditor;
                        } catch (NotFoundException mceNotFound) {
                            return null;
                        }
                    }
                }
            }
        });
        return this;
    }

    /**
     * Note: Uses the passed driver, this is important since this is called by code within a Wait class, using the
     * wrapped driver.
     * 
     * @return the element that wraps the editor
     */
    private WebElement getContainer(WebDriver driver)
    {
        return driver.findElement(By.xpath("//div[starts-with(@id, '" + fieldId + "_container')]"));
    }

    /**
     * @return the element that wraps the editor
     */
    protected WebElement getContainer()
    {
        return getContainer(getDriver());
    }
}
