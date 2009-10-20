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

public class StrikethroughTest extends AbstractSyntaxTest
{
    public void testNotTriggeredWithWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not -- stroked --");
        expects.add("This is not -- stroked --");
        tests.add("This is not --stroked --");
        expects.add("This is not --stroked --");
        tests.add("This is not -- stroked--");
        expects.add("This is not -- stroked--");
        test(tests, expects);
    }

    public void testSimple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is --stroked--");
        expects.add("This is <del>stroked</del>");
        tests.add("This is --a-- stroke");
        expects.add("This is <del>a</del> stroke");
        tests.add("--a--");
        expects.add("<del>a</del>");
        test(tests, expects);
    }

    public void testThree()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is --a-- short stroke--");
        expects.add("This is <del>a</del> short stroke--");
        tests.add("This is --all -- stroked--");
        expects.add("This is <del>all -- stroked</del>");
        tests.add("This is --all --stroked--");
        expects.add("This is <del>all --stroked</del>");
        tests.add("This is --one--stroked--");
        expects.add("This is <del>one</del>stroked--");
        test(tests, expects);
    }

    public void testMultiple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("More --strokes-- on a --line--");
        expects.add("More <del>strokes</del> on a <del>line</del>");
        test(tests, expects);
    }

    public void testExtraDashesAreInside()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("The extra dashes are ---inside---");
        expects.add("The extra dashes are <del>-inside-</del>");
        tests.add("The extra dashes are --- inside ---");
        expects.add("The extra dashes are <del>- inside -</del>");
        test(tests, expects);
    }

    public void testWithoutWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This--is--stroked");
        expects.add("This<del>is</del>stroked");
        test(tests, expects);
    }

    public void testSequence()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("--Eeny--meeny--miny--moe--");
        expects.add("<del>Eeny</del>meeny<del>miny</del>moe--");
        tests.add("-- Eeny--meeny--miny--moe--");
        expects.add("...<li>Eeny<del>meeny</del>miny<del>moe</del></li>...");
        test(tests, expects);
    }

    public void testHR()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("this is a ------ line");
        expects.add("this is a <hr/> line");
        test(tests, expects);
    }

    public void testWithLists()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("-- this is a list item--");
        expects.add("...<li>this is a list item--</li>...");
        tests.add("-- this is a list --item--");
        expects.add("...<li>this is a list <del>item</del></li>...");
        test(tests, expects);
    }

    public void testHtmlComments()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("HTML <!-- comments are ignored -->");
        expects.add("HTML <!-- comments are ignored -->");
        tests.add("Multiple HTML <!-- comments --> are <!-- ignored -->");
        expects.add("Multiple HTML <!-- comments --> are <!-- ignored -->");
        test(tests, expects);
    }

    public void testMultiline()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not --not\nstroked--");
        expects.add("This is not --not\nstroked--");
        test(tests, expects);
    }

    public void testTimeComplexity()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        // Something like this should be (negatively) matched in linear time, thus it should take no
        // time. If the build takes a lot, then the regular expression is not in linear time, thus
        // wrong.
        String text = "--";
        for (int i = 0; i < 200; ++i) {
            text += "abc-";
        }
        tests.add(text);
        expects.add(text);
        long startTime = System.currentTimeMillis();
        test(tests, expects);
        // Even on very slow systems this should not take more than one second. Putting 10 seconds,
        // just to be sure we don't get false test errors.
        assertTrue(System.currentTimeMillis() - startTime < 10000);
    }
}
