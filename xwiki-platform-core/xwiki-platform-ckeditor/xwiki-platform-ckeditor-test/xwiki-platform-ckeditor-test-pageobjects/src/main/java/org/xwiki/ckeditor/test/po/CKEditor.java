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

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.image.ImageDialogEditModal;
import org.xwiki.ckeditor.test.po.image.ImageDialogSelectModal;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models a CKEditor instance.
 *
 * @version $Id$
 * @since 1.13
 */
@Unstable
public class CKEditor extends BaseElement
{
    /**
     * The editor field name.
     */
    private final String name;

    /**
     * Create a new instance that can be used to interact with the specified CKEditor instance.
     * 
     * @param name the editor field name
     */
    public CKEditor(String name)
    {
        this.name = name;
    }

    /**
     * Waits for CKEditor to load.
     *
     * @return this editor instance
     */
    public CKEditor waitToLoad()
    {
        StringBuilder script = new StringBuilder();
        script.append("var name = arguments[0];\n");
        script.append("var callback = arguments[1];\n");
        script.append("require(['deferred!ckeditor'], function(ckeditorPromise) {\n");
        script.append("  ckeditorPromise.done(function(ckeditor) {\n");
        script.append("    // In case the editor instance is not ready yet.\n");
        script.append("    var handler = ckeditor.on('instanceReady', function(event) {\n");
        script.append("      if (name === event.editor.name) {\n");
        script.append("        handler.removeListener();\n");
        script.append("        callback();\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("    // In case the editor instance is ready.\n");
        script.append("    var instance = ckeditor.instances[name];\n");
        script.append("    instance && instance.status === 'ready' && callback();\n");
        script.append("  });\n");
        script.append("});\n");

        XWikiWebDriver driver = getDriver();
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
        driver.executeAsyncScript(script.toString(), this.name);

        return this;
    }

    /**
     * @return the rich text area
     */
    public RichTextAreaElement getRichTextArea()
    {
        // The in-line frame element is renewed while editing so we can't cache it.
        return new RichTextAreaElement((WebElement) getDriver().executeScript(
            "return CKEDITOR.instances[arguments[0]].ui.contentsElement.find('iframe').getItem(0).$;", this.name));
    }

    /**
     * Click on the CKEditor image button.
     *
     * @return a page object for the image selection modal
     * @since 14.7RC1
     */
    public ImageDialogSelectModal clickImageButton()
    {
        internalClickImageButton();
        return new ImageDialogSelectModal().waitUntilReady();
    }

    /**
     * Click on the link button on the toolbar.
     *
     * @return the page object to interact with the link selection modal
     * @since 15.0RC1
     * @since 14.10.3
     */
    public LinkSelectorModal clickLinkButton()
    {
        getDriver().findElement(By.className("cke_button__link_icon")).click();
        return new LinkSelectorModal();
    }

    /**
     * Click on the CKEditor image button when an image widget is on focus (i.e., the image modal will be opened in edit
     * mode).
     *
     * @return a page object for the image edit modal
     * @since 14.8RC1
     */
    public ImageDialogEditModal clickImageButtonWhenImageExists()
    {
        internalClickImageButton();
        return new ImageDialogEditModal().waitUntilReady();
    }

    /**
     * Execute the runnable on the context of the CKEditor iframe.
     *
     * @param runnable the action to run on the context of the CKEditor iframe
     * @since 14.8RC1
     */
    public void executeOnIframe(Runnable runnable)
    {
        try {
            getDriver().switchTo().frame(getDriver().findElement(By.cssSelector("iframe.cke_wysiwyg_frame")));
            runnable.run();
        } finally {
            getDriver().switchTo().parentFrame();
        }
    }


    /**
     * Click the numbered list action, by first unfolding the list menu and selecting the numbered list item.
     *
     * @since 14.10.22
     */
    public void clickNumberedList()
    {
        getDriver().findElement(By.className("cke_button__lists")).click();
        WebElement subMenuFrame = getDriver().findElement(By.cssSelector("iframe.cke_panel_frame"));

        try {
            getDriver().switchTo().frame(subMenuFrame);
            getDriver().findElement(By.className("cke_menubutton__toolbar_numberedlist")).click();
        } finally {
            getDriver().switchTo().parentFrame();
        }
    }

    private void internalClickImageButton()
    {
        getDriver().findElement(By.className("cke_button__image_icon")).click();
    }
}
