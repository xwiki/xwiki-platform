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

import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

/**
 * Default command manager.
 * 
 * @version $Id$
 */
public class DefaultCommandManager extends AbstractCommandManager
{
    @Override
    public boolean execute(Command cmd, String param)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }

        firingDepth++;
        boolean stop = commandListeners.fireBeforeCommand(this, cmd, param);
        firingDepth--;
        handlePendingRegistrations();
        if (stop) {
            return false;
        }

        boolean success = executable.execute(param);
        if (success) {
            firingDepth++;
            commandListeners.fireCommand(this, cmd, param);
            firingDepth--;
            handlePendingRegistrations();
        }
        return success;
    }

    @Override
    public boolean isEnabled(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        return executable.isEnabled();
    }

    @Override
    public boolean isExecuted(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        return executable.isExecuted();
    }

    @Override
    public boolean isSupported(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        return executable.isSupported();
    }

    @Override
    public String getStringValue(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return null;
        }
        return executable.getParameter();
    }
}
