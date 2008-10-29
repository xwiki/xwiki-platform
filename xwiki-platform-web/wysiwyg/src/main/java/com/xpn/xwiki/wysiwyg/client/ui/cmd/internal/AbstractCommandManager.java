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

import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandListenerCollection;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.SourcesCommandEvents;

public abstract class AbstractCommandManager implements CommandManager
{
    protected final CommandListenerCollection commandListeners;

    public AbstractCommandManager()
    {
        commandListeners = new CommandListenerCollection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execute(Command, int)
     */
    public boolean execute(Command cmd, int param)
    {
        return execute(cmd, String.valueOf(param));
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execute(Command, boolean)
     */
    public boolean execute(Command cmd, boolean param)
    {
        return execute(cmd, String.valueOf(param));
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execute(Command)
     */
    public boolean execute(Command cmd)
    {
        return execute(cmd, (String) null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#getIntegerValue(Command)
     */
    public Integer getIntegerValue(Command cmd)
    {
        String sValue = getStringValue(cmd);
        if (sValue != null) {
            try {
                return Integer.valueOf(sValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#getBooleanValue(Command)
     */
    public Boolean getBooleanValue(Command cmd)
    {
        String sValue = getStringValue(cmd);
        if (sValue != null) {
            return Boolean.valueOf(sValue);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesCommandEvents#addCommandListener(CommandListener)
     */
    public void addCommandListener(CommandListener listener)
    {
        commandListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesCommandEvents#removeCommandListener(CommandListener)
     */
    public void removeCommandListener(CommandListener listener)
    {
        commandListeners.remove(listener);
    }
}
