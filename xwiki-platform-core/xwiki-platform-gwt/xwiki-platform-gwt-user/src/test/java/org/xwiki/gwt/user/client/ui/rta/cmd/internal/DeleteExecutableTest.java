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

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextAreaTestCase;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

/**
 * Unit tests for {@link DeleteExecutable}.
 * 
 * @version $Id$
 */
public class DeleteExecutableTest extends RichTextAreaTestCase
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (executable == null) {
            executable = rta.getCommandManager().getExecutable(Command.DELETE);
        }
    }

    /**
     * Selects a button and deletes it.
     */
    public void testDeleteButton()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("<p>before<button>text</button>after</p>");

                Range range = rta.getDocument().createRange();
                range.selectNode(getBody().getFirstChild().getChildNodes().getItem(1));
                select(range);

                assertTrue(executable.execute(null));

                range = rta.getDocument().getSelection().getRangeAt(0);
                assertTrue(range.isCollapsed());
                range.insertNode(rta.getDocument().createTextNode("#"));
                assertEquals("<p>before#after</p>", clean(rta.getHTML()));
            }
        });
    }
}
