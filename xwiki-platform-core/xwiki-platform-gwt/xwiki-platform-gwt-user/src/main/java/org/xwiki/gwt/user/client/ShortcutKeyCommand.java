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

import com.google.gwt.user.client.Command;

/**
 * A {@link Command} that can be triggered by a shortcut key.
 * 
 * @version $Id$
 */
public class ShortcutKeyCommand implements Command
{
    /**
     * The underlying command that is triggered by the associated shortcut key.
     */
    private final Command command;

    /**
     * Flag indicating if this command can be repeated while the shortcut key is held down.
     */
    private final boolean repeatable;

    /**
     * Creates a new shortcut key command by wrapping the given command.
     * 
     * @param command the command to be wrapped
     */
    public ShortcutKeyCommand(Command command)
    {
        this(command, false);
    }

    /**
     * Creates a new shortcut key command by wrapping the given command.
     * 
     * @param command the command to be wrapped
     * @param repeatable {@code true} if the given command can be executed multiple times while the shortcut key is held
     *            down, {@code false} otherwise
     */
    public ShortcutKeyCommand(Command command, boolean repeatable)
    {
        this.command = command;
        this.repeatable = repeatable;
    }

    @Override
    public void execute()
    {
        command.execute();
    }

    /**
     * @return {@code true} is this command can be executed while the shortcut key is held down, {@code false} otherwise
     */
    public boolean isRepeatable()
    {
        return repeatable;
    }

    /**
     * @return the underlying command triggered by the associated shortcut key
     */
    public Command getCommand()
    {
        return command;
    }
}
