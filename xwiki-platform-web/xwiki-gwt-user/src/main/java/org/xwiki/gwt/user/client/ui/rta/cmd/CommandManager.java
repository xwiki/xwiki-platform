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
 * Interface for executing commands on a rich text area and for retrieving informations about the state of these
 * commands. This is the recommended way of altering the document edited with the rich text area because others can be
 * notified about these changes. A group of changes to the edited document, with a well defined purpose, should be
 * grouped in a command.
 * 
 * @version $Id$
 */
public interface CommandManager extends SourcesCommandEvents
{
    /**
     * Executes a command on the current selection of the document edited with the underlying rich text area.
     * 
     * @param cmd The command to execute.
     * @param param The parameter of the command.
     * @return <code>true</code> if the command is executed successfully.
     */
    boolean execute(Command cmd, String param);

    /**
     * Executes a command on the current selection of the document edited with the underlying rich text area.
     * 
     * @param cmd The command to execute.
     * @param param The parameter of the command.
     * @return <code>true</code> if the command is executed successfully.
     */
    boolean execute(Command cmd, int param);

    /**
     * Executes a command on the current selection of the document edited with the underlying rich text area.
     * 
     * @param cmd The command to execute.
     * @param param The parameter of the command.
     * @return <code>true</code> if the command is executed successfully.
     */
    boolean execute(Command cmd, boolean param);

    /**
     * Executes a command on the current selection of the document edited with the underlying rich text area.
     * 
     * @param cmd The command to execute.
     * @return <code>true</code> if the command is executed successfully.
     */
    boolean execute(Command cmd);

    /**
     * Returns a boolean value that indicates whether the specified command can be successfully executed using
     * {@link #execute(Command, String)}, given the current selection of the document edited with the underlying rich
     * text area.
     * 
     * @param cmd The command to test.
     * @return <code>true</code> if the command is enabled.
     */
    boolean isEnabled(Command cmd);

    /**
     * Returns a boolean value that indicates the current state of the given command.
     * 
     * @param cmd The command to test.
     * @return <code>true</code> if the given command has been executed on the current selection.
     */
    boolean isExecuted(Command cmd);

    /**
     * Returns a boolean value that indicates whether the current command is supported by the underlying rich text area.
     * 
     * @param cmd The command to test
     * @return <code>true</code> if the command is supported.
     */
    boolean isSupported(Command cmd);

    /**
     * Returns the value of the given command for the current selection in the document edited with the underlying rich
     * text area. In most of the cases this value is the parameter used for executing the command.
     * 
     * @param cmd The command to query.
     * @return the command value for current selection, if supported.
     */
    String getStringValue(Command cmd);

    /**
     * Returns the value of the given command for the current selection in the document edited with the underlying rich
     * text area. In most of the cases this value is the parameter used for executing the command.
     * 
     * @param cmd The command to query.
     * @return the command value for current selection, if supported.
     */
    Integer getIntegerValue(Command cmd);

    /**
     * Returns the value of the given command for the current selection in the document edited with the underlying rich
     * text area. In most of the cases this value is the parameter used for executing the command.
     * 
     * @param cmd The command to query.
     * @return the command value for current selection, if supported.
     */
    Boolean getBooleanValue(Command cmd);

    /**
     * Associates the given command with the specified executable. From now on, whenever you {@link #execute(Command)}
     * the given command the specified executable will be execute in fact.
     * 
     * @param command The command to be registered.
     * @param executable The code to be associated with the given command. This code will be execute when the command is
     *            fired.
     * @return The previous executable associated with the given command, if any, or null otherwise.
     */
    Executable registerCommand(Command command, Executable executable);

    /**
     * Unregisters the given command. As a consequence, further calls to {@link #execute(Command)} will do nothing
     * because there's no executable associated with the given command.
     * 
     * @param command The command to be executed.
     * @return The executable that was registered with the given command, if any, or null otherwise.
     */
    Executable unregisterCommand(Command command);

    /**
     * @param command A command.
     * @return The executable associated with the given command if there is one, or null otherwise.
     */
    Executable getExecutable(Command command);
}
