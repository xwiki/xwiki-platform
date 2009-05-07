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
package com.xpn.xwiki.wysiwyg.client.plugin.submit.exec;

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Unit test for {@link EnableExecutable}.
 * 
 * @version $Id$
 */
public class EnableExecutableTest extends AbstractRichTextAreaTest
{
    /**
     * The command used to enable or disable the rich text area.
     */
    private static final Command ENABLE = new Command("enable");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRichTextAreaTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        rta.getCommandManager().registerCommand(ENABLE, new EnableExecutable());
    }

    /**
     * Tests if the rich text area can be enabled or disabled.
     */
    public void testEnableDisable()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestEnableDisable();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the rich text area can be enabled or disabled.
     */
    private void doTestEnableDisable()
    {
        assertTrue(rta.getCommandManager().isSupported(ENABLE));
        assertTrue(rta.getCommandManager().isEnabled(ENABLE));

        assertTrue(rta.getCommandManager().isExecuted(ENABLE));
        assertTrue(rta.getCommandManager().execute(ENABLE, false));
        assertFalse(rta.getCommandManager().isExecuted(ENABLE));
        assertTrue(rta.getCommandManager().execute(ENABLE, true));
        assertTrue(rta.getCommandManager().isExecuted(ENABLE));
    }
}
