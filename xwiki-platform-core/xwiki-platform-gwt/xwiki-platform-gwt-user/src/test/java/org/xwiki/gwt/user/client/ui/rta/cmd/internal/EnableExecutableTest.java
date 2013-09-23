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

import org.xwiki.gwt.user.client.ui.rta.RichTextAreaTestCase;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

/**
 * Unit test for {@link EnableExecutable}.
 * 
 * @version $Id$
 */
public class EnableExecutableTest extends RichTextAreaTestCase
{
    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        rta.getCommandManager().registerCommand(Command.ENABLE, new EnableExecutable(rta));
    }

    /**
     * Tests if the rich text area can be enabled or disabled.
     */
    public void testEnableDisable()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            @Override
            public void execute()
            {
                assertTrue(rta.getCommandManager().isSupported(Command.ENABLE));
                assertTrue(rta.getCommandManager().isEnabled(Command.ENABLE));

                assertTrue(rta.getCommandManager().isExecuted(Command.ENABLE));
                assertTrue(rta.getCommandManager().execute(Command.ENABLE, false));
                assertFalse(rta.getCommandManager().isExecuted(Command.ENABLE));
                assertTrue(rta.getCommandManager().execute(Command.ENABLE, true));
                assertTrue(rta.getCommandManager().isExecuted(Command.ENABLE));
            }
        });
    }
}
