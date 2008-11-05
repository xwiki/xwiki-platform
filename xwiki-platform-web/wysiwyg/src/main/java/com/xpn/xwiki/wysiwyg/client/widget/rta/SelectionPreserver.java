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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;

/**
 * Most of the plugins alter the DOM document edited with the rich text area by executing commands on the current
 * selection (thus on the current range). In some cases, a plugin needs to get user input before executing such a
 * command. It can gather the needed information by opening a dialog, for instance. In some browsers this may lead to
 * loosing the selection on the rich text area. In this case the plugin has to {@link #saveSelection()} before the
 * dialog is shown and {@link #restoreSelection()} after the dialog is closed.
 * 
 * @version $Id$
 */
public class SelectionPreserver
{
    /**
     * The rich text area whose selection is preserved.
     */
    private final RichTextArea rta;

    /**
     * The preserved range. Usually a selection contains just one range. So we save only the first range of the
     * selection.
     */
    private Range range;

    /**
     * Creates a new selection preserver for the specified rich text area.
     * 
     * @param rta The rich text area whose selection will be preserved.
     */
    public SelectionPreserver(RichTextArea rta)
    {
        this.rta = rta;
    }

    /**
     * Saves the first range in the current selection for later changes. The selection is taken from the target
     * document.
     * 
     * @see #range
     * @see #restoreSelection()
     */
    public void saveSelection()
    {
        range = rta.getDocument().getSelection().getRangeAt(0);
    }

    /**
     * Restores the saved selection on the target document.
     * 
     * @see #range
     * @see #saveSelection()
     */
    public void restoreSelection()
    {
        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        range = null;
    }
}
