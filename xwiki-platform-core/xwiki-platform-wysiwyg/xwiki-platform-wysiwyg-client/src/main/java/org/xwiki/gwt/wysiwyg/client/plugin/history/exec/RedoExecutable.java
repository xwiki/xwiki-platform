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
 * Loads the next entry in the history of a rich text area.
 * 
 * @version $Id$
 */
public class RedoExecutable extends AbstractHistoryExecutable
{
    /**
     * Creates a new redo executable that uses the given history.
     * 
     * @param history the history to use
     */
    public RedoExecutable(History history)
    {
        super(history);
    }

    @Override
    public boolean execute(String param)
    {
        if (getHistory().canRedo()) {
            getHistory().redo();
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
        return getHistory().canRedo();
    }

    @Override
    public boolean isExecuted()
    {
        // Right now there's no way to test if the redo command has been executed.
        return true;
    }

    @Override
    public boolean isSupported()
    {
        return true;
    }
}
