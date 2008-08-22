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
package com.xpn.xwiki.wysiwyg.client.syntax.rule;

import com.xpn.xwiki.wysiwyg.client.syntax.ValidationRule;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

/**
 * We need a distinct rule for the background color feature because it has different implementations on different
 * browsers.
 */
public class BackColorRule implements ValidationRule
{
    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#areValid(XRichTextArea)
     */
    public boolean areValid(XRichTextArea textArea)
    {
        // On some browsers the BACK_COLOR command sets the background color of the whole text area. On the other hand,
        // not all the browsers support the HILITE_COLOR command.
        if (textArea.getCommandManager().queryCommandSupported(Command.HILITE_COLOR)) {
            return textArea.getCommandManager().queryCommandEnabled(Command.HILITE_COLOR);
        } else {
            return textArea.getCommandManager().queryCommandEnabled(Command.BACK_COLOR);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#getFeatures()
     */
    public String[] getFeatures()
    {
        return new String[] {"backcolor"};
    }
}
