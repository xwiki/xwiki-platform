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
package com.xpn.xwiki.wysiwyg.client.ui.cmd;

public interface CommandManager extends SourcesCommandEvents
{
    /**
     * Executes a command on the current document, current selection, or the given range.
     * 
     * @param cmd The command to execute.
     * @param param Variant that specifies the string, number, or other value to assign. Possible values depend on the
     *            command.
     * @return <code>true</code> if the command is successful.
     */
    boolean execCommand(Command cmd, String param);

    /**
     * @see #execCommand(Command, String)
     */
    boolean execCommand(Command cmd, int param);

    /**
     * @see #execCommand(Command, String)
     */
    boolean execCommand(Command cmd, boolean param);

    /**
     * Executes a command on the current document, current selection, or the given range.
     * 
     * @param cmd The command to execute.
     * @return <code>true</code> if the command is successful.
     */
    boolean execCommand(Command cmd);

    /**
     * Returns a boolean value that indicates whether a specified command can be successfully executed using
     * {@link #execCommand(Command, String)}, given the current state of the document.
     * 
     * @param cmd The command to test.
     * @return <code>true</code> if the command is enabled.
     */
    boolean queryCommandEnabled(Command cmd);

    /**
     * Returns a boolean value that indicates whether the specified command is in the indeterminate state.
     * 
     * @param cmd The command to test
     * @return <code>true</code> if the command is in the indeterminate state.
     */
    boolean queryCommandIndeterm(Command cmd);

    /**
     * Returns a boolean value that indicates the current state of the command.
     * 
     * @param cmd The command to test.
     * @return <code>true</code> if the given command has been executed on the object.
     */
    boolean queryCommandState(Command cmd);

    /**
     * Returns a boolean value that indicates whether the current command is supported on the current range.
     * 
     * @param cmd The command to test
     * @return <code>true</code> if the command is supported.
     */
    boolean queryCommandSupported(Command cmd);

    /**
     * Returns the current value of the document, range, or current selection for the given command.
     * 
     * @param cmd The command to query.
     * @return the command value for the document, range, or current selection, if supported. Possible values depend on
     *         the given command.
     */
    String queryCommandStringValue(Command cmd);

    /**
     * @see #queryCommandStringValue(Command)
     */
    Integer queryCommandIntegerValue(Command cmd);

    /**
     * @see #queryCommandStringValue(Command)
     */
    Boolean queryCommandBooleanValue(Command cmd);
}
