/**
 * ===================================================================
 *
 * Copyright (c) 2005 Artem Melentev, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Artem Melentev
 */
package com.xpn.xwiki.test.plugin.query;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.jcr.query.InvalidQueryException;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.plugin.query.IQueryFactory;
import com.xpn.xwiki.plugin.query.QueryPlugin;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.HibernateTestCase;
import com.xpn.xwiki.test.Utils;

public class QueryPluginTest extends HibernateTestCase {
	private static final Object[] NOTHING = new Object[]{};
	IQueryFactory qf;
	
	protected void setUp() throws Exception {		
		super.setUp();
		getXWiki().setPluginManager(new XWikiPluginManager("com.xpn.xwiki.plugin.query.QueryPlugin", getXWikiContext()));
        QueryPlugin plugin = (QueryPlugin) getXWiki().getPluginManager().getPlugin("query");
        //qf = (QueryFactory) plugin.getPluginApi(plugin, getXWikiContext());
        qf = plugin;
	}
	
	public void checkEquals(List lst, Object[] exps) throws XWikiException, InvalidQueryException {
		assertEquals("Length not same", exps.length, lst.size());
		for (int i=0; i<exps.length; i++) {
			final Object obj = lst.get(i);
			final Object exp = exps[i];
			if (obj instanceof Object[]) {
				Object[] obj0 = (Object[]) obj,
						exp0 = (Object[]) exp;
				assertEquals(obj0.length, exp0.length);
				for (int j=0; i<obj0.length; i++) {
					assertEquals(obj0[j], exp0[j]);
				}
			} else {
				if (exp==obj) continue; // XXX: bugs with proxy objects?
				assertEquals("Objects #"+i+" not equals", obj, exp);
			}
		}
	}
	public void testSearchXP(String sqx, Object[] exps) throws XWikiException, InvalidQueryException {
		checkEquals(qf.xpath(sqx).list(), exps);
	}
	public void testSearchXP1(String sqx, Object exp1) throws XWikiException, InvalidQueryException {
		checkEquals(qf.xpath(sqx).list(), new Object[]{exp1});
	}
	public void testSearchXP1(String sqx, long exp1) throws XWikiException, InvalidQueryException {
		testSearchXP1(sqx, new Long(exp1));
	}
	public void testSearchXP1(String sqx, int exp1) throws XWikiException, InvalidQueryException {
		testSearchXP1(sqx, new Integer(exp1));
	}
	public void testSearchQl(String sq, Object[] exps) throws XWikiException, InvalidQueryException {
		checkEquals(qf.ql(sq).list(), exps);
	}
	public void testSearchQl1(String sq, Object exp1) throws XWikiException, InvalidQueryException {
		checkEquals(qf.ql(sq).list(), new Object[]{exp1});
	}
	public void testSearchXPnQl(String sqx, String sqs, Object[] exps) throws InvalidQueryException, XWikiException {
		testSearchXP(sqx, exps);
		testSearchQl(sqs, exps);
	}
	public void testSearchXPnQl1(String sqx, String sqs, Object exp1) throws InvalidQueryException, XWikiException {
		testSearchXP1(sqx, exp1);
		testSearchQl1(sqs, exp1);
	}
	
	public void testWebDocs() throws XWikiException, InvalidQueryException {
		XWikiHibernateStore hibstore = getXWiki().getHibernateStore();
		
		testSearchXP("//*/*",		NOTHING);
		checkEquals(qf.getDocs("*/*", null, null).list(), NOTHING);
		// testSearchXP("//*",		NOTHING); - not supported
		
		XWikiDocument doc1 = new XWikiDocument("Main", "WebHome");
        doc1.setContent("no content");
        doc1.setAuthor("Artem Melentev");
        doc1.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc1, getXWikiContext());

        testSearchXP1("//*/*", doc1);
        checkEquals(qf.getDocs("*/*", null, null).list(), new Object[]{doc1});
        //testSearchXP1("//*", "Main");
        //testSearchXP1("//Main", "Main");
        testSearchXP1("//Main/WebHome", doc1);
        checkEquals(qf.getDocs("Main.WebHome", null, null).list(), new Object[]{doc1});
        testSearchXP1("//*/WebHome", doc1);
        checkEquals(qf.getDocs("*.WebHome", null, null).list(), new Object[]{doc1});
        testSearchXP1("//Main/*", doc1);
        checkEquals(qf.getDocs("Main/*", null, null).list(), new Object[]{doc1});
        testSearchXP1("//Main/*[@parent='Main.WebHome']", doc1);
        checkEquals(qf.getChildDocs("Main/WebHome", null, null).list(), new Object[]{doc1});
        testSearchXP("//Main/*[@parent!='Main.WebHome']", NOTHING);
        checkEquals(qf.getDocs("Main/*[@parent!='Main.WebHome']", null, null).list(), NOTHING);
        
        XWikiDocument doc2 = new XWikiDocument("Main", "WebHome2");
        doc2.setContent("no content");
        doc2.setAuthor("Someone");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc2, getXWikiContext());
        
        testSearchXP("//*/*",					new Object[]{doc1, doc2});
        checkEquals(qf.getDocs("*/*", null, null).list(), new Object[]{doc1, doc2});
        testSearchXP("//*/* order by @creationDate descending",	new Object[]{doc2, doc1});
        checkEquals(qf.getDocs("*/*", null, "-@creationDate").list(), new Object[]{doc2, doc1});
        testSearchXP("//*/*/@name",			new Object[]{"WebHome", "WebHome2"});
        checkEquals(qf.getDocs("*/*", "@name", null).list(), new Object[]{"WebHome", "WebHome2"});
        //testSearchXP("//*",					new Object[]{"Main"});
        testSearchXP("//*/WebHome",			new Object[]{doc1});
        checkEquals(qf.getDocs("*/WebHome", null, null).list(), new Object[]{doc1});
        testSearchXP("//*/WebHome2",		new Object[]{doc2});
        checkEquals(qf.getDocs("*/WebHome2", null, null).list(), new Object[]{doc2});
        //testSearchXP("//Main",				new Object[]{"Main"});
        testSearchXP("//Main/*",			new Object[]{doc1,doc2});
        checkEquals(qf.getDocs("Main/*", null, null).list(), new Object[]{doc1,doc2});
        //testSearchXP("//*",					new Object[]{"Main"});        
        testSearchXP("//Main/*[jcr:like(@name, '%2')]",		new Object[]{doc2});
        checkEquals(qf.getDocs("Main/*[jcr:like(@name, '%2')]", null, null).list(), new Object[]{doc2});
        
        XWikiDocument doc3 = new XWikiDocument("Main", "WebHome3");
        doc3.setContent("no content");
        doc3.setAuthor("Artem Melentev");
        doc3.setParent("Main.WebHome2");
        hibstore.saveXWikiDoc(doc3, getXWikiContext());
        
        testSearchXP("//*/*[@parent='Main.WebHome2']",				new Object[]{doc3});
        checkEquals(qf.getChildDocs("Main/WebHome2", null, null).list(), new Object[]{doc3});
        
        XWikiDocument doc4 = new XWikiDocument("Test", "WebHome4");
        doc4.setContent("no content");
        doc4.setAuthor("Someone");
        doc4.setParent("Main.WebHome2");
        hibstore.saveXWikiDoc(doc4, getXWikiContext());
        
        testSearchXP("//*/*[@parent='Main.WebHome2']",				new Object[]{doc3, doc4});
        checkEquals(qf.getChildDocs("Main/WebHome2", null, null).list(), new Object[]{doc3, doc4});
        testSearchXP("//*/*[@author='Someone']",					new Object[]{doc2, doc4});
        checkEquals(qf.getDocs("*/*[@author='Someone']", null, null).list(), new Object[]{doc2, doc4});
        testSearchXP("//*/*[@author!='Someone']",					new Object[]{doc1, doc3});
        checkEquals(qf.getDocs("*/*[@author!='Someone']", null, null).list(), new Object[]{doc1, doc3});
        testSearchXP("//Test/*[@author='Someone']",					new Object[]{doc4});
        checkEquals(qf.getDocs("Test/*[@author='Someone']", null, null).list(), new Object[]{doc4});
        
        testSearchXP("//*/*[@author!='Someone' and jcr:like(@name, '%3')]",	new Object[]{doc3});
        checkEquals(qf.getDocs("*/*[@author!='Someone' and jcr:like(@name, '%3')]", null, null).list(), new Object[]{doc3});
        testSearchXP("//*/*[@author!='Someone' and not(jcr:like(@name, '%3'))]",	new Object[]{doc1});
        checkEquals(qf.getDocs("*/*[@author!='Someone' and not(jcr:like(@name, '%3'))]", null, null).list(), new Object[]{doc1});
        testSearchXP("//*/*[@author='Someone' or @author='Artem Melentev']",	new Object[]{doc1,doc2,doc3,doc4});
        checkEquals(qf.getDocs("*/*[@author='Someone' or @author='Artem Melentev']", null, null).list(), new Object[]{doc1,doc2,doc3,doc4});
        
        XWikiDocument doc5 = new XWikiDocument("Test", "WebHome5");
        doc5.setContent("is content");
        doc5.setAuthor("Someone");
        doc5.setParent("Main.WebHome2");
        hibstore.saveXWikiDoc(doc5, getXWikiContext());
        testSearchXP("//*/*[@parent='Main.WebHome2']",	new Object[]{doc3,doc4,doc5});
        checkEquals(qf.getChildDocs("Main.WebHome2", null, null).list(), new Object[]{doc3,doc4,doc5});
        testSearchXP("//*/*[@parent='Main.WebHome2']/@name",	new Object[]{doc3.getName(),doc4.getName(),doc5.getName()});
        checkEquals(qf.getChildDocs("Main.WebHome2", "@name", null).list(), new Object[]{doc3.getName(),doc4.getName(),doc5.getName()});
        testSearchXP("//*/*[@parent='Main.WebHome2' and (@author='Artem Melentev' or @content='is content')]",	new Object[]{doc3,doc5});
        checkEquals(qf.getDocs("*/*[@parent='Main.WebHome2' and (@author='Artem Melentev' or @content='is content')]", null, null).list(), new Object[]{doc3,doc5});
        
        checkEquals(qf.xpath("//*/* order by @creationDate descending").setMaxResults(2).list(), new Object[]{doc5,doc4});
        checkEquals(qf.getDocs("*/*", "", "-@creationDate").setMaxResults(2).list(), new Object[]{doc5,doc4});
        
        checkEquals(qf.getDocs("*.*[@author='Artem Melentev']", "@author", "").setDistinct(true).list(), new Object[]{"Artem Melentev"});
        checkEquals(qf.getDocs("*.*[@author='Artem Melentev']", "@author", "").setDistinct(false).list(), new Object[]{"Artem Melentev", "Artem Melentev"});
        
        testSearchXPnQl1("/element(WebHome4, xwiki:document)/@parent", "select parent from xwiki:document where name='WebHome4'", "Main.WebHome2");
	}
	
	// XXX: Attachments don`t store it`s document!!!
	public void testAttachments() throws XWikiException, IOException, InvalidQueryException {
		XWikiHibernateStore hb = getXWiki().getHibernateStore();
		hb.beginTransaction(getXWikiContext());
		
		XWikiDocument doc1 = new XWikiDocument("Test", "TestAttach1");		
        doc1.setContent("no content");
        doc1.setAuthor("Someone");
        doc1.setParent("Test.WebHome");
        hb.saveXWikiDoc(doc1, getXWikiContext());
        
        Utils.setStandardData();
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, "testfile1");
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, getXWikiContext());
        doc1.getAttachmentList().add(attachment1);        
        hb.saveXWikiDoc(doc1, getXWikiContext());
                
        attachment1 = (XWikiAttachment) hb.getSession(getXWikiContext()).load(XWikiAttachment.class, new Long(attachment1.getId()));
                
        testSearchXP("//*/*/attach/*", new Object[]{attachment1});
        testSearchXPnQl1("//element(*, xwiki:attachment)", "select * from xwiki:attachment", attachment1);
        checkEquals(qf.getAttachment("*/*", "*", null).list(), new Object[]{attachment1});
        testSearchXP("//*/*/attach/*/@filename", new Object[]{attachment1.getFilename()});
        testSearchXPnQl1("/element(*, xwiki:attachment)/@filename", "select filename from xwiki:attachment", "testfile1");
        testSearchXP("//*/TestAttach1/attach/*", new Object[]{attachment1});
        testSearchXP1("//element(TestAttach1, xwiki:document)/attach/*", attachment1);
        testSearchXP1("/*/element(TestAttach1, xwiki:document)/attach/*", attachment1);
        testSearchXP1("//element(TestAttach1, xwiki:document)/element(*, xwiki:attachment)", attachment1);
        checkEquals(qf.getAttachment("*.TestAttach1", "*", null).list(), new Object[]{attachment1});
        testSearchXP("//Test/*/attach/*", new Object[]{attachment1});
        testSearchXP1("//Test/*/element(*,xwiki:attachment)", attachment1);
        testSearchXP1("/element(*,xwiki:document)/element(*,xwiki:attachment)", attachment1);
        checkEquals(qf.getAttachment("Test.*", "*", null).list(), new Object[]{attachment1});
        testSearchXP("//*/*/attach/testfile1", new Object[]{attachment1});
        testSearchXP1("/attach/testfile1", attachment1);
        checkEquals(qf.getAttachment("*/*", "testfile1", null).list(), new Object[]{attachment1});
        
        XWikiAttachment attachment2 = new XWikiAttachment(doc1, "testfile2");
        byte[] attachcontent2 = Utils.getDataAsBytes(new File(Utils.filename2));
        attachment2.setContent(attachcontent2);
        attachment2.setComment("comment");
        doc1.saveAttachmentContent(attachment2, getXWikiContext());
        doc1.getAttachmentList().add(attachment2);
        hb.saveXWikiDoc(doc1, getXWikiContext());
        attachment2 = (XWikiAttachment) hb.getSession(getXWikiContext()).load(XWikiAttachment.class, new Long(attachment2.getId()));
        
        testSearchXP("//*/*/attach/*",				new Object[]{attachment1, attachment2});
        testSearchXPnQl("/element(*,xwiki:attachment)", "select * from xwiki:attachment", new Object[]{attachment1, attachment2});
        checkEquals(qf.getAttachment("*/*", "*", null).list(), new Object[]{attachment1,attachment2});
        testSearchXP("//*/*/attach/testfile1",		new Object[]{attachment1});
        checkEquals(qf.getAttachment("*/*", "testfile1", null).list(), new Object[]{attachment1});
        testSearchXP("//*/*/attach/testfile2",		new Object[]{attachment2});
        testSearchXPnQl1("/element(*, xwiki:document)/attach/element(testfile2, xwiki:attachment)", "select * from xwiki:attachment where filename='testfile2'",	attachment2);
        testSearchXP("//attach/testfile2",		new Object[]{attachment2});
        checkEquals(qf.getAttachment("*/*", "testfile2", null).list(), new Object[]{attachment2});
        testSearchXP("//Test/TestAttach1/attach/testfile1", new Object[]{attachment1});
        checkEquals(qf.getAttachment("Test/TestAttach1", "testfile1", null).list(), new Object[]{attachment1});
        
        XWikiDocument doc2 = new XWikiDocument("Test", "TestAttach2");
        doc2.setContent("no content");
        doc2.setAuthor("Someone over");
        doc2.setParent("Test.WebHome");
        hb.saveXWikiDoc(doc2, getXWikiContext()); // XXX: Was same attachments of different documents be? That would query return? 2 or 1
        
        XWikiAttachment attachment3 = new XWikiAttachment(doc2, "testfile1");
        byte[] attachcontent3 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment3.setContent(attachcontent3);
        doc2.saveAttachmentContent(attachment3, getXWikiContext());
        doc2.getAttachmentList().add(attachment3);
        hb.saveXWikiDoc(doc2, getXWikiContext());
        
        testSearchXP("//*/*/attach/*",				new Object[]{attachment1, attachment2, attachment3});
        checkEquals(qf.getAttachment("*/*", "*", null).list(), new Object[]{attachment1,attachment2,attachment3});
        testSearchXP("//Test/TestAttach2/attach/*",	new Object[]{attachment3});
        checkEquals(qf.getAttachment("Test.TestAttach2", "*", null).list(), new Object[]{attachment3});
        testSearchXP("//Test/TestAttach2/attach/testfile1",	new Object[]{attachment3});
        checkEquals(qf.getAttachment("Test.TestAttach2", "testfile1", null).list(), new Object[]{attachment3});
        testSearchXP("//*/*/attach/testfile1",		new Object[]{attachment1, attachment3});
        checkEquals(qf.getAttachment("*.*", "testfile1", null).list(), new Object[]{attachment1, attachment3});
        testSearchXP("//Test/*[@author='Someone']/attach/*[@comment!='']",	new Object[]{attachment2});
        checkEquals(qf.getAttachment("Test.*[@author='Someone']", "*[@comment!='']", null).list(), new Object[]{attachment2});
        
        hb.endTransaction(getXWikiContext(), false);
	}
	
	public void testObjects() throws HibernateException, XWikiException, InvalidQueryException {		
		XWikiHibernateStore hb = getXWiki().getHibernateStore();
		hb.beginTransaction(getXWikiContext());
		XWikiDocument doc0, doc = doc0 = new XWikiDocument("Test", "TestClass");		
		BaseClass bclass1, bclass = bclass1 = Utils.prepareClass(doc, "Test.TestClass");
		hb.saveXWikiDoc(doc, getXWikiContext());
		
		XWikiDocument doc1 = doc = new XWikiDocument("Test", "TestObject");
		hb.saveXWikiDoc(doc, getXWikiContext());
		hb.endTransaction(getXWikiContext(), true);
		
		testSearchXP("//*/*/obj/*/*",									NOTHING);
		checkEquals(qf.getObjects("*/*","*/*",null,null).list(), NOTHING);
		testSearchXP("//*/*/obj/*/*/@doc:self",						NOTHING);
		checkEquals(qf.getObjects("*/*","*.*","@doc:self",null).list(), NOTHING);
		
		//doc = new XWikiDocument("Test", "TestObject");
		//doc = hb.loadXWikiDoc(doc, getXWikiContext());
		
		hb.beginTransaction(getXWikiContext());
		BaseObject object1, object = object1 = new BaseObject();
        doc.setObject(bclass.getName(), 0, object);
        object.setClassName(bclass.getName());
        object.setName(doc.getFullName());
        object.put("first_name", ((PropertyClass)bclass.get("first_name")).fromString("Artem"));
        object.put("last_name", ((PropertyClass)bclass.get("last_name")).fromString("Melentev"));
        object.put("age", ((PropertyClass)bclass.get("age")).fromString("20"));
        object.put("password", ((PropertyClass)bclass.get("password")).fromString("sesame"));
        object.put("comment",((PropertyClass)bclass.get("comment")).fromString("Hello1\nHello2\nHello3\n"));       
        hb.saveXWikiDoc(doc, getXWikiContext());
        hb.endTransaction(getXWikiContext(), true);
        
        hb.beginTransaction(getXWikiContext());
        object1 = (BaseObject) hb.getSession(getXWikiContext()).load(object.getClass(), new Integer(object1.getId()));        
        
        testSearchXP("//*/*/obj/*/*",							new Object[]{object1});
        checkEquals(qf.getObjects("*/*","*/*",null,null).list(), new Object[]{object1});
        //XXX: testsearch("//*/*/obj/*",					new Object[]{"Test"});        
        testSearchXP("//Test/TestObject/obj/*/*",				new Object[]{object1});
        checkEquals(qf.getObjects("Test.TestObject","*/*",null,null).list(), new Object[]{object1});
        testSearchXP("//Test/TestObject/obj/Test/TestClass",	new Object[]{object1});
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass",null,null).list(), new Object[]{object1});
        testSearchXP("//*/*/obj/*/*/@className",				new Object[]{"Test.TestClass"});
        checkEquals(qf.getObjects("*/*","*/*","@className",null).list(), new Object[]{"Test.TestClass"});
        testSearchXP("//*/*/obj/*/*/@name",					new Object[]{"Test.TestObject"});
        checkEquals(qf.getObjects("*/*","*/*","@name",null).list(), new Object[]{"Test.TestObject"});
        testSearchXP("//Test/TestObject/obj/Test/*",			new Object[]{object1});
        checkEquals(qf.getObjects("Test/TestObject","Test.*",null,null).list(), new Object[]{object1});
        testSearchXP("//Test/TestObject/obj/*/TestClass",		new Object[]{object1});
        checkEquals(qf.getObjects("Test.TestObject","*.TestClass",null,null).list(), new Object[]{object1});
        
        testSearchXP("//Test/TestObject/obj/Test/TestClass/(@name,@className)",			new Object[]{new Object[]{"Test.TestObject", "Test.TestClass"}});
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass","@name,@className",null).list(), new Object[]{new Object[]{"Test.TestObject", "Test.TestClass"}});
        
        doc = (XWikiDocument) hb.getSession(getXWikiContext()).load(XWikiDocument.class, new Long(doc1.getId())); 
        testSearchXP("//*/*/obj/*/*/@doc:self",						new Object[]{doc});
		checkEquals(qf.getObjects("*/*","*/*","@doc:self",null).list(), new Object[]{doc});
        
        hb.endTransaction(getXWikiContext(), false);
        hb.beginTransaction(getXWikiContext());
        
        doc = hb.loadXWikiDoc(doc1, getXWikiContext());
        BaseObject object2 = object = new BaseObject();
        doc.setObject("Test.TestClass", 1, object);
        object.setClassName("Test.TestClass");
        object.setName(doc.getFullName());
        object.put("first_name", ((PropertyClass)bclass.get("first_name")).fromString("Ivan"));
        object.put("last_name", ((PropertyClass)bclass.get("last_name")).fromString("Ivanov"));
        object.put("age", ((PropertyClass)bclass.get("age")).fromString("21"));
        object.put("password", ((PropertyClass)bclass.get("password")).fromString("sesame"));
        object.put("comment",((PropertyClass)bclass.get("comment")).fromString("Hello2\nHello3\nHello4\n"));        
        hb.saveXWikiObject(object, getXWikiContext(), false);
        hb.endTransaction(getXWikiContext(), true);
        
        hb.beginTransaction(getXWikiContext());
        object1 = (BaseObject) hb.getSession(getXWikiContext()).load(object.getClass(), new Integer(object1.getId()));
        object2 = (BaseObject) hb.getSession(getXWikiContext()).load(object.getClass(), new Integer(object2.getId()));        
        
        testSearchXP("//*/*/obj/*/* order by @number",						new Object[]{object1, object2});
        checkEquals(qf.getObjects("*/*","*/*",null,"+@number").list(), new Object[]{object1, object2});
        testSearchXP("//*/*/obj/*/* order by @number descending",				new Object[]{object2, object1});
        checkEquals(qf.getObjects("*/*","*/*",null,"-@number").list(), new Object[]{object2, object1});
        testSearchXP("//Test/TestObject/obj/Test/TestClass order by @number",		new Object[]{object1, object2});
        checkEquals(qf.getObjects("Test/TestObject","Test/TestClass",null,"@number").list(), new Object[]{object1, object2});
        testSearchXP("//*/*/obj/Test/TestClass/@f:first_name order by @f:first_name descending",	new Object[]{"Ivan", "Artem"});
        checkEquals(qf.getObjects("*/*","Test/TestClass","@f:first_name","-@f:first_name").list(), new Object[]{"Ivan", "Artem"});
        // XXX: needed classname. testsearch("//*/*/obj/*/*/@f:first_name order by @f:first_name",	new Object[]{"Artem", "Ivan"});
        testSearchXP("//*/*/obj/Test/TestClass/@f:age  order by @f:age",						new Object[]{new Integer(20), new Integer(21)});
        checkEquals(qf.getObjects("*/*","Test.TestClass","@f:age","+@f:age").list(), new Object[]{new Integer(20), new Integer(21)});
        testSearchXP("//*/*/obj/Test/TestClass/@f:comment  order by @number",					new Object[]{"Hello1\nHello2\nHello3\n", "Hello2\nHello3\nHello4\n"});
        checkEquals(qf.getObjects("*/*","Test.TestClass","@f:comment","@number").list(),		new Object[]{"Hello1\nHello2\nHello3\n", "Hello2\nHello3\nHello4\n"});
        testSearchXP("//*/*/obj/Test/TestClass[@f:first_name='Artem']",		new Object[]{object1});
        checkEquals(qf.getObjects("*/*","Test.TestClass[@f:first_name='Artem']",null,null).list(), new Object[]{object1});
        testSearchXP("//*/*/obj/Test/TestClass[@f:first_name!='Artem']",		new Object[]{object2});
        checkEquals(qf.getObjects("*/*","Test.TestClass[@f:first_name!='Artem']",null,null).list(), new Object[]{object2});
        testSearchXP("//Test/*/obj/Test/TestClass[@f:age>20]",				new Object[]{object2});
        checkEquals(qf.getObjects("*/*","Test.TestClass[@f:age>20]",null,null).list(), new Object[]{object2});
        testSearchXP("//Test/TestObject/obj/Test/TestClass[@f:age>20]/@f:age",		new Object[]{new Integer(21)});
        checkEquals(qf.getObjects("*/*","Test.TestClass[@f:age>20]","@f:age",null).list(), new Object[]{new Integer(21)});
        testSearchXP("//Test/TestObject/obj/Test/TestClass[@f:age<20]/@f:first_name",	NOTHING);
        checkEquals(qf.getObjects("*/*","Test.TestClass[@f:age<20]","@f:first_name",null).list(), NOTHING);
        testSearchXP("//Test/TestObject/obj/Test/TestClass/@f:first_name order by @f:age",	new Object[]{"Artem", "Ivan"});
        checkEquals(qf.getObjects("*/*","Test.TestClass","@f:first_name","+@f:age").list(), new Object[]{"Artem", "Ivan"});
        testSearchXP("//Test/TestObject/obj/Test/TestClass/@f:first_name order by @f:age descending",	new Object[]{"Ivan", "Artem"});
        checkEquals(qf.getObjects("*/*","Test.TestClass","@f:first_name","-@f:age").list(), new Object[]{"Ivan", "Artem"});
        checkEquals(qf.getObjects("*/*","Test.TestClass","@f:first_name","@f:age descending").list(), new Object[]{"Ivan", "Artem"});
        
        testSearchXP("//Test/TestObject/obj/Test/TestClass[@f:age=20]/@f:password",	new Object[]{"sesame"}); // security is in QueryPluginApi
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass[@f:age=20]","@f:password", null).list(), new Object[]{"sesame"});
        testSearchXP("//*/*/obj/Test/TestClass/@f:first_name order by @f:first_name",	new Object[]{"Artem", "Ivan"});
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "@f:first_name").list(), new Object[]{"Artem", "Ivan"});
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "@f:first_name ascending").list(), new Object[]{"Artem", "Ivan"});
        
        testSearchXP("//*/*/obj/Test/TestClass/@f:first_name order by @f:password, @f:age",	new Object[]{"Artem", "Ivan"});
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "@f:password,@f:age").list(), new Object[]{"Artem", "Ivan"});
        testSearchXP("//*/*/obj/Test/TestClass/@f:first_name order by @f:password descending, @f:age descending",	new Object[]{"Ivan", "Artem"});
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "-@f:password,-@f:age").list(), new Object[]{"Ivan", "Artem"});
        testSearchXP("//*/*/obj/Test/TestClass/(@f:first_name,@f:age) order by @f:password descending, @f:age descending",	new Object[]{new Object[]{"Ivan", new Long(21)}, new Object[]{"Artem", new Long(20)}});
        checkEquals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name,@f:age", "-@f:password,-@f:age").list(), new Object[]{new Object[]{"Ivan", new Long(21)}, new Object[]{"Artem", new Long(20)}});
        
        testSearchXP("//*/*/obj/Test/TestClass[@f:first_name='Artem']/@doc:name",			new Object[]{"TestObject"});
		checkEquals(qf.getObjects("*/*","Test.TestClass[@f:first_name='Artem']","@doc:name",null).list(), new Object[]{"TestObject"});
        testSearchXP("//*/*/obj/Test/TestClass[@f:first_name!='Artem']/@doc:fullName",			new Object[]{"Test.TestObject"});
		checkEquals(qf.getObjects("*/*","Test.TestClass[@f:first_name!='Artem']","@doc:fullName",null).list(), new Object[]{"Test.TestObject"});
		testSearchXP("//*/*/obj/Test/TestClass[@f:first_name!='Artem' and @f:first_name!='Ivan']/@doc:web",	NOTHING);
		checkEquals(qf.getObjects("*/*","Test.TestClass[@f:first_name!='Artem' and @f:first_name!='Ivan']","@doc:fullName",null).list(), NOTHING);
		
		testSearchXP("//*/*/obj/Test/TestClass[@f:first_name!='Artem' and @f:first_name!='Ivan']/@f:first_name",	NOTHING);
		
        hb.endTransaction(getXWikiContext(), false);
	}
	
	public void test_jcr_contain() throws HibernateException, XWikiException, InvalidQueryException {
		XWikiHibernateStore hb = getXWiki().getHibernateStore();
		XWikiDocument doc0, doc = doc0 = new XWikiDocument("Test", "Contains");
		BaseClass bclass1, bclass = bclass1 = Utils.prepareClass(doc, "Test.Contains");
		bclass = Utils.prepareAdvancedClass(doc, "Test.Contains");
		ListClass propclass = (ListClass) bclass.get("dblist");
		propclass.setMultiSelect(true);
		propclass.setRelationalStorage(true);
		bclass.put("dblist", propclass);
		hb.saveXWikiDoc(doc0, getXWikiContext());
		
		XWikiDocument doc1 = doc = new XWikiDocument("Test", "Doc");		
		BaseObject object = Utils.prepareAdvancedObject(doc);
        doc.setObject(bclass.getName(), 0, object);
        object.setClassName(bclass.getName());
        object.setName(doc.getFullName());
        object.put("driver", ((PropertyClass)bclass.get("driver")).fromString("1"));
        object.put("category", ((PropertyClass)bclass.get("category")).fromString("1"));
        object.put("category2", ((PropertyClass)bclass.get("category2")).fromString("1|2"));
        object.put("category3", ((PropertyClass)bclass.get("category3")).fromString("1|2"));
        object.put("dblist", ((PropertyClass)bclass.get("dblist")).fromString("XWikiUsers|test2"));
        hb.saveXWikiDoc(doc1, getXWikiContext());

		getXWiki().flushCache();
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:category2, '1')]/@f:first_name", new Object[]{"Ludovic"});
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:category2, '2')]/@f:first_name", new Object[]{"Ludovic"});
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:category,  '2')]/@f:first_name", NOTHING);
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,    'XWikiUsers')]/@f:first_name", new Object[]{"Ludovic"});
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,    'XWikiUsers1')]/@f:first_name", NOTHING);
		testSearchXP("/*/*/obj/Test/Contains[not(jcr:contains(@f:dblist,'XWikiUsers'))]/(@f:first_name)", NOTHING);
		testSearchXP("/*/*/obj/Test/Contains[not(jcr:contains(@f:dblist,'XWikiUsers1'))]/@f:first_name", new Object[]{"Ludovic"});
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,    'test2')]/@f:first_name", new Object[]{"Ludovic"});
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,'test2') and jcr:contains(@f:dblist,'XWikiUsers')]/@f:first_name", new Object[]{"Ludovic"});
		testSearchXP("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,'test2') and jcr:contains(@f:dblist,'XWikiUsers1')]/@f:first_name", NOTHING);
		testSearchXP("/*/*/obj/Test/Contains[not(jcr:contains(@f:dblist,'test2'))and not(jcr:contains(@f:dblist,'XWikiUsers'))]/@f:first_name", NOTHING);		
	}
	
	public void testReturnListS() throws XWikiException, InvalidQueryException {
		XWikiHibernateStore hb = getXWiki().getHibernateStore();
		XWikiDocument doc0, doc = doc0 = new XWikiDocument("Test", "Contains");
		BaseClass bclass1, bclass = bclass1 = Utils.prepareClass(doc, "Test.Contains");
		bclass = Utils.prepareAdvancedClass(doc, "Test.Contains");
		ListClass propclass = (ListClass) bclass.get("dblist");
		propclass.setMultiSelect(true);
		propclass.setRelationalStorage(true);
		bclass.put("dblist", propclass);
		hb.saveXWikiDoc(doc0, getXWikiContext());
		
		XWikiDocument doc1 = doc = new XWikiDocument("Test", "Doc");		
		BaseObject object = Utils.prepareAdvancedObject(doc);
        doc.setObject(bclass.getName(), 0, object);
        object.setClassName(bclass.getName());
        object.setName(doc.getFullName());
        object.put("driver", ((PropertyClass)bclass.get("driver")).fromString("1"));
        object.put("category", ((PropertyClass)bclass.get("category")).fromString("1"));
        object.put("category2", ((PropertyClass)bclass.get("category2")).fromString("1|2"));
        object.put("category3", ((PropertyClass)bclass.get("category3")).fromString("1|2"));
        object.put("dblist", ((PropertyClass)bclass.get("dblist")).fromString("XWikiUsers|test2"));
        hb.saveXWikiDoc(doc1, getXWikiContext());
		
        getXWiki().flushCache();
		testSearchXP("/*/*/obj/Test/Contains/@f:category",	new Object[]{"1"});
		testSearchXP("/*/*/obj/Test/Contains/@f:category2",	new Object[]{"1|2"});
		// Hibernate(even 3.1) could not return lists..(NullPointer in parser) maybe needed special select query. question is posted
		//testsearch("/*/*/obj/Test/Contains/@f:dblist",		new Object[]{Arrays.asList(new Object[]{"XWikiUsers", "test2"})});
	}
	
	public void testDistinct() throws XWikiException, InvalidQueryException {
		XWikiHibernateStore hb = getXWiki().getHibernateStore();
		hb.beginTransaction(getXWikiContext());
		XWikiDocument doc1 = new XWikiDocument("Test", "TestObject");
		hb.saveXWikiDoc(doc1, getXWikiContext());
		XWikiDocument doc2 = new XWikiDocument("Test2", "TestObject");
		hb.saveXWikiDoc(doc2, getXWikiContext());
		hb.endTransaction(getXWikiContext(), true);
		
		testSearchXP("//*/*/@name",			new Object[]{"TestObject", "TestObject"});
		checkEquals(qf.getDocs("*.*", "@name", "").setDistinct(true).list(), new Object[]{"TestObject"});
		checkEquals(qf.getDocs("*.*", "(@name,@name)", "").setDistinct(true).list(), new Object[]{new Object[]{"TestObject", "TestObject"}});
	}
	
	public void testJcrClasses() throws InvalidQueryException, XWikiException, IOException {		
		// test empty
		testSearchXPnQl("//element(*, xwiki:document)", "select * from xwiki:document", NOTHING);		
		testSearchXPnQl("//element(*, xwiki:object)", "select * from xwiki:object", NOTHING);
		testSearchXPnQl("//element(*, xwiki:attachment)", "select * from xwiki:attachment", NOTHING);
		
		XWikiStoreInterface hb = getXWiki().getStore();
		
		// new Class
        XWikiDocument doc0, doc = doc0 = new XWikiDocument("Class", "Class1");
		BaseClass bclass = Utils.prepareClass(doc, "Class.Class1");
		hb.saveXWikiDoc(doc0, getXWikiContext());
		
		// new Doc
		XWikiDocument doc1 = doc = new XWikiDocument("Test", "Doc");
		hb.saveXWikiDoc(doc1, getXWikiContext());
		
		// new attach
		Utils.setStandardData();
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, "testfile1");
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, getXWikiContext());
        doc1.getAttachmentList().add(attachment1);
        hb.saveXWikiDoc(doc1, getXWikiContext());
        		
		// new Obj
		doc = doc1;
		BaseObject object1, object = object1 = new BaseObject();        
        object.setClassName(bclass.getName());
        object.setName(doc.getFullName());
        object.put("first_name", ((PropertyClass)bclass.get("first_name")).fromString("Artem"));
        object.put("last_name", ((PropertyClass)bclass.get("last_name")).fromString("Melentev"));
        object.put("age", ((PropertyClass)bclass.get("age")).fromString("20"));
        object.put("password", ((PropertyClass)bclass.get("password")).fromString("sesame"));
        object.put("comment",((PropertyClass)bclass.get("comment")).fromString("Hello1\nHello2\nHello3\n"));
        doc.setObject(bclass.getName(), 0, object);
        hb.saveXWikiDoc(doc1, getXWikiContext());
		
		testSearchXPnQl("//element(*, xwiki:document)/(@web,@name) order by @web descending", "select web,name from xwiki:document order by web desc",
				new Object[]{new Object[]{"Test", "Doc"}, new Object[]{"Class", "Class1"}});
		testSearchXPnQl1("//element(*, xwiki:object)[@className='Class.Class1']/@f:first_name", "select f:first_name from xwiki:object where className='Class.Class1'", 
				"Artem");
		testSearchXPnQl1("//element(*, xwiki:attachment)/@filename", "select filename from xwiki:attachment", 
				"testfile1");
		
		testSearchXPnQl1("/Test/element(*, xwiki:document)/@name", "select name from xwiki:document where web='Test'", "Doc");		
		testSearchXPnQl1("//Class/element(Class1, xwiki:object)/@f:last_name", "select f:last_name from xwiki:object where className='Class.Class1'", "Melentev");		

		testSearchXP1("/doc/Class/*/@name", "Class1");
		testSearchXP1("//obj/*/*/@name", "Test.Doc");		
		testSearchXP1("//attach/*/@filename", "testfile1");
		
		getXWiki().getHibernateStore().beginTransaction(false, getXWikiContext());
		testSearchXP1("//element(Doc, xwiki:document)//element(*, xwiki:object)/@id", object1.getId());
		testSearchXP1("/Test/Doc/element(*, xwiki:object)/@id", object1.getId());
		testSearchXP1("//element(Doc, xwiki:document)//*[@jcr:primaryType='xwiki:attachment']/@id", attachment1.getId());
		testSearchXP1("/Test/Doc/element(*, xwiki:attachment)/@id", attachment1.getId());
		testSearchXP1("/Test/Doc/element(*, xwiki:object)/@id", object1.getId());
		getXWiki().getHibernateStore().endTransaction(getXWikiContext(), false);
		
		testSearchXP("/Test/Doc/*/element(*, xwiki:document)", NOTHING); // list all childs of Test.Doc
		testSearchXP("/Test/Doc/*/*/*/*", NOTHING); // list all 2 level childs of Test.Doc
		
		XWikiDocument doc2 = new XWikiDocument("Test", "Doc2");
		doc2.setParent("Test.Doc");
		hb.saveXWikiDoc(doc2, getXWikiContext());
		XWikiDocument doc3 = new XWikiDocument("Test", "Doc3");
		doc3.setParent("Test.Doc2");
		hb.saveXWikiDoc(doc3, getXWikiContext());
		
		testSearchXP1("/Test/Doc/*/element(*, xwiki:document)/@fullName", "Test.Doc2"); // list all childs of Test.Doc
		testSearchXP1("/Test/Doc/doc/*/*/@name", "Doc2");
		testSearchXP1("/Test/Doc/*/*/*/*/(@web,@name)", new Object[]{"Test", "Doc3"}); // list all 2 level childs of Test.Doc
		testSearchXP1("/Test/Doc/doc/*/*/doc/*/*/@fullName", "Test.Doc3");
		
		//testsearch("/Test/Doc//element(*, xwiki:document)", new Object[]{}); // list all descendants! of Test.Doc. How implement this?
	}
}
