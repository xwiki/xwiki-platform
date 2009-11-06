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
package org.xwiki.gwt.user.client.ui.rta.cmd;

/**
 * Interface that must be implemented in order to be notified of executed commands.
 * 
 * @version $Id$
 */
public interface CommandListener
{
    /**
     * Notifies that the given command is about to be executed with the given parameter by the specified command
     * manager.
     * 
     * @param sender The command manager that is about to execute the specified command.
     * @param command The command that will be executed.
     * @param param The parameter that will be passed to the command.
     * @return true to prevent the execution of the command, false to allow it.
     */
    boolean onBeforeCommand(CommandManager sender, Command command, String param);

    /**
     * Notifies that the given command has been executed with the given parameter by the specified command manager.
     * 
     * @param sender The command manager that executed the command.
     * @param command The command executed.
     * @param param The parameter used when the command was executed.
     */
    void onCommand(CommandManager sender, Command command, String param);
}
