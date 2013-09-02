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
package org.xwiki.wysiwyg.test.po;

import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible in WYSIWYG edit mode.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
public class WYSIWYGEditPage extends org.xwiki.test.ui.po.editor.WYSIWYGEditPage
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
        BaseElement.getUtil().gotoPage(space, page, "edit", "editor=wysiwyg");
        return new WYSIWYGEditPage();
    }

    @Override
    public WYSIWYGEditPage waitUntilPageIsLoaded()
    {
        return (WYSIWYGEditPage) super.waitUntilPageIsLoaded();
    }

    /**
     * @return the WYSIWYG content editor
     */
    public EditorElement getContentEditor()
    {
        return editor;
    }

    /**
     * Triggers the insert attached image wizard. Sets the content of the editor (rich text area).
     * 
     * @return the pane used to select an attached image to insert
     */
    public AttachedImageSelectPane insertAttachedImage()
    {
        editor.getMenuBar().clickImageMenu();
        return editor.getMenuBar().clickInsertAttachedImageMenu();
    }

    /**
     * Triggers the insert table wizard.
     * 
     * @return the pane used to configure the table to insert
     */
    public TableConfigPane insertTable()
    {
        editor.getMenuBar().clickTableMenu();
        return editor.getMenuBar().clickInsertTableMenu();
    }
}
