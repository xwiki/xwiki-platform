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

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Unit tests for {@link FormatBlockExecutable}.
 * 
 * @version $Id$
 */
public class FormatBlockExecutableTest extends AbstractRichTextAreaTest
{
    /**
     * Heading level 1.
     */
    public static final String H1 = "h1";

    /**
     * The executable being tested.
     */
    private Executable executable = new FormatBlockExecutable();

    /**
     * @see http://jira.xwiki.org/jira/browse/XWIKI-2730
     */
    public void testInsertHeaderOnEmptyDocument()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestInsertHeaderOnEmptyDocument();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see http://jira.xwiki.org/jira/browse/XWIKI-2730
     */
    public void doTestInsertHeaderOnEmptyDocument()
    {
        assertTrue(executable.execute(rta, H1));
        assertTrue(new InsertHTMLExecutable().execute(rta, "Title 1"));
        assertEquals(H1, executable.getParameter(rta));
    }
}
