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
package org.xwiki.gwt.user.client;

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

/**
 * Executes a specific command on a rich text area.
 * 
 * @version $Id$
 */
public class RichTextAreaCommand implements com.google.gwt.user.client.Command
{
    /**
     * The rich text area on which the command is executed.
     */
    private final RichTextArea rta;

    /**
     * The command executed.
     */
    private final Command command;

    /**
     * The parameter passed to the command.
     */
    private final String parameter;

    /**
     * {@code true} to focus the rich text area before executing the command, {@code false} otherwise.
     */
    private final boolean focus;

    /**
     * Creates a new GWT command that executes the specified rich text area command.
     * 
     * @param rta the rich text area
     * @param command the command to be executed
     */
    public RichTextAreaCommand(RichTextArea rta, Command command)
    {
        this(rta, command, null, true);
    }

    /**
     * Creates a new GWT command that executes the specified rich text area command.
     * 
     * @param rta the rich text area
     * @param command the command to be executed
     * @param parameter the parameter to pass to the executed command
     */
    public RichTextAreaCommand(RichTextArea rta, Command command, String parameter)
    {
        this(rta, command, parameter, true);
    }

    /**
     * Creates a new GWT command that executes the specified rich text area command.
     * 
     * @param rta the rich text area
     * @param command the command to be executed
     * @param parameter the parameter to pass to the executed command
     * @param focus {@code true} to focus the rich text area before executing the command, {@code false} otherwise
     */
    public RichTextAreaCommand(RichTextArea rta, Command command, String parameter, boolean focus)
    {
        this.rta = rta;
        this.command = command;
        this.parameter = parameter;
        this.focus = focus;
    }

    @Override
    public void execute()
    {
        if (focus) {
            // Rich text area needs to be focused in order to have a proper selection or caret.
            rta.setFocus(true);
        }
        if (rta.getCommandManager().isEnabled(command)) {
            rta.getCommandManager().execute(command, parameter);
        }
    }
}
