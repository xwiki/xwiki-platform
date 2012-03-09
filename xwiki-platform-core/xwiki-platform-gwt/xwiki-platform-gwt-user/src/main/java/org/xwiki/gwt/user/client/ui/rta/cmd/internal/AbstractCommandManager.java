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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
     * A registration that is pending to be handled because the firing depth is greater than 0. This represents a
     * {@link CommandListener} that waits to be added or removed.
     */
    private static class PendingRegistration
    {
        /**
         * The listener that is waiting to be added or removed.
         */
        private final CommandListener listener;

        /**
         * Whether the listener is waiting to be added or removed.
         */
        private final boolean add;

        /**
         * Creates a new pending registration.
         * 
         * @param listener the listener that is waiting to be handled
         * @param add {@code true} if the listener is waiting to be added, {@code false} if it is waiting to be removed
         */
        public PendingRegistration(CommandListener listener, boolean add)
        {
            this.listener = listener;
            this.add = add;
        }

        /**
         * @return {@link #listener}
         */
        public CommandListener getListener()
        {
            return listener;
        }

        /**
         * @return {@link #add}
         */
        public boolean isAdd()
        {
            return add;
        }
    }

    /**
     * The list of listeners that will be notified whenever a command is executed by this manager.
     */
    protected final CommandListenerCollection commandListeners = new CommandListenerCollection();

    /**
     * The current firing depth. Command listeners are registered right away if firing depth is 0, otherwise the
     * registration is deferred until the firing depth becomes 0.
     */
    protected int firingDepth;

    /**
     * The map of executable known by this command manager manager.
     */
    private final Map<Command, Executable> executables = new HashMap<Command, Executable>();

    /**
     * The list of command listeners that are waiting to be handled because the firing depth is greater than 0.
     */
    private final List<PendingRegistration> pendingRegistrations = new ArrayList<PendingRegistration>();

    @Override
    public boolean execute(Command cmd, int param)
    {
        return execute(cmd, String.valueOf(param));
    }

    @Override
    public boolean execute(Command cmd, boolean param)
    {
        return execute(cmd, String.valueOf(param));
    }

    @Override
    public boolean execute(Command cmd)
    {
        return execute(cmd, (String) null);
    }

    @Override
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

    @Override
    public Boolean getBooleanValue(Command cmd)
    {
        String sValue = getStringValue(cmd);
        if (sValue != null) {
            return Boolean.valueOf(sValue);
        } else {
            return null;
        }
    }

    @Override
    public void addCommandListener(CommandListener listener)
    {
        if (firingDepth == 0) {
            commandListeners.add(listener);
        } else {
            pendingRegistrations.add(new PendingRegistration(listener, true));
        }
    }

    @Override
    public void removeCommandListener(CommandListener listener)
    {
        if (firingDepth == 0) {
            commandListeners.remove(listener);
        } else {
            pendingRegistrations.add(new PendingRegistration(listener, false));
        }
    }

    /**
     * Handle pending registrations.
     */
    protected void handlePendingRegistrations()
    {
        if (firingDepth == 0) {
            for (PendingRegistration pendingRegistration : pendingRegistrations) {
                if (pendingRegistration.isAdd()) {
                    commandListeners.add(pendingRegistration.getListener());
                } else {
                    commandListeners.remove(pendingRegistration.getListener());
                }
            }
            pendingRegistrations.clear();
        }
    }

    @Override
    public Executable registerCommand(Command command, Executable executable)
    {
        return executables.put(command, executable);
    }

    @Override
    public Executable unregisterCommand(Command command)
    {
        return executables.remove(command);
    }

    @Override
    public Executable getExecutable(Command command)
    {
        return executables.get(command);
    }
}
