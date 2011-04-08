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
package org.xwiki.gwt.wysiwyg.client.plugin.readonly;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

/**
 * Custom implementation of {@link ReadOnlyKeyboardHandler} for WebKit-based browsers.
 * 
 * @version $Id$
 */
public class SafariReadOnlyKeyboardHandler extends ReadOnlyKeyboardHandler
{
    /**
     * {@inheritDoc}
     * <p>
     * WebKit browsers don't restore the selection after an element that contained or was touched by the selection is
     * removed. Instead of manually removing the read-only element we select it and let the browser remove it.
     * 
     * @see ReadOnlyKeyboardHandler#onDelete(Event, Element)
     */
    @Override
    protected void onDelete(Event event, Element element)
    {
        Document document = (Document) element.getOwnerDocument();
        Selection selection = document.getSelection();
        Range range = document.createRange();
        range.selectNode(element);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * {@inheritDoc}
     * <p>
     * WebKit browsers don't restore the selection after an element that contained or was touched by the selection is
     * removed. Instead of manually removing the read-only element we extend the given range to include the element and
     * let the browser remove it.
     * 
     * @see ReadOnlyKeyboardHandler#onDelete(Event, Element, Range, boolean)
     */
    @Override
    protected void onDelete(Event event, Element element, Range range, boolean start)
    {
        if (element == null) {
            return;
        }
        Document document = (Document) element.getOwnerDocument();
        Selection selection = document.getSelection();
        if (start) {
            range.setStartBefore(element);
        } else {
            range.setEndAfter(element);
        }
        selection.removeAllRanges();
        selection.addRange(range);
    }
}
