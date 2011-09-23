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

import java.util.List;

/**
 * History entry. Stores the HTML content and the boundary points of the selection as they were when the entry has been
 * saved into the history. A boundary point is stored as the path from its container to the root of the DOM document.
 * The first token in the path is the offset within the container of the boundary point while the rest represent the
 * sibling-index of each of the ancestors of the container up to the root of the document.<br/>
 * The history mechanism has been implemented like a double-linked list, thus each entry knows the its next (newer) and
 * previous (older) entry.
 * 
 * @version $Id$
 */
public class Entry
{
    /**
     * The HTML content when this entry was saved.
     */
    private final String content;

    /**
     * The path to the start of the selection, when this entry was saved.
     */
    private final List<Integer> startPath;

    /**
     * The path to the end of the selection, when this entry was saved.
     */
    private final List<Integer> endPath;

    /**
     * The next (newer) history entry relative to this one.
     */
    private Entry nextEntry;

    /**
     * The previous (older) history entry relative to this one.
     */
    private Entry previousEntry;

    /**
     * Creates a new history entry.
     * 
     * @param content the HTML content.
     * @param startPath the DOM path to the start point of the selection.
     * @param endPath the DOM path to the end point of the selection.
     */
    public Entry(String content, List<Integer> startPath, List<Integer> endPath)
    {
        this.content = content;
        this.startPath = startPath;
        this.endPath = endPath;
    }

    /**
     * @return The HTML content stored by this history entry.
     */
    public String getContent()
    {
        return content;
    }

    /**
     * @return the path to the start point of the selection stored by this history entry.
     */
    public List<Integer> getStartPath()
    {
        return startPath;
    }

    /**
     * @return the path to the end point of the selection stored by this history entry.
     */
    public List<Integer> getEndPath()
    {
        return endPath;
    }

    /**
     * @return the next (newer) history entry relative to this one.
     */
    public Entry getNextEntry()
    {
        return nextEntry;
    }

    /**
     * Sets the next (newer) history entry relative to this one.
     * 
     * @param nextEntry the history entry to follow after this entry.
     */
    public void setNextEntry(Entry nextEntry)
    {
        this.nextEntry = nextEntry;
    }

    /**
     * @return the previous (older) history entry relative to this one.
     */
    public Entry getPreviousEntry()
    {
        return previousEntry;
    }

    /**
     * Sets the previous (older) history entry relative to this one.
     * 
     * @param previousEntry the history entry to precede this entry.
     */
    public void setPreviousEntry(Entry previousEntry)
    {
        this.previousEntry = previousEntry;
    }

    /**
     * @return the index of this history entry, where the oldest history entry always has index 0.
     */
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
