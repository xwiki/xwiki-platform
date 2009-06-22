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
package org.xwiki.rendering.internal.macro.velocity.filter;

import junit.framework.TestCase;

import org.apache.velocity.VelocityContext;

/**
 * Validate the behavior of {@link HTMLVelocityMacroFilter}.
 * 
 * @version $Id$
 */
public class HTMLVelocityMacroFilterTest extends TestCase
{
    private HTMLVelocityMacroFilter filter;

    private VelocityContext context;

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.filter = new HTMLVelocityMacroFilter();
        this.filter.initialize();

        this.context = new VelocityContext();
    }

    public void assertFilter(String expected, String input)
    {
        assertEquals(expected, this.filter.before(input, this.context));
    }

    public void testFilter()
    {
        assertFilter("T T", "T T");
        assertFilter("T T", "T  T");
        assertFilter("T T", "T\nT");
        assertFilter("T T", "T\n\nT");
        assertFilter("T T", "T\tT");
        assertFilter("T T", "T\t\tT");
        assertFilter("T T", "T \n\tT");

        assertFilter("T", "T  ");
        assertFilter("T", "  T");
    }

    public void testFilterSP()
    {
        assertFilter("T$spT", "T$spT");
        assertFilter("T $spT", "T  $spT");
        assertFilter("T$sp T", "T$sp  T");
        assertFilter("T $sp T", "T  $sp  T");
    }

    public void testFilterNL()
    {
        assertFilter("${nl}", "$nl");
        assertFilter("T${nl}", "T  $nl");
        assertFilter("${nl}T", "$nl  T");
        assertFilter("T${nl}T", "T  $nl  T");
        assertFilter("T${nl}", "T\n$nl");
        assertFilter("${nl}T", "$nl\n\nT");
        assertFilter("T${nl}T", "T\n\n$nl\n\nT");
        assertFilter("T${nl}T", "T\n \n $nl \n \nT");

        assertFilter("T \\$nl", "T\n\\$nl");
        assertFilter("\\$nl T", "\\$nl\n\nT");
        assertFilter("T \\$nl T", "T\n\n\\$nl\n\nT");
        assertFilter("T \\$nl T", "T\n \n \\$nl \n \nT");
    }

    public void testFilterIndent()
    {
        assertFilter("#if (true)\ntext#end", "#if (true)\n text#end");
        assertFilter("#if (true)\ntext#end", "#if (true)\n  text#end");
        assertFilter("#if (true)\ntext#end", "#if (true)\n \n text#end");
        assertFilter("#if (true)\ntext#end", "#if (true)\n \n \ttext#end");
        assertFilter("#if (true)\n#if (true)\ntext#end#end", "#if (true)\n  #if (true)\n    text#end#end");
    }

    public void testFilterComment()
    {
        assertFilter("", "##comment\n#*comment*#");
    }

    public void testFilterDirective()
    {
        assertFilter("#set()\n#if()\n#foreach()\n#end\n#elseif()\n#else\n#macro()\n#somemacro() T",
            "##comment\n#set()\n#if()\n#foreach()\n#end\n#elseif()\n#else\n#macro()\n#somemacro()\nT");
    }

    public void testFilterDirectiveSet()
    {
        assertFilter("#set()\n", "#set()\n");
        assertFilter("#set() ", "#set() ");
        assertFilter("#set()\n", "#set()\n \n ");
        assertFilter("#set(\n)\n", "#set(\n)\n");

        assertFilter("#set()\nT", "#set()\n\n \tT");
        assertFilter("T #set()\nT", "T #set()\n\n \tT");
    }

    public void testFilterDirectiveMacro()
    {
        assertFilter("#somemacro() T", "#somemacro()\nT");
        assertFilter("#somemacro() #set()\n", "#somemacro()\n#set()\n");
    }

    public void testFilterWithMSNL()
    {
        assertFilter("#set()\n", "#set()\r\n");
        assertFilter("#set()\n", "#set()\r");
    }
}
