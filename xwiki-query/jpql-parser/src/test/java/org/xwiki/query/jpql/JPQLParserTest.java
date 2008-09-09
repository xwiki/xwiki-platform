package org.xwiki.query.jpql;

import junit.framework.TestCase;

public class JPQLParserTest extends TestCase
{
    JPQLParser parser = new JPQLParser();

    public void testStartSpeed() throws Exception
    {
        // heat up parser engine for more accuracy test timing  
        parser.parse("select a from A as a");
    }

    public void testQuote() throws Exception
    {
        parser.parse("select a from A as a where a.f='str'");
        parser.parse("select a from A as a where a.f=\"str\"");
    }

    public void testObjectsInFrom() throws Exception
    {
        parser.parse("select doc from Document as doc, doc.object('XWiki.Test') as test where test.some=1");
        parser.parse("select doc from Document as doc, doc.object(XWiki.Test) as test where test.some=1");
    }

    public void testObjectInWhere() throws Exception
    {
        parser.parse("select doc from Document as doc where doc.object('XWiki.Test').some=1");
        parser.parse("select doc from Document as doc where doc.object(XWiki.Test).some=1");
    }

    public void testOrderBy() throws Exception
    {
        parser.parse("select doc from Document doc, doc.object(XWiki.XWikiGroups) as g order by g.number");
        // TODO:
        //parser.parse("select doc from Document doc, doc.object(XWiki.XWikiGroups) as g order by g.number desc");
    }
}
