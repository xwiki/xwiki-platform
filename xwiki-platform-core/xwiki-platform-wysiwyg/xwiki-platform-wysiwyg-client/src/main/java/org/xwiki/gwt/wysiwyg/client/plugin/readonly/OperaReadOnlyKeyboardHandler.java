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
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.dom.client.Node;

/**
 * Custom implementation of {@link ReadOnlyKeyboardHandler} for Opera browser.
 * 
 * @version $Id$
 */
public class OperaReadOnlyKeyboardHandler extends ReadOnlyKeyboardHandler
{
    /**
     * {@inheritDoc}
     * <p>
     * Opera doesn't allow us to place the caret before/after the macro unless we put the caret in a text node..
     */
    @Override
    protected void moveCaretOutside(Node node, boolean before)
    {
        Document document = node.getOwnerDocument().cast();
        Node emptyText = document.createTextNode("");
        if (before) {
            node.getParentNode().insertBefore(emptyText, node);
        } else {
            node.getParentNode().insertAfter(emptyText, node);
        }
        Range range = document.createRange();
        range.selectNodeContents(emptyText);
        Selection selection = document.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }
}
