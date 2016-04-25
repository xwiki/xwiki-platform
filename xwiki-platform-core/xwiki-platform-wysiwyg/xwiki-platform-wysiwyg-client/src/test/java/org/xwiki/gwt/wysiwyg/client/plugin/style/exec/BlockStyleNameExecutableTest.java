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
package org.xwiki.gwt.wysiwyg.client.plugin.style.exec;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;
import org.xwiki.gwt.wysiwyg.client.RichTextAreaTestCase;

import com.google.gwt.user.client.Command;

/**
 * Unit tests for {@link BlockStyleNameExecutable}.
 * 
 * @version $Id$
 */
public class BlockStyleNameExecutableTest extends RichTextAreaTestCase
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        executable = new BlockStyleNameExecutable(rta);
    }

    /**
     * Tests how a block style name is applied to a paragraph.
     */
    public void testApplyStyleToParagraph()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p class=\"note\">a<em class=\"todo\">bc</em></p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getLastChild().getFirstChild(), 1);
                range.setEnd(getBody().getFirstChild().getLastChild().getFirstChild(), 2);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute("todo"));
                assertEquals("<p class=\"note todo\">a<em class=\"todo\">bc</em></p>", clean(rta.getHTML()));
            }
        });
    }

    /**
     * Tests how a block style name is removed from a paragraph.
     */
    public void testRemoveStyleFromParagraph()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p class=\"note todo\"><span class=\"note\">123</span></p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 1);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute("note"));
                assertEquals("<p class=\"todo\"><span class=\"note\">123</span></p>", clean(rta.getHTML()));
            }
        });
    }

    /**
     * Tests if applied style names are correctly detected.
     */
    public void testGetAppliedStyleNames()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<div class=\"bar\"><p class=\"note todo\"><span class=\"foo\">123</span></p></div>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild().getFirstChild(), 1);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertEquals("note todo bar", executable.getParameter());
            }
        });
    }
}
