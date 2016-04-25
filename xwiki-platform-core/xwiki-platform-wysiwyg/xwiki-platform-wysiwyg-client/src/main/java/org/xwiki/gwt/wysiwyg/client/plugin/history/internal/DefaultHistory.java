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
package org.xwiki.gwt.wysiwyg.client.plugin.history.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.PasteEvent;
import org.xwiki.gwt.dom.client.PasteHandler;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.Text;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.wysiwyg.client.plugin.history.History;

import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;

/**
 * Default implementation for {@link History}.
 * 
 * @version $Id$
 */
public class DefaultHistory implements History, KeyDownHandler, PasteHandler, CommandListener
{
    /**
     * The list of commands that should be ignored, meaning that they shouldn't generate history entries.
     */
    private static final List<Command> IGNORED_COMMANDS =
        Arrays.asList(Command.UNDO, Command.REDO, new Command("submit"), Command.UPDATE, Command.RESET, Command.ENABLE);

    /**
     * The rich text area for which we record the history. Actions taken on this rich text area trigger the update of
     * the history. Using the {@link History} interface the content of this rich text area can be reverted to a previous
     * version.
     */
    private final RichTextArea textArea;

    /**
     * The maximum number of entries the history can hold. While the history is full, each time we want to add a new
     * entry we have to remove the oldest one to make room.
     */
    private final int capacity;

    /**
     * The oldest stored history entry.
     */
    private Entry oldestEntry;

    /**
     * Points to the history entry storing the current version of the edited content.
     */
    private Entry currentEntry;

    /**
     * The previous keyboard action done by the user. The history is updated whenever the user changes the type of
     * keyboard action he does on the edited content.
     */
    private KeyboardAction previousKeyboardAction;

    /**
     * Starts to record the history of the given rich text area. At each moment the number of history entries stored is
     * at most the specified capacity.
     * 
     * @param textArea the rich text area for which to record the history.
     * @param capacity the maximum number of history entries that can be stored.
     */
    public DefaultHistory(RichTextArea textArea, int capacity)
    {
        assert (capacity > 1);
        this.capacity = capacity;

        this.textArea = textArea;
        textArea.addPasteHandler(this);
        textArea.addKeyDownHandler(this);
        textArea.getCommandManager().addCommandListener(this);
    }

    @Override
    public boolean canRedo()
    {
        return textArea.getDocument() != null && currentEntry != null && currentEntry.getNextEntry() != null
            && !isDirty();
    }

    @Override
    public boolean canUndo()
    {
        return textArea.getDocument() != null && currentEntry != null
            && (currentEntry.getPreviousEntry() != null || isDirty());
    }

    @Override
    public void redo()
    {
        if (canRedo()) {
            load(currentEntry.getNextEntry());
        }
    }

    @Override
    public void undo()
    {
        if (canUndo()) {
            if (!canRedo()) {
                save();
            }
            load(currentEntry.getPreviousEntry());
        }
    }

    /**
     * NOTE: the number of stored entries is computed each time because the history length can drop significantly if the
     * user reverts and continues editing.
     * 
     * @return true if the number of history entries stored is equal or exceeds the {@link #capacity}.
     */
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

    /**
     * @return true if there are no history entries stored.
     */
    private boolean isEmpty()
    {
        return oldestEntry == null;
    }

    /**
     * @return true if the user is doing an edit action on the current version of the edited content. The stored HTML
     *         content in the current history entry should be different from the one in the text area.
     */
    private boolean isDirty()
    {
        return currentEntry != null && !currentEntry.getContent().equals(textArea.getHTML());
    }

    /**
     * @param entry the history entry to load in the rich text area.
     */
    private void load(Entry entry)
    {
        currentEntry = entry;

        textArea.setHTML(entry.getContent());
        Document doc = textArea.getDocument();

        Range range = doc.createRange();
        range.setStart(getNode(doc, entry.getStartPath()), entry.getStartPath().get(0));
        range.setEnd(getNode(doc, entry.getEndPath()), entry.getEndPath().get(0));

        Selection selection = doc.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * @param node a DOM node.
     * @param offset the offset inside the given node. It represents the number of characters in case of text node and
     *            the number of child nodes otherwise.
     * @return the path from the given node to root of the DOM tree, where each token in the path represents the
     *         normalized index of the node at that level.
     */
    private static List<Integer> getPath(Node node, int offset)
    {
        List<Integer> path = new ArrayList<Integer>();
        switch (node.getNodeType()) {
            case 4:
                // CDATA_SECTION_NODE
            case 8:
                // COMMENT_NODE
                path.add(offset);
                break;
            case Node.TEXT_NODE:
                path.add(Text.as(node).getOffset() + offset);
                break;
            case Node.ELEMENT_NODE:
                if (offset == node.getChildNodes().getLength()) {
                    path.add(DOMUtils.getInstance().getNormalizedChildCount(node));
                } else {
                    path.add(DOMUtils.getInstance().getNormalizedNodeIndex(node.getChildNodes().getItem(offset)));
                }
                break;
            default:
                throw new IllegalArgumentException(DOMUtils.UNSUPPORTED_NODE_TYPE);
        }
        Node ancestor = node;
        while (ancestor.getParentNode() != null) {
            path.add(DOMUtils.getInstance().getNormalizedNodeIndex(ancestor));
            ancestor = ancestor.getParentNode();
        }
        return path;
    }

    /**
     * @param doc a DOM document
     * @param path a DOM path. Each token in the path is a node index.
     * @return the node at the end of the given path in the specified DOM tree.
     */
    private static Node getNode(Document doc, List<Integer> path)
    {
        Node node = doc;
        for (int i = path.size() - 1; i > 1; i--) {
            node = node.getChildNodes().getItem(path.get(i));
        }
        assert (node.getNodeType() == Node.ELEMENT_NODE);
        if (node.getChildNodes().getLength() == 0) {
            // If this element had an empty text node as child it was lost so we recreate it.
            node.appendChild(node.getOwnerDocument().createTextNode(""));
        }
        return node.getChildNodes().getItem(path.get(1));
    }

    /**
     * Saves the current state of the underlying rich text area.
     */
    private void save()
    {
        if ((!isEmpty() && !isDirty()) || textArea.getDocument() == null) {
            return;
        }

        Selection selection = textArea.getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            return;
        }
        Range range = selection.getRangeAt(0);

        List<Integer> startPath = getPath(range.getStartContainer(), range.getStartOffset());
        List<Integer> endPath = getPath(range.getEndContainer(), range.getEndOffset());

        Entry newestEntry = new Entry(textArea.getHTML(), startPath, endPath);
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

    @Override
    public void onKeyDown(KeyDownEvent event)
    {
        if (event.getSource() == textArea && !event.isControlKeyDown()) {
            KeyboardAction currentKeyboardAction = KeyboardAction.valueOf(event.getNativeKeyCode());
            if (isEmpty() || currentKeyboardAction != previousKeyboardAction) {
                save();
            }
            previousKeyboardAction = currentKeyboardAction;
        }
    }

    @Override
    public void onPaste(PasteEvent event)
    {
        if (event.getSource() == textArea) {
            save();
            previousKeyboardAction = null;
        }
    }

    @Override
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        if (sender == textArea.getCommandManager() && !IGNORED_COMMANDS.contains(command)) {
            save();
            previousKeyboardAction = null;
        }
        return false;
    }

    @Override
    public void onCommand(CommandManager sender, Command command, String param)
    {
        // ignore
    }
}
