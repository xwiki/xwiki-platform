package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

public class ItalicsTest extends SyntaxTestsParent
{
    protected void setUp()
    {
        super.setUp();
    }

    public void testNotTriggeredWithWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not ~~ italics ~~");
        expects.add("This is not ~~ italics ~~");
        tests.add("This is not ~~italics ~~");
        expects.add("This is not ~~italics ~~");
        tests.add("This is not ~~ italics~~");
        expects.add("This is not ~~ italics~~");
        test(tests, expects);
    }

    public void testSimple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is in ~~italics~~");
        expects.add("This is in <em>italics</em>");
        tests.add("This is ~~a~~ letter in italics");
        expects.add("This is <em>a</em> letter in italics");
        tests.add("~~a~~");
        expects.add("<em>a</em>");
        test(tests, expects);
    }

    public void testThree()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is ~~a~~ short italics~~");
        expects.add("This is <em>a</em> short italics~~");
        tests.add("This is ~~all ~~ italics~~");
        expects.add("This is <em>all ~~ italics</em>");
        tests.add("This is ~~all ~~italics~~");
        expects.add("This is <em>all ~~italics</em>");
        tests.add("This is ~~one~~italics~~");
        expects.add("This is <em>one</em>italics~~");
        test(tests, expects);
    }

    public void testMultiple()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("More ~~italics~~ on a ~~line~~");
        expects
            .add("More <em>italics</em> on a <em>line</em>");
        test(tests, expects);
    }

    public void testExtraTildeAreInside()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("The extra tilde are ~~~inside~~~");
        expects.add("The extra tilde are <em>~inside~</em>");
        tests.add("The extra tilde are ~~~ inside ~~~");
        expects.add("The extra tilde are <em>~ inside ~</em>");
        test(tests, expects);
    }

    public void testWithoutWhitespace()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This~~is~~italics");
        expects.add("This<em>is</em>italics");
        test(tests, expects);
    }

    public void testSequence()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("~~Eeny~~meeny~~miny~~moe~~");
        expects
            .add("<em>Eeny</em>meeny<em>miny</em>moe~~");
        tests.add("~~ Eeny~~meeny~~miny~~moe~~");
        expects
            .add("~~ Eeny<em>meeny</em>miny<em>moe</em>");
        test(tests, expects);
    }

    public void testSeveralInARow()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("this is not in italics: ~~~~ ");
        expects.add("this is not in italics: ~~~~ ");
        tests.add("this is a tilde in italics: ~~~~~ ");
        expects.add("this is a tilde in italics: <em>~</em> ");
        tests.add("this is two tilde in italics: ~~~~~~ ");
        expects.add("this is two tilde in italics: <em>~~</em> ");
        test(tests, expects);
    }

    public void testMultiline()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("This is not ~~not\nitalics~~");
        expects.add("This is not ~~not\nitalics~~");
        test(tests, expects);
    }

    public void testTimeComplexity()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        // Something like this should be (negatively) matched in linear time, thus it should take no
        // time. If the build takes a lot, then the regular expression is not in linear time, thus
        // wrong.
        String text = "~~";
        for (int i = 0; i < 1000; ++i) {
            text += "abc~";
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
