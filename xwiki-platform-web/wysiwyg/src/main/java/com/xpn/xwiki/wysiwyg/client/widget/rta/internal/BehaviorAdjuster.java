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
package com.xpn.xwiki.wysiwyg.client.widget.rta.internal;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.TableCellElement;

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.SourcesLoadEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Adjusts the behavior of the rich text area to meet the cross browser specification.<br/>
 * The built-in WYSIWYG editor provided by all modern browsers may react differently to user input (like typing) on
 * different browsers. This class serves as a base class for browser specific behavior adjustment.
 * 
 * @version $Id$
 */
public class BehaviorAdjuster implements LoadListener
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
        // Workaround till GWT provides a way to detect when the rich text area has finished loading.
        if (textArea.getBasicFormatter() != null && textArea.getBasicFormatter() instanceof SourcesLoadEvents) {
            ((SourcesLoadEvents) textArea.getBasicFormatter()).addLoadListener(this);
        }
    }

    /**
     * Called by the underlying rich text are when user actions trigger browser events, before any registered listener
     * is notified.
     * 
     * @see RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     * @see RichTextArea#getCurrentEvent()
     */
    public void onBeforeBrowserEvent()
    {
        Event event = getTextArea().getCurrentEvent();
        switch (event.getTypeInt()) {
            case Event.ONMOUSEDOWN:
                onBeforeMouseDown();
                break;
            default:
                break;
        }
    }

    /**
     * Called by the underlying rich text are when user actions trigger browser events, after all the registered
     * listeners have been notified.
     * 
     * @see RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     * @see RichTextArea#getCurrentEvent()
     */
    public void onBrowserEvent()
    {
        Event event = getTextArea().getCurrentEvent();
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getTypeInt()) {
            case Event.ONKEYDOWN:
                onKeyDown();
                break;
            case Event.ONKEYPRESS:
                onKeyPress();
                break;
            default:
                break;
        }
    }

    /**
     * Called when a KeyDown event is triggered inside the rich text area.
     */
    protected void onKeyDown()
    {
        Event event = getTextArea().getCurrentEvent();
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyboardListener.KEY_DOWN:
                onDownArrow();
                break;
            case KeyboardListener.KEY_UP:
                onUpArrow();
                break;
            default:
                break;
        }
    }

    /**
     * Called when a KeyPress event is triggered inside the rich text area.
     */
    protected void onKeyPress()
    {
        Event event = getTextArea().getCurrentEvent();
        switch (event.getKeyCode()) {
            case KeyboardListener.KEY_TAB:
                onTab();
                break;
            case KeyboardListener.KEY_DELETE:
                onDelete();
                break;
            case KeyboardListener.KEY_BACKSPACE:
                onBackSpace();
                break;
            default:
                break;
        }
    }

    /**
     * Overwrites the default rich text area behavior when the Tab key is being pressed.
     */
    protected void onTab()
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            return;
        }

        // Prevent the default browser behavior.
        getTextArea().getCurrentEvent().xPreventDefault();

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
                onTabInListItem(ancestor);
                break;
            case 1:
            case 2:
                onTabInTableCell((TableCellElement) ancestor);
                break;
            default:
                onTabDefault();
                break;
        }
    }

    /**
     * Tab key has been pressed in an ordinary context. If the Shift key was not pressed then the current selection will
     * be replaced by 4 spaces. Otherwise no action will be taken.
     */
    protected void onTabDefault()
    {
        if (getTextArea().getCurrentEvent().getShiftKey()) {
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
     * @param item The list item in which the tab key has been pressed.
     */
    protected void onTabInListItem(Node item)
    {
        Range range = getTextArea().getDocument().getSelection().getRangeAt(0);
        if (!range.isCollapsed() || !isAtStart(item, range)) {
            onTabDefault();
        } else {
            Command command = getTextArea().getCurrentEvent().getShiftKey() ? Command.OUTDENT : Command.INDENT;
            if (getTextArea().getCommandManager().isEnabled(command)) {
                getTextArea().getCommandManager().execute(command);
            }
        }
    }

    /**
     * Tab key has been pressed inside a table cell.
     * 
     * @param cell The table cell in which the tab key has been pressed.
     */
    protected void onTabInTableCell(TableCellElement cell)
    {
        Node nextCell = getTextArea().getCurrentEvent().getShiftKey() ? cell.getPreviousCell() : cell.getNextCell();
        if (nextCell == null) {
            if (getTextArea().getCurrentEvent().getShiftKey()) {
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
     */
    protected void onDownArrow()
    {
        navigateOutsideTableCell(false);
    }

    /**
     * Overwrites the default rich text area behavior when the Up arrow key is being pressed.
     */
    protected void onUpArrow()
    {
        navigateOutsideTableCell(true);
    }

    /**
     * Inserts a paragraph before or after the table containing the selection.
     * <p>
     * We decided to use Ctrl+UpArrow for inserting a paragraph before a table and Ctrl+DownArrow for inserting a
     * paragraph after a table. Here's the rationale:
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
     * @param before {@code true} to insert a paragraph before the table, {@code false} to insert a paragraph after the
     *            table
     */
    protected void navigateOutsideTableCell(boolean before)
    {
        Event event = getTextArea().getCurrentEvent();
        if (!event.getCtrlKey() || event.getAltKey() || event.getShiftKey() || event.getMetaKey()) {
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
        Node paragraph = document.xCreatePElement();
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
     */
    protected void onDelete()
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Overwrites the default rich text area behavior when the BackSpace key is being pressed.
     */
    protected void onBackSpace()
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Overwrites the default rich text area behavior when the user holds the mouse down inside.
     */
    protected void onBeforeMouseDown()
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onError(Widget)
     */
    public void onError(Widget sender)
    {
        // Nothing to do upon load error.
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        adjustDragDrop(textArea.getDocument());
    }

    /**
     * Prevents the drag and drop default behavior by disabling the default events.
     * 
     * @param document the document in the loaded rich text area.
     */
    protected void adjustDragDrop(Document document)
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }
}
