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

import junit.framework.Assert;

import org.apache.velocity.VelocityContext;
import org.junit.Test;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Validate the behavior of {@link HTMLVelocityMacroFilter}.
 * 
 * @version $Id$
 */
public class HTMLVelocityMacroFilterTest extends AbstractComponentTestCase
{
    private VelocityMacroFilter filter;

    private VelocityContext context;

    @Override
    protected void registerComponents() throws Exception
    {
        this.filter = getComponentManager().getInstance(VelocityMacroFilter.class, "html");
        this.context = new VelocityContext();
    }

    public void assertFilter(String expected, String input)
    {
        Assert.assertEquals(expected, this.filter.before(input, this.context));
    }

    @Test
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

    @Test
    public void testFilterSP()
    {
        assertFilter("T$spT", "T$spT");
        assertFilter("T $spT", "T  $spT");
        assertFilter("T$sp T", "T$sp  T");
        assertFilter("T $sp T", "T  $sp  T");
    }

    @Test
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

    @Test
    public void testFilterIndent()
    {
        assertFilter("#if (true)\ntext#end", "#if (true)\n text#end");
        assertFilter("#if (true)\ntext#end", "#if (true)\n  text#end");
        assertFilter("#if (true)\ntext#end", "#if (true)\n \n text#end");
        assertFilter("#if (true)\ntext#end", "#if (true)\n \n \ttext#end");
        assertFilter("#if (true)\n#if (true)\ntext#end#end", "#if (true)\n  #if (true)\n    text#end#end");
    }

    @Test
    public void testFilterComment()
    {
        assertFilter("", "##comment\n#*comment*#");
    }

    @Test
    public void testFilterDirective()
    {
        assertFilter("#set()\n#if()\n#foreach()\n#end\n#elseif()\n#else\n#macro()\n#somemacro() T",
            "##comment\n#set()\n#if()\n#foreach()\n#end\n#elseif()\n#else\n#macro()\n#somemacro()\nT");
    }

    @Test
    public void testFilterDirectiveSet()
    {
        assertFilter("#set()\n", "#set()\n");
        assertFilter("#set() ", "#set() ");
        assertFilter("#set()\n", "#set()\n \n ");
        assertFilter("#set(\n)\n", "#set(\n)\n");

        assertFilter("#set()\nT", "#set()\n\n \tT");
        assertFilter("T #set()\nT", "T #set()\n\n \tT");

        assertFilter("#if()\ntext#end", "#if()\ntext\n#end");
    }

    @Test
    public void testFilterDirectiveMacro()
    {
        assertFilter("#somemacro() T", "#somemacro()\nT");
        assertFilter("#somemacro() #set()\nT", "#somemacro()\n#set()\nT");
        assertFilter("#somemacro()#set()\n", "#somemacro()\n#set()\n");
    }

    @Test
    public void testFilterWithMSNL()
    {
        assertFilter("#set()\n", "#set()\r\n");
        assertFilter("#set()\n", "#set()\r");
    }

    @Test
    public void testInvalidOrPartial()
    {
        assertFilter("#${escapetool.H}${declaredRight}#${escapetool.H}",
            "#${escapetool.H}${declaredRight}#${escapetool.H}");
        assertFilter("$ notvar", "$ notvar");
    }
}
