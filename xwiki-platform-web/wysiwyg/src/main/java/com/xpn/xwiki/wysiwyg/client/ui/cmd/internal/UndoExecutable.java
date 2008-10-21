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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import com.xpn.xwiki.wysiwyg.client.history.History;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Executable;
import com.xpn.xwiki.wysiwyg.client.util.Document;

public class UndoExecutable implements Executable
{
    private final History history;

    public UndoExecutable(History history)
    {
        this.history = history;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(Document, String)
     */
    public boolean execute(Document doc, String param)
    {
        if (history.canUndo()) {
            history.undo();
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(Document)
     */
    public String getParameter(Document doc)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(Document)
     */
    public boolean isEnabled(Document doc)
    {
        return history.canUndo();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(Document)
     */
    public boolean isExecuted(Document doc)
    {
        return history.canRedo();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(Document)
     */
    public boolean isSupported(Document doc)
    {
        return true;
    }
}
