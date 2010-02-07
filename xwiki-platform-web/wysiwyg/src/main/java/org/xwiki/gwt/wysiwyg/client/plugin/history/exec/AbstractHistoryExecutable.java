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

import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;
import org.xwiki.gwt.wysiwyg.client.plugin.history.History;


/**
 * Abstract history executable.
 * 
 * @version $Id$
 */
public abstract class AbstractHistoryExecutable implements Executable
{
    /**
     * The history to be used by all concrete implementations.
     */
    private final History history;

    /**
     * Creates a new history executable that will be using the given history.
     * 
     * @param history the history to use
     */
    public AbstractHistoryExecutable(History history)
    {
        this.history = history;
    }

    /**
     * @return the history used by this executable
     */
    protected History getHistory()
    {
        return history;
    }
}
