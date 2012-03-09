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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.UserTestCase;

/**
 * Unit tests for {@link CommandListenerCollection}.
 * 
 * @version $Id$
 */
public class CommandListenerCollectionTest extends UserTestCase
{
    /**
     * The object being tested.
     */
    private final CommandListenerCollection listeners = new CommandListenerCollection();

    /**
     * The list of log messages.
     */
    private final List<String> log = new ArrayList<String>();

    @Override
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();

        listeners.clear();
        log.clear();
    }

    /**
     * Tests that listeners are notified in the order they have been registered.
     */
    public void testFiringOrder()
    {
        listeners.add(new LoggingCommandListener("one", log));
        listeners.add(new LoggingCommandListener("two", log));

        assertFalse(listeners.fireBeforeCommand(null, Command.ENABLE, String.valueOf(true)));
        listeners.fireCommand(null, Command.ENABLE, String.valueOf(true));

        assertEquals(4, log.size());
        assertEquals("one before(enable,true)", log.get(0));
        assertEquals("two before(enable,true)", log.get(1));
        assertEquals("one after(enable,true)", log.get(2));
        assertEquals("two after(enable,true)", log.get(3));
    }

    /**
     * Tests that exceptions thrown inside a command listener are caught.
     */
    public void testExceptionsAreCaught()
    {
        listeners.add(new CommandListener()
        {
            public boolean onBeforeCommand(CommandManager sender, Command command, String param)
            {
                throw new RuntimeException();
            }

            public void onCommand(CommandManager sender, Command command, String param)
            {
                throw new RuntimeException();
            }
        });
        listeners.add(new LoggingCommandListener("alice", log));

        assertFalse(listeners.fireBeforeCommand(null, Command.BOLD, null));
        listeners.fireCommand(null, Command.BOLD, null);

        assertEquals(2, log.size());
        assertEquals("alice before(bold,null)", log.get(0));
        assertEquals("alice after(bold,null)", log.get(1));
    }

    /**
     * Tests that a command listener can prevent a command.
     */
    public void testPreventCommand()
    {
        listeners.add(new LoggingCommandListener("bob", log));
        listeners.add(new CommandListener()
        {
            public boolean onBeforeCommand(CommandManager sender, Command command, String param)
            {
                return true;
            }

            public void onCommand(CommandManager sender, Command command, String param)
            {
            }
        });
        listeners.add(new LoggingCommandListener("carol", log));

        assertTrue(listeners.fireBeforeCommand(null, Command.DELETE, null));

        assertEquals(1, log.size());
        assertEquals("bob before(delete,null)", log.get(0));
    }
}
