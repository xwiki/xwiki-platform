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
package com.xpn.xwiki.wysiwyg.client.history.internal;

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.history.History;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.Selection;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.XShortcutKey;
import com.xpn.xwiki.wysiwyg.client.ui.XShortcutKeyFactory;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.util.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.util.Document;

public class DefaultHistory implements History, KeyboardListener, CommandListener
{
    public static class Entry
    {
        private final String content;

        private final String path;

        private Entry nextEntry;

        private Entry previousEntry;

        public Entry(String content, String path)
        {
            this.content = content;
            this.path = path;
        }

        public String getContent()
        {
            return content;
        }

        public String getPath()
        {
            return path;
        }

        public Entry getNextEntry()
        {
            return nextEntry;
        }

        public void setNextEntry(Entry nextEntry)
        {
            this.nextEntry = nextEntry;
        }

        public Entry getPreviousEntry()
        {
            return previousEntry;
        }

        public void setPreviousEntry(Entry previousEntry)
        {
            this.previousEntry = previousEntry;
        }

        public int getIndex()
        {
            int index = 0;
            Entry entry = this;
            while (entry.getPreviousEntry() != null) {
                index++;
                entry = entry.getPreviousEntry();
            }
            return index;
        }
    }

    public static enum KeyboardAction
    {
        UNDEFINED, INSERT_WORD, DELETE, INSERT_SPACE, INSERT_NEW_LINE, MOVE_CARET
    }

    private final XRichTextArea textArea;

    private final int capacity;

    private final XShortcutKey undoKey = XShortcutKeyFactory.createCtrlShortcutKey('Z');

    private final XShortcutKey redoKey = XShortcutKeyFactory.createCtrlShortcutKey('Y');

    private Entry oldestEntry;

    private Entry currentEntry;

    private KeyboardAction previousKeyboardAction;

    public DefaultHistory(XRichTextArea textArea, int capacity)
    {
        assert (capacity > 1);
        this.capacity = capacity;

        this.textArea = textArea;
        textArea.addShortcutKey(undoKey);
        textArea.addShortcutKey(redoKey);
        textArea.addKeyboardListener(this);
        textArea.getCommandManager().addCommandListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see History#canRedo()
     */
    public boolean canRedo()
    {
        return currentEntry != null && currentEntry.getNextEntry() != null && !isDirty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see History#canUndo()
     */
    public boolean canUndo()
    {
        return currentEntry != null && (currentEntry.getPreviousEntry() != null || isDirty());
    }

    /**
     * {@inheritDoc}
     * 
     * @see History#redo()
     */
    public void redo()
    {
        if (canRedo()) {
            load(currentEntry.getNextEntry());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see History#undo()
     */
    public void undo()
    {
        if (canUndo()) {
            if (!canRedo()) {
                save();
            }
            load(currentEntry.getPreviousEntry());
        }
    }

    private boolean isFull()
    {
        int entryCount = 0;
        Entry entry = oldestEntry;
        while (entry != null) {
            entryCount++;
            entry = entry.getNextEntry();
        }
        return entryCount >= capacity;
    }

    private boolean isEmpty()
    {
        return oldestEntry == null;
    }

    private boolean isDirty()
    {
        return currentEntry != null && !currentEntry.getContent().equals(textArea.getHTML());
    }

    private void load(Entry entry)
    {
        currentEntry = entry;

        textArea.setHTML(entry.getContent());
        Document doc = textArea.getDocument();

        Node node = doc;
        String[] path = entry.getPath().split(" ");
        for (int i = path.length - 1; i > 0; i--) {
            node = node.getChildNodes().getItem(Integer.parseInt(path[i]));
        }

        Range range = doc.createRange();
        int offset = Integer.parseInt(path[0]);
        range.setStart(node, offset);
        range.setEnd(node, offset);

        Selection selection = doc.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }

    private void save()
    {
        if (!isEmpty() && !isDirty()) {
            return;
        }

        Selection selection = textArea.getDocument().getSelection();
        Range range = selection.getRangeAt(0);

        StringBuffer path = new StringBuffer("");
        path.append(range.getEndOffset());
        Node node = range.getEndContainer();
        while (node.getParentNode() != null) {
            path.append(" ");
            path.append(DOMUtils.getInstance().getNodeIndex(node));
            node = node.getParentNode();
        }

        Entry newestEntry = new Entry(textArea.getHTML(), path.toString());
        if (currentEntry != null) {
            currentEntry.setNextEntry(newestEntry);
        }
        newestEntry.setPreviousEntry(currentEntry);
        currentEntry = newestEntry;
        if (oldestEntry == null) {
            oldestEntry = currentEntry;
        }

        if (isFull()) {
            oldestEntry = oldestEntry.getNextEntry();
            oldestEntry.setPreviousEntry(null);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        if (sender == textArea && (modifiers & KeyboardListener.MODIFIER_CTRL) == 0) {
            KeyboardAction currentKeyboardAction = getKeyboardAction(keyCode, modifiers);
            if (isEmpty() || currentKeyboardAction != previousKeyboardAction) {
                save();
            }
            previousKeyboardAction = currentKeyboardAction;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        if (sender == textArea) {
            if ((modifiers & KeyboardListener.MODIFIER_CTRL) != 0) {
                if (keyCode == undoKey.getKeyCode()) {
                    undo();
                } else if (keyCode == redoKey.getKeyCode()) {
                    redo();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if (sender == textArea.getCommandManager()) {
            if (command != Command.UNDO && command != Command.REDO) {
                save();
                previousKeyboardAction = KeyboardAction.UNDEFINED;
            }
        }
    }

    private KeyboardAction getKeyboardAction(int keyCode, int modifiers)
    {
        if (keyCode == ' ' || keyCode == KeyboardListener.KEY_TAB) {
            return KeyboardAction.INSERT_SPACE;
        } else if (keyCode == KeyboardListener.KEY_BACKSPACE || keyCode == KeyboardListener.KEY_DELETE) {
            return KeyboardAction.DELETE;
        } else if (keyCode == KeyboardListener.KEY_ENTER) {
            return KeyboardAction.INSERT_NEW_LINE;
        } else if (keyCode == KeyboardListener.KEY_DOWN || keyCode == KeyboardListener.KEY_END
            || keyCode == KeyboardListener.KEY_HOME || keyCode == KeyboardListener.KEY_LEFT
            || keyCode == KeyboardListener.KEY_PAGEDOWN || keyCode == KeyboardListener.KEY_PAGEUP
            || keyCode == KeyboardListener.KEY_RIGHT || keyCode == KeyboardListener.KEY_UP) {
            return KeyboardAction.MOVE_CARET;
        } else {
            return KeyboardAction.INSERT_WORD;
        }
    }
}
