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
     * The XPath used to select a editor tab (Source/WYSIWYG) by its label.
     */
    private static final String TAB_ITEM_XPATH = "//div[@role = 'tab' and . = '%s']";

    /**
     * The id of the form field field replaced the WYSIWYG editor.
     */
    private final String fieldId;

    /**
     * The menu bar.
     */
    private MenuBarElement menuBar;

    /**
     * The tool bar.
     */
    private ToolBarElement toolBar;

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
     * @return the menu bar
     */
    public MenuBarElement getMenuBar()
    {
        if (menuBar == null) {
            menuBar = new MenuBarElement(getContainer().findElement(By.className("gwt-MenuBar-horizontal")));
        }
        return menuBar;
    }

    /**
     * @return the tool bar
     */
    public ToolBarElement getToolBar()
    {
        if (toolBar == null) {
            toolBar = new ToolBarElement(getContainer().findElement(By.className("xToolbar")));
        }
        return toolBar;
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
     * @return the source text area
     */
    public WebElement getSourceTextArea()
    {
        return getContainer().findElement(By.className("xPlainTextEditor"));
    }

    /**
     * Waits for the WYSIWYG content editor to load.
     */
    public EditorElement waitToLoad()
    {
        getUtil().waitUntilCondition(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                try {
                    getContainer(driver).findElement(
                        By.xpath("//div[@class = 'gwt-TabBarItem gwt-TabBarItem-selected']/div[. = 'Source']"));
                    WebElement sourceTextArea = getContainer(driver).findElement(By.className("xPlainTextEditor"));
                    return sourceTextArea.isEnabled() ? sourceTextArea : null;
                } catch (NotFoundException sourceNotFound) {
                    WebElement richTextEditor = getContainer(driver).findElement(By.className("xRichTextEditor"));
                    try {
                        richTextEditor.findElement(By.className("loading"));
                        return null;
                    } catch (NotFoundException loadingNotFound) {
                        return richTextEditor;
                    }
                }
            }
        });
        return this;
    }

    /**
     * Switches to the Source editor by clicking on the "Source" tab item and waits for the source text area to be
     * initialized.
     */
    public void switchToSource()
    {
        switchToSource(true);
    }

    /**
     * Switches to the Source editor by clicking on the "Source" tab item.
     * 
     * @param wait {@code true} to wait for the source text area to be initialized, {@code false} otherwise
     */
    public void switchToSource(boolean wait)
    {
        getContainer().findElement(By.xpath(String.format(TAB_ITEM_XPATH, "Source"))).click();
        if (wait) {
            waitForSourceTextArea(true);
        }
    }

    /**
     * Switches to the WYSIWYG editor by clicking on the "WYSIWYG" tab item and waits for the rich text area to be
     * initialized.
     */
    public void switchToWysiwyg()
    {
        switchToWysiwyg(true);
    }

    /**
     * Switches the WYSIWYG editor by clicking on the "WYSIWYG" tab item.
     * 
     * @param wait {@code true} to wait for the rich text area to be initialized, {@code false} otherwise
     */
    public void switchToWysiwyg(boolean wait)
    {
        getContainer().findElement(By.xpath(String.format(TAB_ITEM_XPATH, "WYSIWYG"))).click();
        if (wait) {
            waitForSourceTextArea(false);
        }
    }

    /**
     * Waits for the source text area to have the specified state.
     * 
     * @param enabled whether the source text area should be enabled or disabled
     */
    private void waitForSourceTextArea(final boolean enabled)
    {
        getUtil().waitUntilCondition(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                WebElement sourceTextArea = getContainer().findElement(By.className("xPlainTextEditor"));
                return sourceTextArea.isEnabled() == enabled ? sourceTextArea : null;
            }
        });
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
        return getDriver().findElement(By.xpath("//div[starts-with(@id, '" + fieldId + "_container')]"));
    }
}
