package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBTreeListClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 19 oct. 2006
 * Time: 16:08:43
 * To change this template use File | Settings | File Templates.
 */
public class ClassAdvancedTest extends HibernateTestCase {

    protected void setUp() throws Exception {
       super.setUp();
    }

    protected void teadDown() throws Exception {
       super.tearDown();
    }

    /**
     *
     * @throws com.xpn.xwiki.XWikiException
         */
    public void testTreeMapDisplayers() throws XWikiException {
        // Set a tree class
        XWikiDocument doc = new XWikiDocument("XWiki", "TreeClass");
        BaseClass bclass = doc.getxWikiClass();
        bclass.addTextField("treename", "Tree Name", 30);
        xwiki.saveDocument(doc, context);

        createTreeDoc("Item1", "Item 1", "");
        createTreeDoc("Item11","Item 1.1", "Item1");
        createTreeDoc("Item12","Item 1.2", "Item1");
        createTreeDoc("Item121","Item 1.2.1", "Item12");
        createTreeDoc("Item2","Item 2", "");
        createTreeDoc("Item21", "Item 2.1","Item2");
        createTreeDoc("Item22", "Item 2.2","Item2");
        createTreeDoc("Item221", "Item 2.2.1","Item21");
        createTreeDoc("Item222", "Item 2.2.2", "Item21");
        createTreeDoc("Item3", "Item 3","");

        doc = new XWikiDocument("XWiki", "OtherClass");
        bclass = doc.getxWikiClass();
        bclass.addDBTreeListField("treeitem", "Tree Item", "select doc.fullName, doc.title, doc.parent from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name and obj.className='XWiki.TreeClass' order by doc.title");
        xwiki.saveDocument(doc, context);

        doc = new XWikiDocument("XWiki", "OtherPage");
        BaseObject obj = doc.newObject("XWiki.OtherClass", context);
        List list = new ArrayList();
        list.add("Tree.Item221");
        list.add("Tree.Item12");
        doc.setListValue("XWiki.OtherClass", "treeitem", list);

        String[] viewresult = {"Item 2.2.1 Item 1.2"};
        String[] editresult = {"<select", "Tree.Item1", "Tree.Item21", ">Item 1", ">Item 2", ">&nbsp;Item 2.1", ">&nbsp;&nbsp;Item 2.2.1"};
        ClassesTest.testDisplayer("treeitem", obj, bclass, viewresult, editresult, context);
    }

    /**
     *
     * @throws com.xpn.xwiki.XWikiException
         */
    public void testTreeMapDisplayers2() throws XWikiException {
        // Set a tree class
        XWikiDocument doc = new XWikiDocument("XWiki", "TreeClass");
        BaseClass bclass = doc.getxWikiClass();
        bclass.addTextField("treename", "Tree Name", 30);
        xwiki.saveDocument(doc, context);

        createTreeDoc("Item1", "Item 1", "");
        createTreeDoc("Item11","Item 1.1", "Item1");
        createTreeDoc("Item12","Item 1.2", "Item1");
        createTreeDoc("Item121","Item 1.2.1", "Item12");
        createTreeDoc("Item2","Item 2", "");
        createTreeDoc("Item21", "Item 2.1","Item2");
        createTreeDoc("Item22", "Item 2.2","Item2");
        createTreeDoc("Item221", "Item 2.2.1","Item21");
        createTreeDoc("Item222", "Item 2.2.2", "Item21");
        createTreeDoc("Item3", "Item 3","");

        doc = new XWikiDocument("XWiki", "OtherClass");
        bclass = doc.getxWikiClass();
        // Multiple select
        bclass.addDBTreeListField("treeitem", "Tree Item", 10, true, "select doc.fullName, doc.title, doc.parent from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name and obj.className='XWiki.TreeClass' order by doc.title");
        xwiki.saveDocument(doc, context);

        doc = new XWikiDocument("XWiki", "OtherPage");
        BaseObject obj = doc.newObject("XWiki.OtherClass", context);
        List list = new ArrayList();
        list.add("Tree.Item221");
        list.add("Tree.Item12");
        doc.setListValue("XWiki.OtherClass", "treeitem", list);

        String[] viewresult = {"Item 2.2.1 Item 1.2"};
        String[] editresult = {"<select", "multiple", "Tree.Item1", "Tree.Item21", ">Item 1", ">Item 2", ">&nbsp;Item 2.1", ">&nbsp;&nbsp;Item 2.2.1"};
        ClassesTest.testDisplayer("treeitem", obj, bclass, viewresult, editresult, context);
    }

    private void createTreeDoc(String pageName, String title, String parentName) throws XWikiException {
        XWikiDocument doc;
        doc = new XWikiDocument("Tree", pageName);
        doc.setParent(parentName.equals("") ? "" : "Tree." + parentName);
        doc.setTitle(title);
        doc.getObject("XWiki.TreeClass", true, context);
        doc.setStringValue("XWiki.TreeClass", "treename", "tree1");
        xwiki.saveDocument(doc, context);
    }

}
