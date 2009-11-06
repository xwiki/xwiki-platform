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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListenerCollection;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;


/**
 * An abstract command manager that knows only how to register commands and listeners.
 * 
 * @version $Id$
 */
public abstract class AbstractCommandManager implements CommandManager
{
    /**
     * The list of listeners that will be notified whenever a command is executed by this manager.
     */
    protected final CommandListenerCollection commandListeners;

    /**
     * The map of executable known by this command manager manager.
     */
    private final Map<Command, Executable> executables;

    /**
     * Creates a new command manager that has no commands registered.
     */
    public AbstractCommandManager()
    {
        commandListeners = new CommandListenerCollection();
        executables = new HashMap<Command, Executable>();
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
     * @see CommandManager#addCommandListener(CommandListener)
     */
    public void addCommandListener(CommandListener listener)
    {
        commandListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#removeCommandListener(CommandListener)
     */
    public void removeCommandListener(CommandListener listener)
    {
        commandListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#registerCommand(Command, Executable)
     */
    public Executable registerCommand(Command command, Executable executable)
    {
        return executables.put(command, executable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#unregisterCommand(Command)
     */
    public Executable unregisterCommand(Command command)
    {
        return executables.remove(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#getExecutable(Command)
     */
    public Executable getExecutable(Command command)
    {
        return executables.get(command);
    }
}
