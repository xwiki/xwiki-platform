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
package com.xpn.xwiki.wysiwyg.client.ui.cmd;

public class Command
{
    /**
     * This command will set the background color of the document.
     */
    public static final Command BACK_COLOR = new Command("backcolor");

    /**
     * If there is no selection, the insertion point will set bold for subsequently typed characters.<br/> If there is
     * a selection and all of the characters are already bold, the bold will be removed. Otherwise, all selected
     * characters will become bold.
     */
    public static final Command BOLD = new Command("bold");

    /**
     * This command will make the editor readonly (true) or editable (false). Anticipated usage is for temporarily
     * disabling input while something else is occurring elsewhere in the web page.
     */
    public static final Command CONTENT_READ_ONLY = new Command("contentReadOnly");

    /**
     * If there is a selection, this command will copy the selection to the clipboard. If there isn't a selection,
     * nothing will happen.
     */
    public static final Command COPY = new Command("copy");

    /**
     * This command will not do anything if no selection is made. If there is a selection, a link will be inserted
     * around the selection with the url parameter as the href of the link.
     */
    public static final Command CREATE_LINK = new Command("createlink");

    /**
     * If there is a selection, this command will copy the selection to the clipboard and remove the selection from the
     * edit control. If there isn't a selection, nothing will happen.
     */
    public static final Command CUT = new Command("cut");

    /**
     * This command will add a <code>&lt;small&gt;</code> tag around selection or at insertion point.
     */
    public static final Command DECREASE_FONT_SIZE = new Command("decreasefontsize");

    /**
     * This command will delete all text and objects that are selected. If no text is selected it deletes one character
     * to the right. This is similar to the Delete button on the keyboard.
     */
    public static final Command DELETE = new Command("delete");

    /**
     * This command will set the font face for a selection or at the insertion point if there is no selection.<br/>The
     * given string is such as would be used in the "name" attribute of the font tag.
     */
    public static final Command FONT_NAME = new Command("fontname");

    /**
     * This command will set the fontsize for a selection or at the insertion point if there is no selection.<br/>The
     * given number is such as would be used in the "size" attribute of the font tag.
     */
    public static final Command FONT_SIZE = new Command("fontsize");

    /**
     * This command will set the text color of the selection or at the insertion point.
     */
    public static final Command FORE_COLOR = new Command("forecolor");

    /**
     * Adds an HTML block-style tag around a selection or at the insertion point line. Requires a tag-name string to be
     * passed in as a value argument. Virtually all block style tags can be used (eg. "H1", "EM", "BUTTON", "TEXTAREA").
     * (Internet Explorer supports only heading tags H1 - H6, ADDRESS, and PRE.)
     */
    public static final Command FORMAT_BLOCK = new Command("formatblock");

    /**
     * Selected block will be formatted as the given type of heading.
     */
    public static final Command HEADING = new Command("heading");

    /**
     * This command will set the hilite color of the selection or at the insertion point. It only works with
     * styleWithCSS enabled.
     */
    public static final Command HILITE_COLOR = new Command("hilitecolor");

    /**
     * This command will add a <code>&lt;big&gt;</code> tag around selection or at insertion point.
     */
    public static final Command INCREASE_FONT_SIZE = new Command("increasefontsize");

    /**
     * Indent the block where the caret is located. If the caret is inside a list, that item becomes a sub-item one
     * level deeper.
     */
    public static final Command INDENT = new Command("indent");

    /**
     * Selects whether pressing return inside a paragraph creates another paragraph or just inserts a
     * <code>&lt;br&gt;</code> tag.
     */
    public static final Command INSERT_BR_ON_RETURN = new Command("insertbronreturn");

    /**
     * This command will insert a horizontal rule (line) at the insertion point.<br/>Does it delete the selection? Yes!
     */
    public static final Command INSERT_HORIZONTAL_RULE = new Command("inserthorizontalrule");

    /**
     * This command will insert the given html into the <body> in place of the current selection or at the caret
     * location.<br/>The given string is the HTML to insert.
     */
    public static final Command INSERT_HTML = new Command("inserthtml");

    /**
     * This command will insert an image (referenced by the given url) at the insertion point.
     */
    public static final Command INSERT_IMAGE = new Command("insertimage");

    /**
     * Depends on the selection. If the caret is not inside a non-LI block, that block becomes the first LI and an OL.
     * If the caret is inside a bulleted item, the bulleted item becomes a numbered item.
     */
    public static final Command INSERT_ORDERED_LIST = new Command("insertorderedlist");

    /**
     * Depends on the selection. If the caret is not inside a non-LI block, that block becomes the first LI and UL. If
     * the caret is inside a numbered item, the numbered item becomes a bulleted item.
     */
    public static final Command INSERT_UNORDERED_LIST = new Command("insertunorderedlist");

    /**
     * Inserts a new paragraph.
     */
    public static final Command INSERT_PARAGRAPH = new Command("insertparagraph");

    /**
     * If there is no selection, the insertion point will set italic for subsequently typed characters.<br/>If there is
     * a selection and all of the characters are already italic, the italic will be removed. Otherwise, all selected
     * characters will become italic.
     */
    public static final Command ITALIC = new Command("italic");

    /**
     * Center-aligns the current block.
     */
    public static final Command JUSTIFY_CENTER = new Command("justifycenter");

    /**
     * Fully-justifies the current block.
     */
    public static final Command JUSTIFY_FULL = new Command("justifyfull");

    /**
     * Left-aligns the current block.
     */
    public static final Command JUSTIFY_LEFT = new Command("justifyleft");

    /**
     * Right aligns the current block.
     */
    public static final Command JUSTIFY_RIGHT = new Command("justifyright");

    /**
     * Outdent the block where the caret is located. If the block is not indented prior to calling outdent, nothing will
     * happen.<br/>If the caret is in a list item, the item will bump up a level in the list or break out of the list
     * entirely.
     */
    public static final Command OUTDENT = new Command("outdent");

    /**
     * This command will paste the contents of the clipboard at the location of the caret. If there is a selection, it
     * will be deleted prior to the insertion of the clipboard's contents.
     */
    public static final Command PASTE = new Command("paste");

    /**
     * This command will redo the previous undo action. If undo was not the most recent action, this command will have
     * no effect.
     */
    public static final Command REDO = new Command("redo");

    /**
     * Removes inline formatting from the current selection.
     */
    public static final Command REMOVE_FORMAT = new Command("removeformat");

    /**
     * This command will select all of the contents within the editable area.
     */
    public static final Command SELECT_ALL = new Command("selectall");

    /**
     * If there is no selection, the insertion point will set strikethrough for subsequently typed characters.<br/>If
     * there is a selection and all of the characters are already striked, the strikethrough will be removed. Otherwise,
     * all selected characters will have a line drawn through them.
     */
    public static final Command STRIKE_THROUGH = new Command("strikethrough");

    /**
     * This command is used for toggling the format of generated content. By default (at least today), this is true. An
     * example of the differences is that the "bold" command will generate <code>&lt;b&gt;</code> if the styleWithCSS
     * command is false and generate css style attribute if the styleWithCSS command is true.
     */
    public static final Command STYLE_WITH_CSS = new Command("styleWithCSS");

    /**
     * If there is no selection, the insertion point will set subscript for subsequently typed characters.<br/>If there
     * is a selection and all of the characters are already subscripted, the subscript will be removed. Otherwise, all
     * selected characters will be drawn slightly lower than normal text.
     */
    public static final Command SUB_SCRIPT = new Command("subscript");

    /**
     * If there is no selection, the insertion point will set superscript for subsequently typed characters.<br/>If
     * there is a selection and all of the characters are already superscripted, the superscript will be removed.
     * Otherwise, all selected characters will be drawn slightly higher than normal text.
     */
    public static final Command SUPER_SCRIPT = new Command("superscript");

    /**
     * If there is no selection, the insertion point will set underline for subsequently typed characters.<br/>If there
     * is a selection and all of the characters are already underlined, the underline will be removed. Otherwise, all
     * selected characters will become underlined.
     */
    public static final Command UNDERLINE = new Command("underline");

    /**
     * This command will undo the previous action. If no action has occurred in the document, then this command will
     * have no effect.
     */
    public static final Command UNDO = new Command("undo");

    /**
     * If the insertion point is within a link or if the current selection contains a link, the link will be removed and
     * the text will remain.
     */
    public static final Command UNLINK = new Command("unlink");

    /**
     * @see #STYLE_WITH_CSS
     */
    public static final Command USE_CSS = new Command("useCSS");

    protected String name;

    public Command(String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#toString()
     */
    public String toString()
    {
        return name;
    }
}
