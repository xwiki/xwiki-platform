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

public class UnderlineTest extends AbstractSyntaxTest
{
    public void testNotTriggeredWithWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not __ underlined __");
        expects.add("This is not __ underlined __");
        tests.add("This is not __underlined __");
        expects.add("This is not __underlined __");
        tests.add("This is not __ underlined__");
        expects.add("This is not __ underlined__");
        test(tests, expects);
    }

    public void testSimple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is __underlined__");
        expects.add("This is <em class=\"underline\">underlined</em>");
        tests.add("This is __a__ letter underlined");
        expects.add("This is <em class=\"underline\">a</em> letter underlined");
        tests.add("__a__");
        expects.add("<em class=\"underline\">a</em>");
        test(tests, expects);
    }

    public void testThree()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is __a__ short underline__");
        expects.add("This is <em class=\"underline\">a</em> short underline__");
        tests.add("This is __all __ underlined__");
        expects.add("This is <em class=\"underline\">all __ underlined</em>");
        tests.add("This is __all __underlined__");
        expects.add("This is <em class=\"underline\">all __underlined</em>");
        tests.add("This is __one__underlined__");
        expects.add("This is <em class=\"underline\">one</em>underlined__");
        test(tests, expects);
    }

    public void testMultiple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("More __underlines__ on a __line__");
        expects.add("More <em class=\"underline\">underlines</em> on a <em class=\"underline\">line</em>");
        test(tests, expects);
    }

    public void testExtraUnderscoresAreInside()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("The extra underscores are ___inside___");
        expects.add("The extra underscores are <em class=\"underline\">_inside_</em>");
        tests.add("The extra underscores are ___ inside ___");
        expects.add("The extra underscores are <em class=\"underline\">_ inside _</em>");
        test(tests, expects);
    }

    public void testWithoutWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This__is__underlined");
        expects.add("This<em class=\"underline\">is</em>underlined");
        test(tests, expects);
    }

    public void testSequence()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("__Eeny__meeny__miny__moe__");
        expects.add("<em class=\"underline\">Eeny</em>meeny<em class=\"underline\">miny</em>moe__");
        tests.add("__ Eeny__meeny__miny__moe__");
        expects.add("__ Eeny<em class=\"underline\">meeny</em>miny<em class=\"underline\">moe</em>");
        test(tests, expects);
    }

    public void testSeveralInARow()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("this is not an underline: ____ ");
        expects.add("this is not an underline: ____ ");
        tests.add("this is an underscore underlined: _____ ");
        expects.add("this is an underscore underlined: <em class=\"underline\">_</em> ");
        tests.add("this is two underscores underlined: ______ ");
        expects.add("this is two underscores underlined: <em class=\"underline\">__</em> ");
        test(tests, expects);
    }

    public void testMultiline()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not __not\nunderlined__");
        expects.add("This is not __not\nunderlined__");
        test(tests, expects);
    }

    public void testTimeComplexity()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        // Something like this should be (negatively) matched in linear time, thus it should take no
        // time. If the build takes a lot, then the regular expression is not in linear time, thus
        // wrong.
        String text = "__";
        for (int i = 0; i < 200; ++i) {
            text += "abc_";
        }
        tests.add(text);
        expects.add(text);
        text = "__";
        for (int i = 0; i < 200; ++i) {
            text += "abc _ ";
        }
        tests.add(text);
        expects.add(text);
        long startTime = System.currentTimeMillis();
        test(tests, expects);
        // Even on very slow systems this should not take more than one second. Putting 10 seconds,
        // just to be sure we don't get false test errors.
        assertTrue(System.currentTimeMillis() - startTime < 10000);
    }

    public void testLinkTargetDoesNotTriggerUnderline()
    {
        String testString = "[link>Main.WebHome>_blank] and [another link>Main.WebHome>_blank]";
        String result = this.renderer.render(testString, this.document, this.document, this.context);
        assertTrue(result.indexOf("em") == -1);
    }
}
