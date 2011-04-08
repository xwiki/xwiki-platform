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

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;

/**
 * Provides the default rich text area commands.
 * 
 * @version $Id$
 */
public class DefaultCommandProvider
{
    /**
     * Provides the default commands to the given rich text area.
     * 
     * @param rta the rich text area to provide the commands to
     */
    public void provideTo(RichTextArea rta)
    {
        CommandManager cm = rta.getCommandManager();
        
        // Register default executables.
        Command[] defaultCommands = new Command[] {Command.BACK_COLOR, Command.BOLD, Command.CREATE_LINK,
            Command.FONT_NAME, Command.FONT_SIZE, Command.FORE_COLOR, Command.FORMAT_BLOCK, Command.INDENT,
            Command.INSERT_HORIZONTAL_RULE, Command.INSERT_IMAGE, Command.INSERT_ORDERED_LIST, Command.INSERT_PARAGRAPH,
            Command.INSERT_UNORDERED_LIST, Command.ITALIC, Command.JUSTIFY_CENTER, Command.JUSTIFY_FULL,
            Command.JUSTIFY_LEFT, Command.JUSTIFY_RIGHT, Command.OUTDENT, Command.REDO, Command.REMOVE_FORMAT,
            Command.STRIKE_THROUGH, Command.SUB_SCRIPT, Command.SUPER_SCRIPT, Command.TELETYPE, Command.UNDERLINE,
            Command.UNDO, Command.UNLINK};
        for (int i = 0; i < defaultCommands.length; i++) {
            cm.registerCommand(defaultCommands[i], new DefaultExecutable(rta, defaultCommands[i].toString()));
        }

        // Register custom executables.
        cm.registerCommand(Command.DELETE, new DeleteExecutable(rta));
        cm.registerCommand(Command.INSERT_HTML, new InsertHTMLExecutable(rta));
        cm.registerCommand(Command.UPDATE, new UpdateExecutable());
        cm.registerCommand(Command.ENABLE, new EnableExecutable(rta));
        cm.registerCommand(Command.RESET, new ResetExecutable(rta));
    }
}
