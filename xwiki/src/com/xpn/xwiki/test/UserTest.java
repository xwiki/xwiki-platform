
package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.XWikiUserProvider;
import com.xpn.xwiki.user.XWikiGroupProvider;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.opensymphony.user.*;

import java.util.Collection;

import net.sf.hibernate.HibernateException;

/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 26 janv. 2004
 * Time: 00:59:44
 */

public class UserTest extends TestCase {

    private static XWiki xwiki;
    private static XWikiContext context;
    private static UserManager um;

    public UserTest() throws XWikiException {
        context = new XWikiContext();
        xwiki = new XWiki("./xwiki.cfg", context);
        context.setWiki(xwiki);
    }


    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheInterface)
            return (XWikiHibernateStore)((XWikiCacheInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void setUp() throws HibernateException {
        StoreHibernateTest.cleanUp(getHibStore());
        um = UserManager.getInstance();
        getUserProvider(um).setxWiki(xwiki);
        getGroupProvider(um).setxWiki(xwiki);
        xwiki.flushCache();
    }

    public void prepareData() throws XWikiException {
        XWikiSimpleDoc doc = new XWikiSimpleDoc("XWiki","LudovicDubost");
        BaseClass bclass = xwiki.getUserClass();
        BaseObject bobj = new BaseObject();
        bobj.setName("XWiki.LudovicDubost");
        bobj.setxWikiClass(bclass);
        bobj.setStringValue("fullname", "Ludovic Dubost");
        bobj.setStringValue("email", "ldubost@pobox.com");
        bobj.setStringValue("password", "toto");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ Ludovic Dubost HomePage");
        xwiki.saveDocument(doc);

        doc = new XWikiSimpleDoc("XWiki","AdminGroup");
        bclass = xwiki.getGroupClass();
        bobj = new BaseObject();
        bobj.setName("XWiki.AdminGroup");
        bobj.setxWikiClass(bclass);
        bobj.setStringValue("member", "XWiki.LudovicDubost");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ AdminGroup");
        xwiki.saveDocument(doc);
    }

    public XWikiUserProvider getUserProvider(UserManager um) {
        return (XWikiUserProvider) um.getProfileProviders().toArray()[0];
    }

    public XWikiGroupProvider getGroupProvider(UserManager um) {
        return (XWikiGroupProvider) um.getAccessProviders().toArray()[0];
    }

    public void testUserList() throws XWikiException, ImmutableException, DuplicateEntityException {
        Collection users = um.getUsers();
        assertEquals("There should be no users", 0, users.size());
        prepareData();
        users = um.getUsers();
        assertEquals("There should be one user", 1, users.size());
        testUser((User)users.toArray()[0]);
    }

    public void testUserCreate() throws XWikiException, ImmutableException, DuplicateEntityException {
        Collection users = um.getUsers();
        assertEquals("There should be no users", 0, users.size());
        prepareData();
        users = um.getUsers();
        assertEquals("There should be one user", 1, users.size());
        User user2 = um.createUser("XWiki.JohnDoe");
        users = um.getUsers();
        assertEquals("There should be two user", 2, users.size());
    }

    public void testUserRead()  throws XWikiException, ImmutableException, EntityNotFoundException {
        prepareData();
        User user = um.getUser("XWiki.LudovicDubost");
        testUser(user);
    }

    public void testUser(User user)  throws XWikiException, ImmutableException {
        assertEquals("User name is incorrect", "XWiki.LudovicDubost", user.getName());
        assertEquals("User email is incorrect","ldubost@pobox.com", user.getEmail() );
        assertEquals("User full name is incorrect","Ludovic Dubost",user.getFullName());
        assertFalse("Authentication should fail", user.authenticate("titi"));
        assertTrue("Authentication should pass", user.authenticate("toto"));
        user.setPassword("titi");
        user.setFullName("John Doe");
        user.store();
        assertTrue("Authentication should fail", user.authenticate("titi"));
        assertFalse("Authentication should pass", user.authenticate("toto"));
        assertEquals("User full name is incorrect","John Doe",user.getFullName());
    }

    public void testGroupList() throws XWikiException, ImmutableException, DuplicateEntityException {
        Collection groups = um.getGroups();
        assertEquals("There should be no groups", 0, groups.size());
        prepareData();
        groups = um.getGroups();
        assertEquals("There should be one group", 1, groups.size());
    }

    public void testGroupCreate() throws XWikiException, ImmutableException, DuplicateEntityException {
        Collection groups = um.getGroups();
        assertEquals("There should be no groups", 0, groups.size());
        prepareData();
        groups = um.getGroups();
        assertEquals("There should be one group", 1, groups.size());
        Group group2 = um.createGroup("XWiki.AnotherGroup");
        groups = um.getGroups();
        assertEquals("There should be two groups", 2, groups.size());
    }

    public void testGroupSearch() throws XWikiException, EntityNotFoundException, ImmutableException, DuplicateEntityException {
        prepareData();
        User user = um.getUser("XWiki.LudovicDubost");
        Group group = um.getGroup("XWiki.AdminGroup");
        Collection groups = user.getGroups();
        assertEquals("There should be one group", 1, user.getGroups().size());
        assertTrue("This should work", user.addToGroup(group));
        assertTrue("This should work", user.inGroup(group));
        User user2 = um.createUser("XWiki.JohnDoe");
        Group group2 = um.createGroup("XWiki.AnotherGroup");
        assertEquals("There should be one group in user", 1, user.getGroups().size());
        assertEquals("There should be no group in user2", 0, user2.getGroups().size());
        assertTrue("This should work", user.inGroup(group));
        assertFalse("This should fail", user.inGroup(group2));
        assertFalse("This should fail", user2.inGroup(group2));

        // Adding the second group to the second user
        assertTrue("This should work", user2.addToGroup(group2));
        assertEquals("There should be one group in user", 1, user.getGroups().size());
        assertEquals("There should be one group in user2", 1, user2.getGroups().size());
        assertTrue("This should work", user.inGroup(group));
        assertFalse("This should fail", user.inGroup(group2));
        assertTrue("This should work", user2.inGroup(group2));

        // Adding the second group to the first user
        assertTrue("This should work", user.addToGroup(group2));
        assertEquals("There should be one group in user", 2, user.getGroups().size());
        assertEquals("There should be one group in user2", 1, user2.getGroups().size());
        assertTrue("This should work", user.inGroup(group));
        assertTrue("This should work", user.inGroup(group2));
        assertTrue("This should work", user2.inGroup(group2));

        // Removing the second group to the second user
        assertTrue("This should work", user.removeFromGroup(group2));
        assertEquals("There should be one group in user", 1,user.getGroups().size());
        assertEquals("There should be one group in user2", 1, user2.getGroups().size());
        assertTrue("This should work", user.inGroup(group));
        assertFalse("This should fail", user.inGroup(group2));
        assertTrue("This should work", user2.inGroup(group2));
    }
}
