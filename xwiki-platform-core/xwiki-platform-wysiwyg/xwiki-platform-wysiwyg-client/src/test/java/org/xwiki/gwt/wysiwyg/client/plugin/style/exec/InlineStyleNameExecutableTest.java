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
 * Unit tests for {@link InlineStyleNameExecutable}.
 * 
 * @version $Id$
 */
public class InlineStyleNameExecutableTest extends RichTextAreaTestCase
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    /**
     * {@inheritDoc}
     * 
     * @see RichTextAreaTestCase#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        executable = new InlineStyleNameExecutable(rta);
    }

    /**
     * Tests how an in-line style name is applied to the current text selection.
     */
    public void testApplyStyleToSelectedText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p class=\"todo\">a<em class=\"note\">bc</em></p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getLastChild().getFirstChild(), 1);
                range.setEnd(getBody().getFirstChild().getLastChild().getFirstChild(), 2);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute("todo"));
                assertEquals("<p class=\"todo\">a<em class=\"note\">b<span " + "class=\"todo\">c</span></em></p>",
                    clean(rta.getHTML()));
            }
        });
    }

    /**
     * Tests how an in-line style name is removed from the current text selection.
     */
    public void testRemoveStyleFromSelectedText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p class=\"note\"><span class=\"note todo\">123</span></p>");

                Range range = rta.getDocument().createRange();
                range.selectNode(getBody().getFirstChild().getFirstChild().getFirstChild());
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute("note"));
                assertEquals("<p class=\"note\"><span class=\"todo\">123</span></p>", clean(rta.getHTML()));
            }
        });
    }

    /**
     * Tests if applied in-line style names are correctly detected.
     */
    public void testGetAppliedStyleNames()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p class=\"foo\"><span class=\"bar\">1<em class=\"note todo\">23</em></span></p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getLastChild().getFirstChild(), 1);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertEquals("note todo bar", executable.getParameter());
            }
        });
    }
}
