package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

public class LineTest extends SyntaxTestsParent
{
    protected void setUp()
    {
        super.setUp();
    }

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

    /*
    public void testSequence()
    {
        ArrayList<String> tests = new ArrayList<String>();
        ArrayList<String> expects = new ArrayList<String>();
        tests.add("---- ------   ----");
        expects.add("<hr/> <hr/>   <hr/>");
        test(tests, expects);
    }
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
