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
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.wysiwyg.AttachedImageSelectPane;
import org.xwiki.test.ui.po.editor.wysiwyg.EditorElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible in WYSIWYG edit mode.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class WYSIWYGEditPage extends PreviewableEditPage
{
    @FindBy(id = "xwikidoctitleinput")
    private WebElement titleField;

    @FindBy(name = "action_save")
    private WebElement saveAndViewSubmit;

    @FindBy(name = "parent")
    private WebElement parentInput;

    /**
     * The WYSIWYG content editor.
     */
    private final EditorElement editor = new EditorElement("content");

    /**
     * Go to the passed page in WYSIWYG edit mode.
     */
    public static WYSIWYGEditPage gotoPage(String space, String page)
    {
        BaseElement.getUtil().gotoPage(space, page, "edit", "editor=wysiwyg");
        return new WYSIWYGEditPage();
    }

    public String getDocumentTitle()
    {
        return this.titleField.getAttribute("value");
    }

    public ViewPage save()
    {
        this.saveAndViewSubmit.submit();
        return new ViewPage();
    }

    /**
     * Get the <code>content</code> of the page.
     */
    public String getContent()
    {
        // Handle both the TinyMCE editor and the GWT Editor depending on the syntax
        if ("xwiki/1.0".equals(getSyntaxId())) {
            String windowHandle = getDriver().getWindowHandle();
            getDriver().switchTo().frame("mce_editor_0");
            String content = getDriver().findElement(By.id("mceSpanFonts")).getText();
            getDriver().switchTo().window(windowHandle);
            return content;
        } else {
            return editor.getRichTextArea().getText();
        }
    }

    /**
     * @return the WYSIWYG content editor
     */
    public EditorElement getContentEditor()
    {
        return editor;
    }

    /**
     * Triggers the insert attached image wizard.
     * 
     * @return the pane used to select an attached image to insert
     */
    public AttachedImageSelectPane insertAttachedImage()
    {
        editor.getMenuBar().clickImageMenu();
        return editor.getMenuBar().clickInsertAttachedImageMenu();
    }

    /**
     * @return the value of the parent field.
     */
    public String getParent()
    {
        return this.parentInput.getAttribute("value");
    }
}
