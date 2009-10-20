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

public class BoldTest extends AbstractSyntaxTest
{
    public void testNotTriggeredWithWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not * bold *");
        expects.add("This is not * bold *");
        tests.add("This is not *bold *");
        expects.add("This is not *bold *");
        tests.add("This is not * bold*");
        expects.add("This is not * bold*");
        test(tests, expects);
    }

    public void testSimple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is *bold*");
        expects.add("This is <strong>bold</strong>");
        tests.add("This is *a* bold letter");
        expects.add("This is <strong>a</strong> bold letter");
        tests.add("*a*");
        expects.add("<strong>a</strong>");
        test(tests, expects);
    }

    public void testThree()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is *a* short bold*");
        expects.add("This is <strong>a</strong> short bold*");
        tests.add("This is *all * bold*");
        expects.add("This is <strong>all * bold</strong>");
        tests.add("This is *all *bold*");
        expects.add("This is <strong>all *bold</strong>");
        tests.add("This is *one*bold*");
        expects.add("This is <strong>one</strong>bold*");
        test(tests, expects);
    }

    public void testMultiple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("More *bolds* on a *line*");
        expects.add("More <strong>bolds</strong> on a <strong>line</strong>");
        test(tests, expects);
    }

    public void testExtraStarsAreInside()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("The extra stars are **inside**");
        expects.add("The extra stars are <strong>*inside*</strong>");
        tests.add("The extra stars are ** inside **");
        expects.add("The extra stars are <strong>* inside *</strong>");
        test(tests, expects);
    }

    public void testWithoutWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This*is*bold");
        expects.add("This<strong>is</strong>bold");
        test(tests, expects);
    }

    public void testSequence()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("*Eeny*meeny*miny*moe*");
        expects.add("<strong>Eeny</strong>meeny<strong>miny</strong>moe*");
        tests.add("* Eeny*meeny*miny*moe*");
        expects.add("...<li>Eeny<strong>meeny</strong>miny<strong>moe</strong></li>...");
        test(tests, expects);
    }

    public void testWithLists()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("* this is a list item*");
        expects.add("...<li>this is a list item*</li>...");
        tests.add("* this is a list *item*");
        expects.add("...<li>this is a list <strong>item</strong></li>...");
        test(tests, expects);
    }

    public void testSeveralInARow()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("this is not bold: ** ");
        expects.add("this is not bold: ** ");
        tests.add("this is a bold star: *** ");
        expects.add("this is a bold star: <strong>*</strong> ");
        tests.add("this is two bold stars: **** ");
        expects.add("this is two bold stars: <strong>**</strong> ");
        test(tests, expects);
    }

    public void testMultiline()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not *not\nbold*");
        expects.add("This is not *not\nbold*");
        test(tests, expects);
    }

    public void testTimeComplexity()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        // Something like this should be (negatively) matched in linear time, thus it should take no
        // time. If the build takes a lot, then the regular expression is not in linear time, thus
        // wrong.
        String text = "*";
        for (int i = 0; i < 200; ++i) {
            text += "abc *";
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
