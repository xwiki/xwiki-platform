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
import java.util.List;

import org.xwiki.gwt.user.client.UserTestCase;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.user.client.ui.rta.cmd.LoggingCommandListener;

/**
 * Unit tests for {@link DefaultCommandManager}.
 * 
 * @version $Id$
 */
public class DefaultCommandManagerTest extends UserTestCase
{
    /**
     * The object being tested.
     */
    private DefaultCommandManager cm;

    /**
     * The list of log messages.
     */
    private final List<String> log = new ArrayList<String>();

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        cm = new DefaultCommandManager();
        cm.registerCommand(Command.UPDATE, new UpdateExecutable());
        log.clear();
    }

    /**
     * Tests that {@link CommandListener#onBeforeCommand(CommandManager, Command, String)} is fired before
     * {@link CommandListener#onCommand(CommandManager, Command, String)}.
     */
    public void testFiringOrder()
    {
        cm.addCommandListener(new LoggingCommandListener("order", log));
        assertTrue(cm.execute(Command.UPDATE, "force"));
        assertEquals(2, log.size());
        assertEquals("order before(update,force)", log.get(0));
        assertEquals("order after(update,force)", log.get(1));
    }

    /**
     * Tests that a command listener can prevent a command from being executed.
     */
    public void testPreventCommand()
    {
        cm.addCommandListener(new LoggingCommandListener("alice", log));
        cm.addCommandListener(new CommandListener()
        {
            public boolean onBeforeCommand(CommandManager sender, Command command, String param)
            {
                return true;
            }

            public void onCommand(CommandManager sender, Command command, String param)
            {
            }
        });
        cm.addCommandListener(new LoggingCommandListener("bob", log));

        assertFalse(cm.execute(Command.UPDATE));
        assertEquals(1, log.size());
        assertEquals("alice before(update,null)", log.get(0));
    }

    /**
     * Tests that a command listener can be added while a command is being executed.
     */
    public void testAddListenerWhileHandlingCommand()
    {
        cm.addCommandListener(new CommandListener()
        {
            public boolean onBeforeCommand(CommandManager sender, Command command, String param)
            {
                cm.addCommandListener(new LoggingCommandListener("carol", log));
                return false;
            }

            public void onCommand(CommandManager sender, Command command, String param)
            {
            }
        });
        assertTrue(cm.execute(Command.UPDATE, 10));
        assertEquals(1, log.size());
        assertEquals("carol after(update,10)", log.get(0));
    }
}
