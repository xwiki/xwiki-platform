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

import com.google.gwt.user.client.Element;
import com.xpn.xwiki.wysiwyg.client.history.History;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Executable;

public class RedoExecutable implements Executable
{
    private final History history;

    public RedoExecutable(History history)
    {
        this.history = history;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(Element, String)
     */
    public boolean execute(Element target, String param)
    {
        if (history.canRedo()) {
            history.redo();
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(Element)
     */
    public String getParameter(Element target)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(Element)
     */
    public boolean isEnabled(Element target)
    {
        return history.canRedo();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(Element)
     */
    public boolean isExecuted(Element target)
    {
        // Right now there's no way to test if the redo command has been executed.
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(Element)
     */
    public boolean isSupported(Element target)
    {
        return true;
    }
}
