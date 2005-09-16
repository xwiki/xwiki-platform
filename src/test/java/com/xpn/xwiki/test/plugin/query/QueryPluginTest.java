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
import com.xpn.xwiki.test.HibernateTestCase;
import com.xpn.xwiki.test.Utils;

public class QueryPluginTest extends HibernateTestCase {
	IQueryFactory qf;
	
	protected void setUp() throws Exception {		
		super.setUp();
		getXWiki().setPluginManager(new XWikiPluginManager("com.xpn.xwiki.plugin.query.QueryPlugin", getXWikiContext()));
        QueryPlugin plugin = (QueryPlugin) getXWiki().getPluginManager().getPlugin("query");
        //qf = (QueryFactory) plugin.getPluginApi(plugin, getXWikiContext());
        qf = plugin;
	}
	
	public void checkequals(List lst, Object[] exps) throws XWikiException, InvalidQueryException {
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
	public void testsearch(String sxq, Object[] exps) throws XWikiException, InvalidQueryException {
		checkequals(qf.xpath(sxq).list(), exps);		
	}
	
	public void testWebDocs() throws XWikiException, InvalidQueryException {
		XWikiHibernateStore hibstore = getXWiki().getHibernateStore();
		
		testsearch("//*/*",		new Object[]{});
		checkequals(qf.getDocs("*/*", null, null).list(), new Object[]{});
		testsearch("//*",		new Object[]{});
		
		XWikiDocument doc1 = new XWikiDocument("Main", "WebHome");
        doc1.setContent("no content");
        doc1.setAuthor("Artem Melentev");
        doc1.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc1, getXWikiContext());

        testsearch("//*/*", new Object[]{doc1});
        checkequals(qf.getDocs("*/*", null, null).list(), new Object[]{doc1});
        testsearch("//*", new Object[]{"Main"});
        testsearch("//Main", new Object[]{"Main"});
        testsearch("//Main/WebHome", new Object[]{doc1});
        checkequals(qf.getDocs("Main.WebHome", null, null).list(), new Object[]{doc1});
        testsearch("//*/WebHome", new Object[]{doc1});
        checkequals(qf.getDocs("*.WebHome", null, null).list(), new Object[]{doc1});
        testsearch("//Main/*", new Object[]{doc1});
        checkequals(qf.getDocs("Main/*", null, null).list(), new Object[]{doc1});
        testsearch("//Main/*[@parent='Main.WebHome']", new Object[]{doc1});
        checkequals(qf.getChildDocs("Main/WebHome", null, null).list(), new Object[]{doc1});
        testsearch("//Main/*[@parent!='Main.WebHome']", new Object[]{});
        checkequals(qf.getDocs("Main/*[@parent!='Main.WebHome']", null, null).list(), new Object[]{});
        
        XWikiDocument doc2 = new XWikiDocument("Main", "WebHome2");
        doc2.setContent("no content");
        doc2.setAuthor("Someone");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc2, getXWikiContext());
        
        testsearch("//*/*",					new Object[]{doc1, doc2});
        checkequals(qf.getDocs("*/*", null, null).list(), new Object[]{doc1, doc2});
        testsearch("//*/* order by @creationDate descending",	new Object[]{doc2, doc1});
        checkequals(qf.getDocs("*/*", null, "-@creationDate").list(), new Object[]{doc2, doc1});
        testsearch("//*/*/@name",			new Object[]{"WebHome", "WebHome2"});
        checkequals(qf.getDocs("*/*", "@name", null).list(), new Object[]{"WebHome", "WebHome2"});
        testsearch("//*",					new Object[]{"Main"});
        testsearch("//*/WebHome",			new Object[]{doc1});
        checkequals(qf.getDocs("*/WebHome", null, null).list(), new Object[]{doc1});
        testsearch("//*/WebHome2",		new Object[]{doc2});
        checkequals(qf.getDocs("*/WebHome2", null, null).list(), new Object[]{doc2});
        testsearch("//Main",				new Object[]{"Main"});
        testsearch("//Main/*",			new Object[]{doc1,doc2});
        checkequals(qf.getDocs("Main/*", null, null).list(), new Object[]{doc1,doc2});
        testsearch("//*",					new Object[]{"Main"});        
        testsearch("//Main/*[jcr:like(@name, '%2')]",		new Object[]{doc2});
        checkequals(qf.getDocs("Main/*[jcr:like(@name, '%2')]", null, null).list(), new Object[]{doc2});
        
        XWikiDocument doc3 = new XWikiDocument("Main", "WebHome3");
        doc3.setContent("no content");
        doc3.setAuthor("Artem Melentev");
        doc3.setParent("Main.WebHome2");
        hibstore.saveXWikiDoc(doc3, getXWikiContext());
        
        testsearch("//*/*[@parent='Main.WebHome2']",				new Object[]{doc3});
        checkequals(qf.getChildDocs("Main/WebHome2", null, null).list(), new Object[]{doc3});
        
        XWikiDocument doc4 = new XWikiDocument("Test", "WebHome4");
        doc4.setContent("no content");
        doc4.setAuthor("Someone");
        doc4.setParent("Main.WebHome2");
        hibstore.saveXWikiDoc(doc4, getXWikiContext());
        
        testsearch("//*/*[@parent='Main.WebHome2']",				new Object[]{doc3, doc4});
        checkequals(qf.getChildDocs("Main/WebHome2", null, null).list(), new Object[]{doc3, doc4});
        testsearch("//*/*[@author='Someone']",					new Object[]{doc2, doc4});
        checkequals(qf.getDocs("*/*[@author='Someone']", null, null).list(), new Object[]{doc2, doc4});
        testsearch("//*/*[@author!='Someone']",					new Object[]{doc1, doc3});
        checkequals(qf.getDocs("*/*[@author!='Someone']", null, null).list(), new Object[]{doc1, doc3});
        testsearch("//Test/*[@author='Someone']",					new Object[]{doc4});
        checkequals(qf.getDocs("Test/*[@author='Someone']", null, null).list(), new Object[]{doc4});
        
        testsearch("//*/*[@author!='Someone' and jcr:like(@name, '%3')]",	new Object[]{doc3});
        checkequals(qf.getDocs("*/*[@author!='Someone' and jcr:like(@name, '%3')]", null, null).list(), new Object[]{doc3});
        testsearch("//*/*[@author!='Someone' and not(jcr:like(@name, '%3'))]",	new Object[]{doc1});
        checkequals(qf.getDocs("*/*[@author!='Someone' and not(jcr:like(@name, '%3'))]", null, null).list(), new Object[]{doc1});
        testsearch("//*/*[@author='Someone' or @author='Artem Melentev']",	new Object[]{doc1,doc2,doc3,doc4});
        checkequals(qf.getDocs("*/*[@author='Someone' or @author='Artem Melentev']", null, null).list(), new Object[]{doc1,doc2,doc3,doc4});
        
        XWikiDocument doc5 = new XWikiDocument("Test", "WebHome5");
        doc5.setContent("is content");
        doc5.setAuthor("Someone");
        doc5.setParent("Main.WebHome2");
        hibstore.saveXWikiDoc(doc5, getXWikiContext());
        testsearch("//*/*[@parent='Main.WebHome2']",	new Object[]{doc3,doc4,doc5});
        checkequals(qf.getChildDocs("Main.WebHome2", null, null).list(), new Object[]{doc3,doc4,doc5});
        testsearch("//*/*[@parent='Main.WebHome2']/@name",	new Object[]{doc3.getName(),doc4.getName(),doc5.getName()});
        checkequals(qf.getChildDocs("Main.WebHome2", "@name", null).list(), new Object[]{doc3.getName(),doc4.getName(),doc5.getName()});
        testsearch("//*/*[@parent='Main.WebHome2' and (@author='Artem Melentev' or @content='is content')]",	new Object[]{doc3,doc5});
        checkequals(qf.getDocs("*/*[@parent='Main.WebHome2' and (@author='Artem Melentev' or @content='is content')]", null, null).list(), new Object[]{doc3,doc5});
        
        checkequals(qf.xpath("//*/* order by @creationDate descending").setMaxResults(2).list(), new Object[]{doc5,doc4});
        checkequals(qf.getDocs("*/*", "", "-@creationDate").setMaxResults(2).list(), new Object[]{doc5,doc4});
        
        checkequals(qf.getDocs("*.*[@author='Artem Melentev']", "@author", "").setDistinct(true).list(), new Object[]{"Artem Melentev"});
        checkequals(qf.getDocs("*.*[@author='Artem Melentev']", "@author", "").setDistinct(false).list(), new Object[]{"Artem Melentev", "Artem Melentev"});        
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
                
        testsearch("//*/*/attach/*", new Object[]{attachment1});
        checkequals(qf.getAttachment("*/*", "*", null).list(), new Object[]{attachment1});
        testsearch("//*/*/attach/*/@filename", new Object[]{attachment1.getFilename()});
        testsearch("//*/TestAttach1/attach/*", new Object[]{attachment1});
        checkequals(qf.getAttachment("*.TestAttach1", "*", null).list(), new Object[]{attachment1});
        testsearch("//Test/*/attach/*", new Object[]{attachment1});
        checkequals(qf.getAttachment("Test.*", "*", null).list(), new Object[]{attachment1});
        testsearch("//*/*/attach/testfile1", new Object[]{attachment1}); // XXX: What with spaces in filenames??
        checkequals(qf.getAttachment("*/*", "testfile1", null).list(), new Object[]{attachment1});
        
        XWikiAttachment attachment2 = new XWikiAttachment(doc1, "testfile2");
        byte[] attachcontent2 = Utils.getDataAsBytes(new File(Utils.filename2));
        attachment2.setContent(attachcontent2);
        attachment2.setComment("comment");
        doc1.saveAttachmentContent(attachment2, getXWikiContext());
        doc1.getAttachmentList().add(attachment2);
        hb.saveXWikiDoc(doc1, getXWikiContext());
        attachment2 = (XWikiAttachment) hb.getSession(getXWikiContext()).load(XWikiAttachment.class, new Long(attachment2.getId()));
        
        testsearch("//*/*/attach/*",				new Object[]{attachment1, attachment2});
        checkequals(qf.getAttachment("*/*", "*", null).list(), new Object[]{attachment1,attachment2});
        testsearch("//*/*/attach/testfile1",		new Object[]{attachment1});
        checkequals(qf.getAttachment("*/*", "testfile1", null).list(), new Object[]{attachment1});
        testsearch("//*/*/attach/testfile2",		new Object[]{attachment2});
        checkequals(qf.getAttachment("*/*", "testfile2", null).list(), new Object[]{attachment2});
        testsearch("//Test/TestAttach1/attach/testfile1", new Object[]{attachment1});
        checkequals(qf.getAttachment("Test/TestAttach1", "testfile1", null).list(), new Object[]{attachment1});
        
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
        
        testsearch("//*/*/attach/*",				new Object[]{attachment1, attachment2, attachment3});
        checkequals(qf.getAttachment("*/*", "*", null).list(), new Object[]{attachment1,attachment2,attachment3});
        testsearch("//Test/TestAttach2/attach/*",	new Object[]{attachment3});
        checkequals(qf.getAttachment("Test.TestAttach2", "*", null).list(), new Object[]{attachment3});
        testsearch("//Test/TestAttach2/attach/testfile1",	new Object[]{attachment3});
        checkequals(qf.getAttachment("Test.TestAttach2", "testfile1", null).list(), new Object[]{attachment3});
        testsearch("//*/*/attach/testfile1",		new Object[]{attachment1, attachment3});
        checkequals(qf.getAttachment("*.*", "testfile1", null).list(), new Object[]{attachment1, attachment3});
        testsearch("//Test/*[@author='Someone']/attach/*[@comment!='']",	new Object[]{attachment2});
        checkequals(qf.getAttachment("Test.*[@author='Someone']", "*[@comment!='']", null).list(), new Object[]{attachment2});
        
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
		
		testsearch("//*/*/obj/*/*",									new Object[]{});
		checkequals(qf.getObjects("*/*","*/*",null,null).list(), new Object[]{});
		testsearch("//*/*/obj/*/*/@doc:self",						new Object[]{});
		checkequals(qf.getObjects("*/*","*.*","@doc:self",null).list(), new Object[]{});
		
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
        
        testsearch("//*/*/obj/*/*",							new Object[]{object1});
        checkequals(qf.getObjects("*/*","*/*",null,null).list(), new Object[]{object1});
        //XXX: testsearch("//*/*/obj/*",					new Object[]{"Test"});        
        testsearch("//Test/TestObject/obj/*/*",				new Object[]{object1});
        checkequals(qf.getObjects("Test.TestObject","*/*",null,null).list(), new Object[]{object1});
        testsearch("//Test/TestObject/obj/Test/TestClass",	new Object[]{object1});
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass",null,null).list(), new Object[]{object1});
        testsearch("//*/*/obj/*/*/@className",				new Object[]{"Test.TestClass"});
        checkequals(qf.getObjects("*/*","*/*","@className",null).list(), new Object[]{"Test.TestClass"});
        testsearch("//*/*/obj/*/*/@name",					new Object[]{"Test.TestObject"});
        checkequals(qf.getObjects("*/*","*/*","@name",null).list(), new Object[]{"Test.TestObject"});
        testsearch("//Test/TestObject/obj/Test/*",			new Object[]{object1});
        checkequals(qf.getObjects("Test/TestObject","Test.*",null,null).list(), new Object[]{object1});
        testsearch("//Test/TestObject/obj/*/TestClass",		new Object[]{object1});
        checkequals(qf.getObjects("Test.TestObject","*.TestClass",null,null).list(), new Object[]{object1});
        
        testsearch("//Test/TestObject/obj/Test/TestClass/(@name,@className)",			new Object[]{new Object[]{"Test.TestObject", "Test.TestClass"}});
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass","@name,@className",null).list(), new Object[]{new Object[]{"Test.TestObject", "Test.TestClass"}});
        
        doc = (XWikiDocument) hb.getSession(getXWikiContext()).load(XWikiDocument.class, new Long(doc1.getId())); 
        testsearch("//*/*/obj/*/*/@doc:self",						new Object[]{doc});
		checkequals(qf.getObjects("*/*","*/*","@doc:self",null).list(), new Object[]{doc});
        
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
        
        testsearch("//*/*/obj/*/* order by @number",						new Object[]{object1, object2});
        checkequals(qf.getObjects("*/*","*/*",null,"+@number").list(), new Object[]{object1, object2});
        testsearch("//*/*/obj/*/* order by @number descending",				new Object[]{object2, object1});
        checkequals(qf.getObjects("*/*","*/*",null,"-@number").list(), new Object[]{object2, object1});
        testsearch("//Test/TestObject/obj/Test/TestClass order by @number",		new Object[]{object1, object2});
        checkequals(qf.getObjects("Test/TestObject","Test/TestClass",null,"@number").list(), new Object[]{object1, object2});
        testsearch("//*/*/obj/Test/TestClass/@f:first_name",				new Object[]{"Ivan", "Artem"});
        checkequals(qf.getObjects("*/*","Test/TestClass","@f:first_name",null).list(), new Object[]{"Ivan", "Artem"});
        // XXX: needed classname. testsearch("//*/*/obj/*/*/@f:first_name order by @f:first_name",	new Object[]{"Artem", "Ivan"});
        testsearch("//*/*/obj/Test/TestClass/@f:age",						new Object[]{new Integer(21), new Integer(20)});
        checkequals(qf.getObjects("*/*","Test.TestClass","@f:age",null).list(), new Object[]{new Integer(21), new Integer(20)});
        testsearch("//*/*/obj/Test/TestClass/@f:comment",					new Object[]{"Hello2\nHello3\nHello4\n", "Hello1\nHello2\nHello3\n"});
        checkequals(qf.getObjects("*/*","Test.TestClass","@f:comment",null).list(), new Object[]{"Hello2\nHello3\nHello4\n", "Hello1\nHello2\nHello3\n"});
        testsearch("//*/*/obj/Test/TestClass[@f:first_name='Artem']",		new Object[]{object1});
        checkequals(qf.getObjects("*/*","Test.TestClass[@f:first_name='Artem']",null,null).list(), new Object[]{object1});
        testsearch("//*/*/obj/Test/TestClass[@f:first_name!='Artem']",		new Object[]{object2});
        checkequals(qf.getObjects("*/*","Test.TestClass[@f:first_name!='Artem']",null,null).list(), new Object[]{object2});
        testsearch("//Test/*/obj/Test/TestClass[@f:age>20]",				new Object[]{object2});
        checkequals(qf.getObjects("*/*","Test.TestClass[@f:age>20]",null,null).list(), new Object[]{object2});
        testsearch("//Test/TestObject/obj/Test/TestClass[@f:age>20]/@f:age",		new Object[]{new Integer(21)});
        checkequals(qf.getObjects("*/*","Test.TestClass[@f:age>20]","@f:age",null).list(), new Object[]{new Integer(21)});
        testsearch("//Test/TestObject/obj/Test/TestClass[@f:age<20]/@f:first_name",	new Object[]{});
        checkequals(qf.getObjects("*/*","Test.TestClass[@f:age<20]","@f:first_name",null).list(), new Object[]{});
        testsearch("//Test/TestObject/obj/Test/TestClass/@f:first_name order by @f:age",	new Object[]{"Artem", "Ivan"});
        checkequals(qf.getObjects("*/*","Test.TestClass","@f:first_name","+@f:age").list(), new Object[]{"Artem", "Ivan"});
        testsearch("//Test/TestObject/obj/Test/TestClass/@f:first_name order by @f:age descending",	new Object[]{"Ivan", "Artem"});
        checkequals(qf.getObjects("*/*","Test.TestClass","@f:first_name","-@f:age").list(), new Object[]{"Ivan", "Artem"});
        checkequals(qf.getObjects("*/*","Test.TestClass","@f:first_name","@f:age descending").list(), new Object[]{"Ivan", "Artem"});
        
        testsearch("//Test/TestObject/obj/Test/TestClass[@f:age=20]/@f:password",	new Object[]{"sesame"}); // security is in QueryPluginApi
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass[@f:age=20]","@f:password", null).list(), new Object[]{"sesame"});
        testsearch("//*/*/obj/Test/TestClass/@f:first_name order by @f:first_name",	new Object[]{"Artem", "Ivan"});
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "@f:first_name").list(), new Object[]{"Artem", "Ivan"});
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "@f:first_name ascending").list(), new Object[]{"Artem", "Ivan"});
        
        testsearch("//*/*/obj/Test/TestClass/@f:first_name order by @f:password, @f:age",	new Object[]{"Artem", "Ivan"});
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "@f:password,@f:age").list(), new Object[]{"Artem", "Ivan"});
        testsearch("//*/*/obj/Test/TestClass/@f:first_name order by @f:password descending, @f:age descending",	new Object[]{"Ivan", "Artem"});
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name", "-@f:password,-@f:age").list(), new Object[]{"Ivan", "Artem"});
        testsearch("//*/*/obj/Test/TestClass/(@f:first_name,@f:age) order by @f:password descending, @f:age descending",	new Object[]{new Object[]{"Ivan", new Long(21)}, new Object[]{"Artem", new Long(20)}});
        checkequals(qf.getObjects("Test.TestObject","Test.TestClass","@f:first_name,@f:age", "-@f:password,-@f:age").list(), new Object[]{new Object[]{"Ivan", new Long(21)}, new Object[]{"Artem", new Long(20)}});
        
        testsearch("//*/*/obj/Test/TestClass[@f:first_name='Artem']/@doc:name",			new Object[]{"TestObject"});
		checkequals(qf.getObjects("*/*","Test.TestClass[@f:first_name='Artem']","@doc:name",null).list(), new Object[]{"TestObject"});
        testsearch("//*/*/obj/Test/TestClass[@f:first_name!='Artem']/@doc:fullName",			new Object[]{"Test.TestObject"});
		checkequals(qf.getObjects("*/*","Test.TestClass[@f:first_name!='Artem']","@doc:fullName",null).list(), new Object[]{"Test.TestObject"});
		testsearch("//*/*/obj/Test/TestClass[@f:first_name!='Artem' and @f:first_name!='Ivan']/@doc:web",	new Object[]{});
		checkequals(qf.getObjects("*/*","Test.TestClass[@f:first_name!='Artem' and @f:first_name!='Ivan']","@doc:fullName",null).list(), new Object[]{});
		
		testsearch("//*/*/obj/Test/TestClass[@f:first_name!='Artem' and @f:first_name!='Ivan']/@f:first_name",	new Object[]{});
		
        hb.endTransaction(getXWikiContext(), false);
	}
	
	public void test_jcr_contain() throws HibernateException, XWikiException, InvalidQueryException {
		XWikiHibernateStore hb = getXWiki().getHibernateStore();
		XWikiDocument doc = new XWikiDocument("Test", "Contains");
		BaseClass bclass = Utils.prepareClass(doc, "Test.Contains");
		bclass = Utils.prepareAdvancedClass(doc, "Test.Contains");
		ListClass propclass = (ListClass) bclass.get("dblist");
		propclass.setMultiSelect(true);
		propclass.setRelationalStorage(true);
        BaseObject obj1, obj = obj1 = Utils.prepareObject(doc, "Test.Contains");
        obj.put("driver", ((PropertyClass)bclass.get("driver")).fromString("1"));
        obj.put("category", ((PropertyClass)bclass.get("category")).fromString("1"));
        obj.put("category2", ((PropertyClass)bclass.get("category2")).fromString("1|2"));
        obj.put("category3", ((PropertyClass)bclass.get("category3")).fromString("1|2"));
        obj.put("dblist", ((PropertyClass)bclass.get("dblist")).fromString("XWikiUsers|test2"));
        doc.addObject("Test.Contains", obj1);
		hb.saveXWikiDoc(doc, getXWikiContext());
		
		getXWiki().flushCache();
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:category2, '1')]/@f:first_name", new Object[]{"Ludovic"});
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:category2, '2')]/@f:first_name", new Object[]{"Ludovic"});
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:category,  '2')]/@f:first_name", new Object[]{});
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,    'XWikiUsers')]/@f:first_name", new Object[]{"Ludovic"});
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,    'XWikiUsers1')]/@f:first_name", new Object[]{});
		testsearch("/*/*/obj/Test/Contains[not(jcr:contains(@f:dblist,'XWikiUsers'))]/(@f:first_name)", new Object[]{});
		testsearch("/*/*/obj/Test/Contains[not(jcr:contains(@f:dblist,'XWikiUsers1'))]/@f:first_name", new Object[]{"Ludovic"});
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,    'test2')]/@f:first_name", new Object[]{"Ludovic"});
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,'test2') and jcr:contains(@f:dblist,'XWikiUsers')]/@f:first_name", new Object[]{"Ludovic"});
		testsearch("/*/*/obj/Test/Contains[jcr:contains(@f:dblist,'test2') and jcr:contains(@f:dblist,'XWikiUsers1')]/@f:first_name", new Object[]{});
		testsearch("/*/*/obj/Test/Contains[not(jcr:contains(@f:dblist,'test2'))and not(jcr:contains(@f:dblist,'XWikiUsers'))]/@f:first_name", new Object[]{});
	}
	
	public void testReturnListS() throws XWikiException, InvalidQueryException {
		XWikiHibernateStore hb = getXWiki().getHibernateStore();
		XWikiDocument doc = new XWikiDocument("Test", "Contains");
		BaseClass bclass = Utils.prepareClass(doc, "Test.Contains");
		bclass = Utils.prepareAdvancedClass(doc, "Test.Contains");
		ListClass propclass = (ListClass) bclass.get("dblist");
		propclass.setMultiSelect(true);
		propclass.setRelationalStorage(true);
        BaseObject obj1, obj = obj1 = Utils.prepareObject(doc, "Test.Contains");
        obj.put("driver", ((PropertyClass)bclass.get("driver")).fromString("1"));
        obj.put("category", ((PropertyClass)bclass.get("category")).fromString("1"));
        obj.put("category2", ((PropertyClass)bclass.get("category2")).fromString("1|2"));
        obj.put("category3", ((PropertyClass)bclass.get("category3")).fromString("1|2"));
        obj.put("dblist", ((PropertyClass)bclass.get("dblist")).fromString("XWikiUsers|test2"));
        doc.addObject("Test.Contains", obj1);
		hb.saveXWikiDoc(doc, getXWikiContext());
		getXWiki().flushCache();
		testsearch("/*/*/obj/Test/Contains/@f:category",	new Object[]{"1"});
		testsearch("/*/*/obj/Test/Contains/@f:category2",	new Object[]{"1|2"});
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
		
		testsearch("//*/*/@name",			new Object[]{"TestObject", "TestObject"});
		checkequals(qf.getDocs("*.*", "@name", "").setDistinct(true).list(), new Object[]{"TestObject"});
		checkequals(qf.getDocs("*.*", "(@name,@name)", "").setDistinct(true).list(), new Object[]{new Object[]{"TestObject", "TestObject"}});
	}
}
