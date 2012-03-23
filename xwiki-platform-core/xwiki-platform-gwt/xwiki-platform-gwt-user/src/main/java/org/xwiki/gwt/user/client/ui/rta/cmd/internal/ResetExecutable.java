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

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

/**
 * Resets the content of the rich text area. This executable should be used, instead of setting the content of the rich
 * text area directly, in order to let command listeners to be notified and adjust the content. There is a difference
 * between listening to the command associated with this executable and listening to inner HTML changes on the rich text
 * area's document body. The later implies that the new content was generated on the client (like an undo operation),
 * while the reset executable implies the new content comes from the server and is more like the initial content of the
 * rich text area.
 * 
 * @version $Id$
 */
public class ResetExecutable extends AbstractRichTextAreaExecutable
{
    /**
     * Creates a new executable that can reset the HTML of the specified rich text area.
     * 
     * @param rta the execution target
     */
    public ResetExecutable(RichTextArea rta)
    {
        super(rta);
    }

    @Override
    public boolean execute(String parameter)
    {
        if (parameter != null) {
            rta.setHTML(parameter);
        } else {
            // The content of the rich text area was changed without calling {@link RichTextArea#setHTML(String)} (e.g.
            // the rich text area was reloaded) and we must notify the inner HTML listeners as though it was called.
            rta.getDocument().fireInnerHTMLChange((Element) rta.getDocument().getDocumentElement());
        }
        return true;
    }

    @Override
    public String getParameter()
    {
        return rta.getHTML();
    }

    @Override
    public boolean isEnabled()
    {
        return isSupported();
    }

    @Override
    public boolean isExecuted()
    {
        return rta.isEnabled();
    }

    @Override
    public boolean isSupported()
    {
        return rta != null;
    }
}
