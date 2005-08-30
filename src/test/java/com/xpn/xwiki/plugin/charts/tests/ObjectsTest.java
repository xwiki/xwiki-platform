package com.xpn.xwiki.plugin.charts.tests;


import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;

public class ObjectsTest extends TestCase {
	public ObjectsTest(String arg0) {
		super(arg0);
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(ObjectsTest.class);
	}

	protected void setUp() throws Exception {
        this.config = new XWikiConfig();
        this.config.put("xwiki.store.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");

        this.context = new XWikiContext();
        this.xwiki = new XWiki(this.config, this.context);
        this.context.setWiki(this.xwiki);
        
		this.xclass = TestHelper.createTableDataSourceClass(context);
		
		this.doc = TestHelper.createDocument("Main.Doc", "{table}\ncontent\n{table}", context);
	}

	protected void tearDown() throws Exception {
	}
	
	public void testObjectDocument() throws Exception {
		BaseObject xobject = TestHelper.defineTable(xclass, this.doc, this.context, 0, "A1-A1", false, false);
		Assert.assertEquals(this.doc.getFullName(), xobject.getName());
	}
	
	public void testListDocuments() throws Exception {
		List list = xwiki.getStore().searchDocumentsNames("", context);
		Assert.assertTrue(list.size()>=1);
		Assert.assertTrue(list.contains(doc));
		
		XWikiDocument doc1 = TestHelper.createDocument("XWikiTest.Doc1", "", context);
		XWikiDocument doc2 = TestHelper.createDocument("XWikiTest.Doc2", "", context);
		XWikiDocument doc3 = TestHelper.createDocument("XWikiTest.Doc3", "", context);

		list = xwiki.getStore().searchDocumentsNames("doc.fullName LIKE 'XWikiTest.%'", context);
		Assert.assertEquals(3, list.size());
		Assert.assertTrue(list.contains(doc1));
		Assert.assertTrue(list.contains(doc2));
		Assert.assertTrue(list.contains(doc3));
	}

	public void testId() throws Exception {
		BaseObject obj = TestHelper.defineTable(xclass, this.doc, this.context, 0, "A1-A1", false, false);

		List list = xwiki.getStore().search("from "+BaseObject.class.getName()
				+ " as obj where obj.id='"+obj.getId()+"'", 0, 0, context);
		Assert.assertEquals(1, list.size());

		BaseObject obj2 = (BaseObject)list.get(0);

		List propertyList = xwiki.getStore().search("from "+BaseProperty.class.getName()
				+ " as p where p.id.id='"+obj.getId()+"'", 0, 0, context);

		Iterator it = propertyList.iterator();
		while (it.hasNext()) {
			BaseProperty prop = (BaseProperty)it.next();
			obj2.addField(prop.getName(), prop);
		}
		
		Assert.assertEquals(obj, obj2);
	}
    
    private XWiki xwiki;
    private XWikiConfig config;
    private XWikiContext context;
    private BaseClass xclass;
    private XWikiDocument doc;
}
