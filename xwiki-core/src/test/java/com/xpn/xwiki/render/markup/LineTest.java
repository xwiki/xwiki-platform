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
 *
 */
package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

public class LineTest extends AbstractSyntaxTest
{
    public void testNotTriggeredWithLess()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not -- a line");
        expects.add("This is not -- a line");
        tests.add("This is not --- a line");
        expects.add("This is not --- a line");
        tests.add("This is not a----line");
        expects.add("This is not a----line");
        tests.add("This is not a ----line");
        expects.add("This is not a ----line");
        tests.add("This is not a---- line");
        expects.add("This is not a---- line");
        tests.add("This is not a line----");
        expects.add("This is not a line----");
        tests.add("----This is not a line");
        expects.add("----This is not a line");
        test(tests, expects);
    }

    public void testStrikethroughHasPriority()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is ----stroked----");
        expects.add("This is <del>--stroked--</del>");
        test(tests, expects);
    }

    public void testSimple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is a ---- line");
        expects.add("This is a <hr/> line");
        tests.add("This is a line ----");
        expects.add("This is a line <hr/>");
        test(tests, expects);
    }

    public void testLongerLines()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is a ----- line");
        expects.add("This is a <hr/> line");
        tests.add("This is a --------- long line");
        expects.add("This is a <hr/> long line");
        tests.add("This is a ------------ really long line");
        expects.add("This is a <hr/> really long line");
        test(tests, expects);
    }

    public void testAloneOnALine()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("----");
        expects.add("<hr/>");
        tests.add("  ----");
        expects.add("  <hr/>");
        tests.add("----   ");
        expects.add("<hr/>   ");
        tests.add("   ----  ");
        expects.add("   <hr/>  ");
        test(tests, expects);
    }

    public void testListHasPriority()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("---- this is a list item");
        expects.add("...<li>this is a list item</li>...");
        test(tests, expects);
    }

    public void testLineAfterList()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("* this is a list item\n----");
        expects.add("...<hr/>...");
        test(tests, expects);
    }

    /*
     * public void testSequence() { ArrayList<String> tests = new ArrayList<String>(); ArrayList<String> expects =
     * new ArrayList<String>(); tests.add("---- ------ ----"); expects.add("<hr/> <hr/> <hr/>"); test(tests, expects); }
     */

    public void testXmlComments()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("Ignore <!----> comments");
        expects.add("Ignore <!----> comments");
        test(tests, expects);
    }
}
