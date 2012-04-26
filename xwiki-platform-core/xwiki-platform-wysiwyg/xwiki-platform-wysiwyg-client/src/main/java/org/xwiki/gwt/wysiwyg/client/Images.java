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
package org.xwiki.gwt.wysiwyg.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * This {@link ClientBundle} is used for all the button icons. Using a client bundle allows all of these images to be
 * packed into a single image, which saves a lot of HTTP requests, drastically improving startup time.
 * 
 * @version $Id$
 */
public interface Images extends ClientBundle
{
    /**
     * An instance of this client bundle that can be used anywhere in the code to extract images.
     */
    Images INSTANCE = GWT.create(Images.class);

    @Source("attachment.gif")
    ImageResource attachment();

    @Source("backcolor.gif")
    ImageResource backColor();

    @Source("bold.gif")
    ImageResource bold();

    @Source("charmap.gif")
    ImageResource charmap();

    @Source("table_delete_col.gif")
    ImageResource deleteCol();

    @Source("table_delete_row.gif")
    ImageResource deleteRow();

    @Source("forecolor.gif")
    ImageResource foreColor();

    @Source("hr.gif")
    ImageResource hr();

    @Source("image.gif")
    ImageResource image();

    @Source("import.gif")
    ImageResource importMenuEntryIcon();

    @Source("import.gif")
    ImageResource importWizardIcon();

    @Source("import_office_file.gif")
    ImageResource importOfficeFileMenuEntryIcon();

    @Source("paste.gif")
    ImageResource paste();

    @Source("indent.gif")
    ImageResource indent();

    @Source("table_delete.gif")
    ImageResource deleteTable();

    @Source("table_insert_col_after.gif")
    ImageResource insertColAfter();

    @Source("table_insert_col_before.gif")
    ImageResource insertColBefore();

    @Source("table_insert_row_after.gif")
    ImageResource insertRowAfter();

    @Source("table_insert_row_before.gif")
    ImageResource insertRowBefore();

    @Source("table.gif")
    ImageResource insertTable();

    @Source("italic.gif")
    ImageResource italic();

    @Source("justifycenter.gif")
    ImageResource justifyCenter();

    @Source("justifyfull.gif")
    ImageResource justifyFull();

    @Source("justifyleft.gif")
    ImageResource justifyLeft();

    @Source("justifyright.gif")
    ImageResource justifyRight();

    @Source("link.gif")
    ImageResource link();

    @Source("macro.gif")
    ImageResource macro();

    @Source("macro_edit.gif")
    ImageResource macroEdit();

    @Source("macro_insert.gif")
    ImageResource macroInsert();

    @Source("macro_refresh.gif")
    ImageResource macroRefresh();

    @Source("ol.gif")
    ImageResource ol();

    @Source("outdent.gif")
    ImageResource outdent();

    @Source("redo.gif")
    ImageResource redo();

    @Source("removeformat.gif")
    ImageResource removeFormat();

    @Source("strikethrough.gif")
    ImageResource strikeThrough();

    @Source("subscript.gif")
    ImageResource subscript();

    @Source("superscript.gif")
    ImageResource superscript();

    @Source("tt.gif")
    ImageResource teletype();

    @Source("ul.gif")
    ImageResource ul();

    @Source("underline.gif")
    ImageResource underline();

    @Source("undo.gif")
    ImageResource undo();

    @Source("unlink.gif")
    ImageResource unlink();
}
