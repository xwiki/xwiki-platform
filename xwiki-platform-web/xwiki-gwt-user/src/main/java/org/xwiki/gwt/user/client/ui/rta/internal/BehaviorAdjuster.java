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
package org.xwiki.gwt.user.client.ui.rta.internal;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.TableCellElement;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;

/**
 * Adjusts the behavior of the rich text area to meet the cross browser specification.<br/>
 * The built-in WYSIWYG editor provided by all modern browsers may react differently to user input (like typing) on
 * different browsers. This class serves as a base class for browser specific behavior adjustment.
 * 
 * @version $Id$
 */
public class BehaviorAdjuster
{
    /**
     * The name of the <code>&lt;li&gt;</code> tag.
     */
    public static final String LI = "li";

    /**
     * The name of the <code>&lt;td&gt;</code> tag.
     */
    public static final String TD = "td";

    /**
     * The name of the <code>&lt;th&gt;</code> tag.
     */
    public static final String TH = "th";

    /**
     * Collection of DOM utility methods.
     */
    protected DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * The rich text area whose behavior is being adjusted.
     */
    private RichTextArea textArea;

    /**
     * @return The rich text area whose behavior is being adjusted.
     */
    public RichTextArea getTextArea()
    {
        return textArea;
    }

    /**
     * NOTE: We were forced to add this method because instances of this class are created using deferred binding and
     * thus we cannot pass the rich text area as a parameter to the constructor. As a consequence this method can be
     * called only once.
     * 
     * @param textArea The textArea whose behavior needs to be adjusted.
     */
    public void setTextArea(RichTextArea textArea)
    {
        if (this.textArea != null) {
            throw new IllegalStateException("Text area has already been set!");
        }
        this.textArea = textArea;
    }

    /**
     * Called by the underlying rich text are when user actions trigger browser events, before any registered listener
     * is notified.
     * 
     * @param event the native event that was fired
     * @see RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     * @see RichTextArea#getCurrentEvent()
     */
    public void onBeforeBrowserEvent(Event event)
    {
        switch (event.getTypeInt()) {
            case Event.ONMOUSEDOWN:
                onBeforeMouseDown(event);
                break;
            case Event.ONMOUSEUP:
                onBeforeMouseUp(event);
                break;
            default:
                break;
        }
    }

    /**
     * Called by the underlying rich text are when user actions trigger browser events, after all the registered
     * listeners have been notified.
     * 
     * @param event the native event that was fired
     * @see RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     * @see RichTextArea#getCurrentEvent()
     */
    public void onBrowserEvent(Event event)
    {
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getTypeInt()) {
            case Event.ONKEYDOWN:
                onKeyDown(event);
                break;
            case Event.ONKEYPRESS:
                onKeyPress(event);
                break;
            case Event.ONLOAD:
                onLoad(event);
                break;
            default:
                break;
        }
    }

    /**
     * Called when a KeyDown event is triggered inside the rich text area.
     * 
     * @param event the native event that was fired
     */
    protected void onKeyDown(Event event)
    {
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyCodes.KEY_DOWN:
                onDownArrow(event);
                break;
            case KeyCodes.KEY_UP:
                onUpArrow(event);
                break;
            default:
                break;
        }
    }

    /**
     * Called when a KeyPress event is triggered inside the rich text area.
     * 
     * @param event the native event that was fired
     */
    protected void onKeyPress(Event event)
    {
        switch (event.getKeyCode()) {
            case KeyCodes.KEY_TAB:
                onTab(event);
                break;
            case KeyCodes.KEY_DELETE:
                onDelete(event);
                break;
            case KeyCodes.KEY_BACKSPACE:
                onBackSpace(event);
                break;
            default:
                break;
        }
    }

    /**
     * Overwrites the default rich text area behavior when the Tab key is being pressed.
     * 
     * @param event the native event that was fired
     */
    protected void onTab(Event event)
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            return;
        }

        // Prevent the default browser behavior.
        event.xPreventDefault();

        // See in which context the tab key has been pressed.
        Range range = selection.getRangeAt(0);
        List<String> specialTags = Arrays.asList(new String[] {LI, TD, TH});
        Node ancestor = range.getStartContainer();
        int index = specialTags.indexOf(ancestor.getNodeName().toLowerCase());
        while (ancestor != null && index < 0) {
            ancestor = ancestor.getParentNode();
            if (ancestor != null) {
                index = specialTags.indexOf(ancestor.getNodeName().toLowerCase());
            }
        }

        // Handle the tab key depending on the context.
        switch (index) {
            case 0:
                onTabInListItem(event, ancestor);
                break;
            case 1:
            case 2:
                onTabInTableCell(event, (TableCellElement) ancestor);
                break;
            default:
                onTabDefault(event);
                break;
        }
    }

    /**
     * Tab key has been pressed in an ordinary context. If the Shift key was not pressed then the current selection will
     * be replaced by 4 spaces. Otherwise no action will be taken.
     * 
     * @param event the native event that was fired
     */
    protected void onTabDefault(Event event)
    {
        if (event.getShiftKey()) {
            // Do nothing.
        } else {
            if (getTextArea().getCommandManager().isEnabled(Command.INSERT_HTML)) {
                getTextArea().getCommandManager().execute(Command.INSERT_HTML, "&nbsp;&nbsp;&nbsp;&nbsp;");
                getTextArea().getDocument().getSelection().collapseToEnd();
            }
        }
    }

    /**
     * Tab key has been pressed inside a list item. If the selection is collapsed at the beginning of a list item then
     * indent or outdent that list item depending on the Shift key. Otherwise use the default behavior for Tab key.
     * 
     * @param event the native event that was fired
     * @param item the list item in which the tab key has been pressed
     */
    protected void onTabInListItem(Event event, Node item)
    {
        Range range = getTextArea().getDocument().getSelection().getRangeAt(0);
        if (!range.isCollapsed() || !isAtStart(item, range)) {
            onTabDefault(event);
        } else {
            Command command = event.getShiftKey() ? Command.OUTDENT : Command.INDENT;
            if (getTextArea().getCommandManager().isEnabled(command)) {
                getTextArea().getCommandManager().execute(command);
            }
        }
    }

    /**
     * Tab key has been pressed inside a table cell.
     * 
     * @param event the native event that was fired
     * @param cell The table cell in which the tab key has been pressed.
     */
    protected void onTabInTableCell(Event event, TableCellElement cell)
    {
        Node nextCell = event.getShiftKey() ? cell.getPreviousCell() : cell.getNextCell();
        if (nextCell == null) {
            if (event.getShiftKey()) {
                return;
            } else {
                getTextArea().getCommandManager().execute(new Command("insertrowafter"));
                nextCell = cell.getNextCell();
            }
        }

        Selection selection = getTextArea().getDocument().getSelection();
        Range range = selection.getRangeAt(0);

        // Place the caret at the beginning of the next cell.
        Node leaf = domUtils.getFirstLeaf(nextCell);
        if (leaf == nextCell || leaf.getNodeType() == Node.TEXT_NODE) {
            range.setStart(leaf, 0);
        } else {
            range.setStartBefore(leaf);
        }

        range.collapse(true);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * @param container A block level element containing the start of the given range.
     * @param range A DOM range.
     * @return true if the start of the given range is at the beginning of its block level container.
     */
    protected boolean isAtStart(Node container, Range range)
    {
        if (!container.hasChildNodes()) {
            return true;
        }
        if (range.getStartOffset() > 0) {
            return false;
        }
        return domUtils.getFirstLeaf(container) == domUtils.getFirstLeaf(range.getStartContainer());
    }

    /**
     * Overwrites the default rich text area behavior when the Down arrow key is being pressed.
     * 
     * @param event the native event that was fired
     */
    protected void onDownArrow(Event event)
    {
        navigateOutsideTableCell(event, false);
    }

    /**
     * Overwrites the default rich text area behavior when the Up arrow key is being pressed.
     * 
     * @param event the native event that was fired
     */
    protected void onUpArrow(Event event)
    {
        navigateOutsideTableCell(event, true);
    }

    /**
     * Inserts a paragraph before or after the table containing the selection.
     * <p>
     * We decided to use Control/Meta+UpArrow for inserting a paragraph before a table and Control/Meta+DownArrow for
     * inserting a paragraph after a table. Here's the rationale:
     * <ul>
     * <li>We can't reliably detect if the user can place the caret before of after a table. Our playground is the DOM
     * tree which we fully control but in the end the browser decides how to render each node. The table can have
     * previous siblings or previous nodes in the DOM tree but they may not be rendered at all (as it happens with HTML
     * garbage like empty elements) or not rendered before/after the table (as it happens with absolute positioned
     * elements). So we have to insert the paragraph each time.</li>
     * <li>We can't use the same key to insert a paragraph before and after the table because we can have a table with
     * just one empty cell. So we can't rely on the Enter key.</li>
     * <li>We can't use just the navigation keys because they would insert a paragraph before/after the table even when
     * the user can navigate outside of the table.</li>
     * </ul>
     * We can replace the Ctrl with Alt. The idea is to use the Up/Down arrow keys with a modifier. They will work form
     * any table cell.
     * 
     * @param event the native event that was fired
     * @param before {@code true} to insert a paragraph before the table, {@code false} to insert a paragraph after the
     *            table
     */
    protected void navigateOutsideTableCell(Event event, boolean before)
    {
        // Navigate only if the Control or Meta modifiers are pressed along with the Up/Down arrow keys.
        if (event.getAltKey() || event.getShiftKey() || !(event.getCtrlKey() ^ event.getMetaKey())) {
            return;
        }

        Selection selection = getTextArea().getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            return;
        } else {
            selection.collapseToStart();
        }

        Range range = selection.getRangeAt(0);
        Node ancestor = domUtils.getFirstAncestor(range.getStartContainer(), "table");
        if (ancestor == null) {
            return;
        }

        event.xPreventDefault();

        Document document = getTextArea().getDocument();
        Node paragraph = document.createPElement();
        paragraph.appendChild(document.createTextNode(""));

        if (before) {
            ancestor.getParentNode().insertBefore(paragraph, ancestor);
        } else {
            domUtils.insertAfter(paragraph, ancestor);
        }

        range.selectNodeContents(paragraph.getFirstChild());
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * Overwrites the default rich text area behavior when the Delete key is being pressed.
     * 
     * @param event the native event that was fired
     */
    protected void onDelete(Event event)
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Overwrites the default rich text area behavior when the BackSpace key is being pressed.
     * 
     * @param event the native event that was fired
     */
    protected void onBackSpace(Event event)
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Overwrites the default rich text area behavior when the user holds the mouse down inside.
     * 
     * @param event the native event that was fired
     */
    protected void onBeforeMouseDown(Event event)
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Overwrites the default rich text area behavior when the user releases the mouse button.
     * 
     * @param event the native event that was fired
     */
    protected void onBeforeMouseUp(Event event)
    {
        // The height of the body element is given by its content. When the height of the body element is less than the
        // rich text area height the user can click outside of the body element, on the HTML element. When this happens
        // the selection can be lost. To prevent this we give the focus back to the body element.
        if (Element.is(event.getEventTarget())
            && "html".equalsIgnoreCase(Element.as(event.getEventTarget()).getTagName())) {
            textArea.setFocus(true);
        }
    }

    /**
     * Executes custom code after the rich text area is loaded.
     * 
     * @param event the native event that was fired
     */
    protected void onLoad(Event event)
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }
}
