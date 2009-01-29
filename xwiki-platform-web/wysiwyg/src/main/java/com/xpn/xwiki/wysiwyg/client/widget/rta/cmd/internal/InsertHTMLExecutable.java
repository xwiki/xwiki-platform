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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Inserts an HTML fragment in place of the current selection. We overwrite the default implementation provided by the
 * predefined insertHTML command for two reasons:
 * <ul>
 * <li>Internet Explorer doesn't support the insertHTML predefined command.</li>
 * <li>Besides inserting the specified HTML in the edited DOM document, Mozilla also does some unwanted cleaning of the
 * DOM nodes like br's which leads to unexpected effects of executing this command. This is most annoying in tests when
 * we have to know how the DOM tree will be after executing the command.</li>
 * </ul>
 * 
 * @version $Id$
 */
public class InsertHTMLExecutable extends AbstractExecutable
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        Element container = rta.getDocument().xCreateDivElement().cast();
        container.xSetInnerHTML(param);

        Selection selection = rta.getDocument().getSelection();
        if (!selection.isCollapsed()) {
            // Delete the selected contents. The given HTML fragment will be inserted in place of the deleted text.
            // NOTE: We cannot use Range#deleteContents because it may lead to DTD-invalid HTML. That's because it
            // operates on any DOM tree without taking care of the underlying XML syntax, (X)HTML in our case. Let's use
            // the Delete command instead which is HTML-aware. Moreover, others could listen to this command and adjust
            // the DOM before we insert the HTML.
            rta.getCommandManager().execute(Command.DELETE);
        }
        // At this point the selection should be collapsed.
        Range range = selection.getRangeAt(0);
        range.insertNode(container.extractContents());
        selection.removeAllRanges();
        selection.addRange(range);

        return true;
    }
}
