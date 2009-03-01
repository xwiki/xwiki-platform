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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.exec;

import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroSelector;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHTMLExecutable;

/**
 * Inserts a new macro in the edited document or replaces an existing one.
 * 
 * @version $Id$
 */
public class InsertExecutable extends InsertHTMLExecutable
{
    /**
     * Used to query the currently selected macros.
     */
    private final MacroSelector selector;

    /**
     * Creates a new executable.
     * 
     * @param selector {@link #selector}
     */
    public InsertExecutable(MacroSelector selector)
    {
        this.selector = selector;
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        // We don't go through the command manager because we don't want to trigger the history mechanism.
        return super.execute(rta, "<!--" + param + "--><!--stopmacro-->")
            && rta.getCommandManager().getExecutable(MacroPlugin.REFRESH).execute(rta, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        if (selector.getMacroCount() > 0) {
            return selector.getMacro(0).getMetaData().getFirstChild().getNodeValue();
        } else {
            return null;
        }
    }
}
