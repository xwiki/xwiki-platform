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
package org.xwiki.blocknote.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the BlockNote editor.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
public class BlockNoteEditor extends BaseElement
{
    private final String name;

    private WebElement container;

    /**
     * Create a new instance that can be used to interact with the specified BlockNote instance.
     * 
     * @param name the editor field name
     */
    public BlockNoteEditor(String name)
    {
        this.name = name;
        this.waitToLoad();
    }

    /**
     * @return the name of this editor instance (usually matches the name of the form field the editor is attached to)
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Waits for BlockNote to load.
     *
     * @return this editor instance
     */
    public BlockNoteEditor waitToLoad()
    {
        this.container = this.getDriver().findElement(
            By.cssSelector(".xwiki-blocknote-wrapper:has(> .xwiki-blocknote > input[name='" + this.name + "'])"));
        return this;
    }

    /**
     * @return the toolbar of this editor
     */
    public BlockNoteToolBar getToolBar()
    {
        return new BlockNoteToolBar(this.container.findElement(By.className("bn-toolbar")));
    }

    /**
     * @return the rich text area of this editor
     */
    public BlockNoteRichTextArea getRichTextArea()
    {
        return new BlockNoteRichTextArea(this.container.findElement(By.cssSelector(".bn-container .bn-editor")));
    }
}
