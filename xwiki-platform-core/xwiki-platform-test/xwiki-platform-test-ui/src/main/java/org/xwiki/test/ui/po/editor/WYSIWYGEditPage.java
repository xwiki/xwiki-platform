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

import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.editor.wysiwyg.EditorElement;
import org.xwiki.test.ui.po.editor.wysiwyg.RichTextAreaElement;

/**
 * Represents the actions possible in WYSIWYG edit mode.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class WYSIWYGEditPage extends PreviewableEditPage
{
    /**
     * The WYSIWYG content editor.
     */
    private final EditorElement editor = new EditorElement("content");

    /**
     * Go to the passed page in WYSIWYG edit mode.
     */
    public static WYSIWYGEditPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page, "edit", "editor=wysiwyg");
        return new WYSIWYGEditPage();
    }

    /**
     * Get the <code>content</code> of the page.
     */
    public String getContent()
    {
        return editor.getRichTextArea().getText();
    }

    /**
     * Sets the content of the editor (rich text area).
     * 
     * @param content the content to be set
     */
    public void setContent(String content)
    {
        RichTextAreaElement textArea = editor.getRichTextArea();
        textArea.clear();
        textArea.sendKeys(content);
    }

    @Override
    public WYSIWYGEditPage waitUntilPageIsLoaded()
    {
        editor.waitToLoad();
        return this;
    }

    @Override
    public WikiEditPage editWiki()
    {
        // Make sure the editor is loaded before we try to click the edit menu entry.
        this.waitUntilPageIsLoaded();

        return super.editWiki();
    }

    @Override
    public WYSIWYGEditPage editWYSIWYG()
    {
        // Make sure the editor is loaded before we try to click the edit menu entry.
        this.waitUntilPageIsLoaded();

        return super.editWYSIWYG();
    }

    @Override
    public <T extends InlinePage> T editInline()
    {
        // Make sure the editor is loaded before we try to click the edit menu entry.
        this.waitUntilPageIsLoaded();

        return super.editInline();
    }

    @Override
    public RightsEditPage editRights()
    {
        // Make sure the editor is loaded before we try to click the edit menu entry.
        this.waitUntilPageIsLoaded();

        return super.editRights();
    }

    @Override
    public ObjectEditPage editObjects()
    {
        // Make sure the editor is loaded before we try to click the edit menu entry.
        this.waitUntilPageIsLoaded();

        return super.editObjects();
    }

    @Override
    public ClassEditPage editClass()
    {
        // Make sure the editor is loaded before we try to click the edit menu entry.
        this.waitUntilPageIsLoaded();

        return super.editClass();
    }
}
