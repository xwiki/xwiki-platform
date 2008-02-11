package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

public class BoldTest extends SyntaxTestsParent
{
    protected void setUp()
    {
        super.setUp();
    }

    public void testNotTriggeredWithWhitespace()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
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
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
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
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
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
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("More *bolds* on a *line*");
        expects
            .add("More <strong>bolds</strong> on a <strong>line</strong>");
        test(tests, expects);
    }

    public void testExtraStarsAreInside()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("The extra stars are **inside**");
        expects.add("The extra stars are <strong>*inside*</strong>");
        tests.add("The extra stars are ** inside **");
        expects.add("The extra stars are <strong>* inside *</strong>");
        test(tests, expects);
    }

    public void testWithoutWhitespace()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("This*is*bold");
        expects.add("This<strong>is</strong>bold");
        test(tests, expects);
    }

    public void testSequence()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("*Eeny*meeny*miny*moe*");
        expects
            .add("<strong>Eeny</strong>meeny<strong>miny</strong>moe*");
        tests.add("* Eeny*meeny*miny*moe*");
        expects
            .add("...<li>Eeny<strong>meeny</strong>miny<strong>moe</strong></li>...");
        test(tests, expects);
    }

    public void testWithLists()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("* this is a list item*");
        expects.add("...<li>this is a list item*</li>...");
        tests.add("* this is a list *item*");
        expects.add("...<li>this is a list <strong>item</strong></li>...");
        test(tests, expects);
    }

    public void testSeveralInARow()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
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
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("This is not *not\nbold*");
        expects.add("This is not *not\nbold*");
        test(tests, expects);
    }

    public void testTimeComplexity()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        // Something like this should be (negatively) matched in linear time, thus it should take no
        // time. If the build takes a lot, then the regular expression is not in linear time, thus
        // wrong.
        String text = "*";
        for (int i = 0; i < 1000; ++i) {
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
