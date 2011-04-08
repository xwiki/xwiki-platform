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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

/**
 * Base class for browser specific implementations of {@link InsertHTMLExecutable}. We moved the implementation from
 * {@link InsertHTMLExecutable} to this class in order to allow classes that extend {@link InsertHTMLExecutable} to have
 * browser specific behavior.
 * 
 * @version $Id$
 */
public class InsertHTMLExecutableImpl
{

    /**
     * Deletes the selected content within the given rich text area.
     * 
     * @param rta a rich text area
     * @return the caret after the selection has been deleted
     */
    protected Range deleteSelection(RichTextArea rta)
    {
        // Delete the selected contents. The given HTML fragment will be inserted in place of the deleted text.
        // NOTE: We cannot use Range#deleteContents because it may lead to DTD-invalid HTML. That's because it operates
        // on any DOM tree without taking care of the underlying XML syntax, (X)HTML in our case. Let's use the Delete
        // command instead which is HTML-aware. Moreover, others could listen to this command and adjust the DOM before
        // we insert the HTML.
        rta.getCommandManager().execute(Command.DELETE);
        return rta.getDocument().getSelection().getRangeAt(0);
    }
}
