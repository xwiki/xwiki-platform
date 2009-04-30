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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Default command manager.
 * 
 * @version $Id$
 */
public class DefaultCommandManager extends AbstractCommandManager
{
    /**
     * The map of predefined executable provided by the rich text area.
     */
    public static final Map<Command, Executable> EXECUTABLES;

    /**
     * The underlying rich text area on which this command manager operates. All the commands are execute on this rich
     * text area.
     */
    private final RichTextArea rta;

    static {
        Command[] defaultCommands =
            new Command[] {Command.BACK_COLOR, Command.BOLD, Command.CREATE_LINK, Command.DELETE, Command.FONT_NAME,
                Command.FONT_SIZE, Command.FORE_COLOR, Command.FORMAT_BLOCK, Command.INDENT,
                Command.INSERT_HORIZONTAL_RULE, Command.INSERT_IMAGE, Command.INSERT_ORDERED_LIST,
                Command.INSERT_PARAGRAPH, Command.INSERT_UNORDERED_LIST, Command.ITALIC, Command.JUSTIFY_CENTER,
                Command.JUSTIFY_FULL, Command.JUSTIFY_LEFT, Command.JUSTIFY_RIGHT, Command.OUTDENT, Command.REDO,
                Command.REMOVE_FORMAT, Command.STRIKE_THROUGH, Command.SUB_SCRIPT, Command.SUPER_SCRIPT,
                Command.TELETYPE, Command.UNDERLINE, Command.UNDO, Command.UNLINK};
        EXECUTABLES = new HashMap<Command, Executable>();
        for (int i = 0; i < defaultCommands.length; i++) {
            EXECUTABLES.put(defaultCommands[i], new DefaultExecutable(defaultCommands[i].toString()));
        }

        // Register custom executables.
        EXECUTABLES.put(Command.INSERT_HTML, new InsertHTMLExecutable());
    }

    /**
     * Creates a new command manager for the given rich text area.
     * 
     * @param rta The rich text area on which this manager will operate.
     */
    public DefaultCommandManager(RichTextArea rta)
    {
        this(rta, EXECUTABLES);
    }

    /**
     * Creates a new command manager for the given rich text area, initializing its executables map with the one
     * specified.
     * 
     * @param rta The rich text area on which this manager will operate.
     * @param executables The initial executables this manager will know.
     */
    public DefaultCommandManager(RichTextArea rta, Map<Command, Executable> executables)
    {
        this.rta = rta;
        for (Map.Entry<Command, Executable> entry : executables.entrySet()) {
            registerCommand(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#execute(Command, String)
     */
    public boolean execute(Command cmd, String param)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        if (commandListeners.fireBeforeCommand(this, cmd, param)) {
            return false;
        }
        boolean success = executable.execute(rta, param);
        if (success) {
            commandListeners.fireCommand(this, cmd, param);
        }
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#isEnabled(Command)
     */
    public boolean isEnabled(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        return executable.isEnabled(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#isExecuted(Command)
     */
    public boolean isExecuted(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        return executable.isExecuted(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#isSupported(Command)
     */
    public boolean isSupported(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        return executable.isSupported(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#getStringValue(Command)
     */
    public String getStringValue(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return null;
        }
        return executable.getParameter(rta);
    }
}
