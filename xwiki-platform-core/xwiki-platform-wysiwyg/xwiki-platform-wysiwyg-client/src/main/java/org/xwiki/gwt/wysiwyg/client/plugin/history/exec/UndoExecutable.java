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
package org.xwiki.gwt.wysiwyg.client.plugin.history.exec;

import org.xwiki.gwt.wysiwyg.client.plugin.history.History;

/**
 * Loads the previous entry in the history of a rich text area.
 * 
 * @version $Id$
 */
public class UndoExecutable extends AbstractHistoryExecutable
{
    /**
     * Creates a new undo executable that uses the given history.
     * 
     * @param history the history to be used
     */
    public UndoExecutable(History history)
    {
        super(history);
    }

    @Override
    public boolean execute(String param)
    {
        if (getHistory().canUndo()) {
            getHistory().undo();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getParameter()
    {
        return null;
    }

    @Override
    public boolean isEnabled()
    {
        return getHistory().canUndo();
    }

    @Override
    public boolean isExecuted()
    {
        return getHistory().canRedo();
    }

    @Override
    public boolean isSupported()
    {
        return true;
    }
}
