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
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Validation rule for disabling the list features when the selection or caret is inside a header. HTML headers can
 * contain only in-line content so list are forbidden in headers anyway. The problem is that by clicking the ordered or
 * unordered list button when the selection is inside a header we get a new list containing that header. At this moment
 * xwiki/2.0 syntax doesn't support block elements inside list items. We should drop this constraint as soon as the
 * syntax for embedded documents is implemented.
 * 
 * @version $Id$
 */
public class DisableListInHeader implements ValidationRule
{
    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#areValid(SubmittableRichTextArea)
     */
    public boolean areValid(RichTextArea textArea)
    {
        String level = textArea.getCommandManager().getStringValue(Command.FORMAT_BLOCK);
        if (level != null && level.toLowerCase().startsWith("h")) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#getFeatures()
     */
    public String[] getFeatures()
    {
        return new String[] {"orderedlist", "unorderedlist"};
    }
}
