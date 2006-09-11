package com.xpn.xwiki.test.plugin.query;

import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.plugin.query.IQueryFactory;
import com.xpn.xwiki.plugin.query.QueryPlugin;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.Utils;
import com.xpn.xwiki.test.HibernateTestCase;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 10 sept. 2006
 * Time: 13:38:09
 * To change this template use File | Settings | File Templates.
 */
public class QueryGeneratorTest extends HibernateTestCase {
    IQueryFactory qf;

    protected void setUp() throws Exception {
        super.setUp();
        getXWiki().setPluginManager(new XWikiPluginManager("com.xpn.xwiki.plugin.query.QueryPlugin", getXWikiContext()));
        QueryPlugin plugin = (QueryPlugin) getXWiki().getPluginManager().getPlugin("query");
        //qf = (QueryFactory) plugin.getPluginApi(plugin, getXWikiContext());
        qf = plugin;
    }

    public void testQueryGenerator(XWikiQuery query, String result) throws XWikiException {
        assertEquals(result, qf.makeQuery(query));
    }

    public void testQueryGenerator() throws XWikiException {
        XWikiHibernateStore hb = getXWiki().getHibernateStore();
        XWikiDocument doc = new XWikiDocument("Test", "TestClass");
        Utils.prepareClass(doc, "Test.TestClass");
        Utils.prepareAdvancedClass(doc, "Test.TestClass");
        hb.saveXWikiDoc(doc, getXWikiContext());

        XWikiQuery query = new XWikiQuery();
        query.setParam("Test.TestClass_first_name", "Artem");

        assertEquals(query.getClasses().size(), 1);
        assertTrue(query.getClasses().contains("Test.TestClass"));

        testQueryGenerator(query, "//*/*/obj/Test/TestClass[jcr:like(@f:first_name, '%Artem%')]/@name");
        query = new XWikiQuery();
        query.setParam("Test.TestClass_first_name_exact", "Artem");
        testQueryGenerator(query, "//*/*/obj/Test/TestClass[@f:first_name='Artem']/@name");
        query = new XWikiQuery();
        query.setParam("Test.TestClass_first_name_not", "Artem");
        testQueryGenerator(query, "//*/*/obj/Test/TestClass[@f:first_name!='Artem']/@name");
        query = new XWikiQuery();
        query.setParam("Test.TestClass_age_morethan", new Integer(20));
        testQueryGenerator(query, "//*/*/obj/Test/TestClass[@f:age>20]/@name");
        query = new XWikiQuery();
        query.setParam("Test.TestClass_age_lessthan", new Integer(20));
        testQueryGenerator(query, "//*/*/obj/Test/TestClass[@f:age<20]/@name");
        query = new XWikiQuery();
        query.setParam("Test.TestClass_category", "1");
        testQueryGenerator(query, "//*/*/obj/Test/TestClass[jcr:contains(@f:category,'1')]/@name");
        String[] params = {"1", "2"};
        query = new XWikiQuery();
        query.setParam("Test.TestClass_category", params);
        testQueryGenerator(query, "//*/*/obj/Test/TestClass[(jcr:contains(@f:category,'1') or jcr:contains(@f:category,'2'))]/@name");
    }

    public void prepareData(XWikiHibernateStore hb) throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "TestClass");
        BaseClass bclass = Utils.prepareClass(doc, "Test.TestClass");
        hb.saveXWikiDoc(doc, getXWikiContext());

        XWikiDocument doc1 = new XWikiDocument("Test", "TestObject");
        // hb.saveXWikiDoc(doc1, getXWikiContext());
        BaseObject object  = new BaseObject();
        doc1.setObject(bclass.getName(), 0, object);
        object.setClassName(bclass.getName());
        object.setName(doc1.getFullName());
        object.put("first_name", ((PropertyClass)bclass.get("first_name")).fromString("Artem"));
        object.put("last_name", ((PropertyClass)bclass.get("last_name")).fromString("Melentev"));
        object.put("age", ((PropertyClass)bclass.get("age")).fromString("20"));
        object.put("password", ((PropertyClass)bclass.get("password")).fromString("sesame"));
        object.put("comment",((PropertyClass)bclass.get("comment")).fromString("Hello1\nHello2\nHello3\n"));
        hb.saveXWikiDoc(doc1, getXWikiContext());
    }

    public void testRunQuery() throws XWikiException {
        XWikiHibernateStore hb = getXWiki().getHibernateStore();
        prepareData(hb);

        XWikiQuery query = new XWikiQuery();
        query.setParam("Test.TestClass_first_name", "abc");
        List list = xwiki.search(query, context);
        assertEquals("List should have 0 item", 0, list.size());
        query.setParam("Test.TestClass_first_name", "rte");
        list = xwiki.search(query, context);
        assertEquals("List should have 1 item", 1, list.size());
    }

    public void testRunQueryAsTable() throws XWikiException {
        XWikiHibernateStore hb = getXWiki().getHibernateStore();
        prepareData(hb);

        XWikiQuery query = new XWikiQuery();
        query.setParam("Test.TestClass_first_name", "art");
        String[] displayProperties = { "Test.TestClass_first_name", "Test.TestClass_last_name", "Test.TestClass_age", "doc.name", "link"};
        query.setDisplayProperties(displayProperties);
        String result =  xwiki.searchAsTable(query, context);
        assertTrue("Result is invalid:\r\n" + result, result.indexOf("dfdfsd")!=-1);
    }

}
