/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author amelentev
 */

package com.xpn.xwiki.test.plugin.query;

import java.util.List;

import javax.jcr.query.InvalidQueryException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.plugin.query.IQueryFactory;
import com.xpn.xwiki.plugin.query.QueryPlugin;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.test.HibernateTestCase;
import com.xpn.xwiki.test.Utils;

public class QueryPluginApiTest extends HibernateTestCase {
	private static final Object[] EMPTY = new Object[]{};
	IQueryFactory qf;
	
	protected void setUp() throws Exception {		
		super.setUp();
		getXWiki().setPluginManager(new XWikiPluginManager("com.xpn.xwiki.plugin.query.QueryPlugin", getXWikiContext()));
        QueryPlugin plugin = (QueryPlugin) getXWiki().getPluginManager().getPlugin("query");
        qf = (IQueryFactory) plugin.getPluginApi(plugin, getXWikiContext());
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
	
	public void testQueryRights() throws XWikiException, InvalidQueryException {
		XWikiHibernateStore hb = getXWiki().getHibernateStore();		
		XWikiDocument doc0, doc = doc0 = new XWikiDocument("Class", "Class0");		
		BaseClass bclass1, bclass = bclass1 = Utils.prepareClass(doc, "Class.Class0");
		getXWiki().getStore().saveXWikiDoc(doc0, getXWikiContext());
		
		XWikiDocument doc1 = doc = new XWikiDocument("Test", "Test0");
		BaseClass bclass2 = Utils.prepareClass(doc, "Test.Test0");
		doc.setAuthor("Artem");
		BaseObject object1, object = object1 = new BaseObject();
        doc.setObject(bclass.getName(), 0, object);
        object.setClassName(bclass.getName());
        object.setName(doc.getFullName());
        object.put("first_name", ((PropertyClass)bclass.get("first_name")).fromString("Artem"));
        object.put("password", ((PropertyClass)bclass.get("password")).fromString("sesame"));
        getXWiki().getStore().saveXWikiDoc(doc1, getXWikiContext());
                
        /*hb.beginTransaction(getXWikiContext());        
        bclass1 = (BaseClass) hbload(BaseClass.class, new Long(bclass.getId()));
        object1 = (BaseObject) hbload(BaseObject.class, new Long(object1.getId()));
        doc1 = (XWikiDocument) hbload(XWikiDocument.class, new Long(doc1.getId()));*/        
        
        //hb.beginTransaction(false, getXWikiContext());
        //doc = (XWikiDocument) hb.getSession(getXWikiContext()).load(XWikiDocument.class, new Long(doc1.getId()));
        getXWiki().flushCache();
        //hb.beginTransaction(true, getXWikiContext());
        doc = getXWiki().getStore().loadXWikiDoc(new XWikiDocument("Test", "Test0"), getXWikiContext());
        Document secdoc1 = new Document(doc, getXWikiContext());
        //object = (BaseObject) hb.getSession(getXWikiContext()).load(BaseObject.class, new Integer(object1.getId()));
        Object secobj1	 = new com.xpn.xwiki.api.Object(object1, getXWikiContext());
        getXWiki().flushCache();
        testSearchXPnQl1("/Test/Test0", "select * from xwiki:document where fullName='Test.Test0'", secdoc1);
        testSearchXP("//doc/Test/Test0",								new Object[]{secdoc1});
        testSearchXPnQl1("//element(Test0, xwiki:document)",	"select * from xwiki:document where name='Test0'", secdoc1);
        testSearchXPnQl1("/Test/element(*, xwiki:document)", "select * from xwiki:document where web='Test'", secdoc1);
        //hb.endTransaction(getXWikiContext(), false);
        
        testSearchXPnQl("/Test/Test0/@name", "select name from xwiki:document where fullName='Test.Test0'", new Object[]{"Test0"});
        testSearchXP1("/element(Test0, xwiki:document)/@name",		"Test0");
        
        testSearchXPnQl1("/Test/Test0/@web", "select web from xwiki:document where name='Test0'", "Test");
        testSearchXPnQl1("/Test/Test0/(@name,@author)", "select name,author from xwiki:document where name='Test0'",	new Object[]{"Test0", "Artem"});
        testSearchXP("/Test/Test0/@fullName",						new Object[]{"Test.Test0"});
        testSearchXP("/Test/Test0/obj/Class/Class0",					new Object[]{secobj1});
        testSearchXP("/*/*/obj/Class/Class0",						new Object[]{secobj1});
        testSearchXPnQl1("//obj/*/*", "select * from xwiki:object", secobj1);
        testSearchXP("/Test/Test0/obj/Class/Class0/@name",				new Object[]{"Test.Test0"});        
        testSearchXP("/Test/Test0/obj/Class/Class0/@f:first_name",		new Object[]{"Artem"});
        testSearchXP("/Test/Test0/obj/Class/Class0/(@name,@f:first_name)",		new Object[]{new Object[]{"Test.Test0","Artem"}});
        testSearchXPnQl1("//obj/Class/Class0/(@name,@f:first_name)", "select name,f:first_name from xwiki:object where className='Class.Class0'",	new Object[]{"Test.Test0","Artem"});
        
        try {
        	testSearchXP("//Test/Test0/obj/Class/Class0/@f:password",	EMPTY);
        	assertTrue(false);
        } catch (XWikiException e) {
        	assertEquals(e.getCode(), XWikiException.ERROR_XWIKI_ACCESS_DENIED);
        }
        
        Utils.updateRight(getXWiki(), getXWikiContext(), "Test.Test0", "XWiki.XWikiGuest", "", "query,view", false, false);
        getXWiki().flushCache();
        testSearchXPnQl("/Test/Test0", "select * from xwiki:document where fullName='Test.Test0'", EMPTY);
        testSearchXP("//doc/Test/Test0",								EMPTY);
        testSearchXPnQl("//element(Test0, xwiki:document)",	"select * from xwiki:document where name='Test0'", EMPTY);
        testSearchXPnQl("/Test/element(*, xwiki:document)", "select * from xwiki:document where web='Test'", EMPTY);
        
        testSearchXPnQl("/Test/Test0/@name", "select name from xwiki:document where fullName='Test.Test0'", EMPTY);
        testSearchXP("/element(Test0, xwiki:document)/@name",		EMPTY);
        
        testSearchXPnQl("/Test/Test0/@web", "select web from xwiki:document where name='Test0'", EMPTY);
        testSearchXPnQl("/Test/Test0/(@name,@author)", "select name,author from xwiki:document where name='Test0'",	EMPTY);
        testSearchXP("/Test/Test0/@fullName",						EMPTY);
        testSearchXP("/Test/Test0/obj/Class/Class0",					EMPTY);
        testSearchXP("/*/*/obj/Class/Class0",							EMPTY);
        testSearchXPnQl("//obj/Class/Class0", "select * from xwiki:object where className='Class.Class0'", EMPTY);
        testSearchXP("/Test/Test0/obj/Class/Class0/@name",					EMPTY);        
        testSearchXP("/Test/Test0/obj/Class/Class0/@f:first_name",			EMPTY);
        testSearchXP("/Test/Test0/obj/Class/Class0/(@name,@f:first_name)",	EMPTY);
        testSearchXPnQl("//obj/Class/Class0/(@name,@f:first_name)", "select name,f:first_name from xwiki:object where className='Class.Class0'",	EMPTY);
                
        Utils.updateRight(getXWiki(), getXWikiContext(), "Test.Test0", "XWiki.XWikiGuest", "", "query", false, false);
        getXWiki().flushCache();
        testSearchXP("//Test/Test0",									EMPTY);
        testSearchXP("//Test/Test0/@name",							new Object[]{"Test0"});
        testSearchXP("//Test/Test0/@web",								EMPTY);
        testSearchXP("//Test/Test0/@fullName",						new Object[]{"Test.Test0"});
        testSearchXP("//Test/Test0/(@name,@author)",					EMPTY);
        testSearchXP("//Test/Test0/obj/Class/Class0",					EMPTY);
        testSearchXP("//Test/Test0/obj/Class/Class0/@name",				new Object[]{"Test.Test0"});
        testSearchXP("//Test/Test0/obj/Class/Class0/@classname",		EMPTY);
        testSearchXP("//Test/Test0/obj/Class/Class0/@f:first_name",		EMPTY);
        testSearchXP("//Test/Test0/obj/Class/Class0/(@name,@f:first_name)",		EMPTY);
        
        Utils.updateRight(getXWiki(), getXWikiContext(), "Test.Test0", "XWiki.XWikiGuest", "", "view", false, false);
        getXWiki().flushCache();
        doc1 = getXWiki().getStore().loadXWikiDoc(doc1, getXWikiContext());
        secdoc1 = new Document(doc1, getXWikiContext());
        testSearchXP("//Test/Test0",									new Object[]{secdoc1});
        testSearchXP("//Test/Test0/@name",							EMPTY);
        testSearchXP("//Test/Test0/@web",								new Object[]{"Test"});
        testSearchXP("//Test/Test0/@fullName",						EMPTY);
        testSearchXP("//Test/Test0/(@name,@creationDate)",			EMPTY);
        testSearchXP("//Test/Test0/obj/Class/Class0",					new Object[]{secobj1});
        testSearchXP("//Test/Test0/obj/Class/Class0/@name",				EMPTY);
        testSearchXP("//Test/Test0/obj/Class/Class0/@f:first_name",		new Object[]{"Artem"});
        testSearchXP("//Test/Test0/obj/Class/Class0/(@name,@f:first_name)",		EMPTY);        
	}
}
