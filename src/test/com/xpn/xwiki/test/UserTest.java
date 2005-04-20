
package com.xpn.xwiki.test;

import com.opensymphony.module.access.NotFoundException;
import com.opensymphony.module.access.provider.osuser.OSUserUserProvider;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import junit.framework.TestCase;
import org.hibernate.HibernateException;

/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 * User: Ludovic Dubost
 * Date: 26 janv. 2004
 * Time: 00:59:44
 */

public class UserTest extends TestCase {

    private XWiki xwiki;
    private XWikiContext context;

    public static boolean inTest = false;

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void setUp() throws HibernateException, XWikiException {
        OSUserUserProvider toto = new OSUserUserProvider();
        context = new XWikiContext();
        context.setDatabase("xwikitest");
        xwiki = new XWiki("./xwiki.cfg", context);
        xwiki.setDatabase("xwikitest");
        context.setWiki(xwiki);
        StoreHibernateTest.cleanUp(getHibStore(), context);
        xwiki.flushCache();
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public void prepareData() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("XWiki","LudovicDubost");
        BaseClass bclass = xwiki.getUserClass(context);
        BaseObject bobj = new BaseObject();
        bobj.setName("XWiki.LudovicDubost");
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("fullname", "Ludovic Dubost");
        bobj.setStringValue("email", "ldubost@pobox.com");
        bobj.setStringValue("password", "toto");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ Ludovic Dubost HomePage");
        xwiki.saveDocument(doc, context);

        doc = new XWikiDocument("XWiki","AdminGroup");
        bclass = xwiki.getGroupClass(context);
        bobj = new BaseObject();
        bobj.setName("XWiki.AdminGroup");
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("member", "XWiki.LudovicDubost");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ AdminGroup");
        xwiki.saveDocument(doc, context);

        doc = new XWikiDocument("Test","TestDoc");
        doc.setContent("---+ TestDoc");
        xwiki.saveDocument(doc, context);

    }

    public void updateRight(String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        Utils.updateRight(xwiki, context, fullname, user, group, level, allow, global);
    }

    /*
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
        user.setFullName("John Doe", context);
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
    } */

    public void testUserAccessRead()  throws XWikiException, NotFoundException {
        String docname = "Test.TestDoc";
        prepareData();
        xwiki.flushCache();
        inTest = true;
        assertTrue("View Access should be allowed",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        xwiki.flushCache();
        assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.Ludovic","","view", true, false);
        xwiki.flushCache();
        assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
        xwiki.flushCache();
        assertTrue("Edit Access should be granted",
                    xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
        xwiki.flushCache();
        assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
    }

      public void testUserAccessReadWithAdmin()  throws XWikiException, NotFoundException {
        String docname = "Test.TestDoc";
        prepareData();
        xwiki.flushCache();
        inTest = true;

        // Give Admin right to LudovicDubost
        updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","admin", true, true);

        assertTrue("View Access should be allowed",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        xwiki.flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
          updateRight(docname, "XWiki.Ludovic","","view", true, false);
          xwiki.flushCache();
          assertTrue("View Access should be allowed even though doc is protected",
                      xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
        xwiki.flushCache();
        assertTrue("Edit Access should be granted",
                    xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
        xwiki.flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
    }

    public void testUserAccessReadWithWebGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give positive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      assertTrue("Edit Access should be granted",
                    xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view", true,false);
      xwiki.flushCache();
      assertTrue("View Access should be granted",
                      xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

    public void testUserAccessReadWithWebNegativeGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;

      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }



   public void testUserAccessReadWithXWikiGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
       assertFalse("View Access should refused because there is a more restrictive right",
                      xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("View Access should be granted",
                   xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

    public void testUserAccessReadWithXWikiNegativeGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

  public void testUserAccessReadWithXWikiPositiveAndWebNegativeGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", true, true);
      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

    public void testUserAccessReadWithXWikiNegativeAndWebPositiveGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;

      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", false, true);
      // Give postive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("View Access should be granted",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }


    public void testGroupAccessRead()  throws XWikiException, NotFoundException {
        String docname = "Test.TestDoc";
        prepareData();
        xwiki.flushCache();
        inTest = true;
        assertTrue("View Access should be allowed",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));

        updateRight(docname, "", "XWiki.AdminGroup","view", true, false);
        xwiki.flushCache();
        assertFalse("View Access should be refused",
                     xwiki.getRightService().hasAccessLevel("view", "XWiki.JohnDoe", docname, context));

        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        xwiki.flushCache();
        assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));

        updateRight(docname, "XWiki.Ludovic","","view", true, false);
        xwiki.flushCache();
        assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
        xwiki.flushCache();
        assertTrue("Edit Access should be granted",
                    xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
        xwiki.flushCache();
        assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
    }

      public void testGroupAccessReadWithAdmin()  throws XWikiException, NotFoundException {
        String docname = "Test.TestDoc";
        prepareData();
        xwiki.flushCache();
        inTest = true;

        // Give Admin right to LudovicDubost
        updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","admin", true, true);

        assertTrue("View Access should be allowed",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        xwiki.flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
          updateRight(docname, "XWiki.Ludovic","","view", true, false);
          xwiki.flushCache();
          assertTrue("View Access should be allowed even though doc is protected",
                      xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
        xwiki.flushCache();
        assertTrue("Edit Access should be granted",
                    xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
        updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
        xwiki.flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
    }

    public void testGroupAccessReadWithWebGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give positive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("View Access should be granted",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

    public void testGroupAccessReadWithWebNegativeGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;

      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }



   public void testGroupAccessReadWithXWikiGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("View Access should be granted",
                   xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

    public void testGroupAccessReadWithXWikiNegativeGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

  public void testGroupAccessReadWithXWikiPositiveAndWebNegativeGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", true, true);
      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

    public void testGroupAccessReadWithXWikiNegativeAndWebPositiveGlobalRight()  throws XWikiException, NotFoundException {
      String docname = "Test.TestDoc";
      prepareData();
      xwiki.flushCache();
      inTest = true;

      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", false, true);
      // Give postive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      xwiki.flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      xwiki.flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      xwiki.flushCache();
      assertTrue("View Access should be granted",
                    xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
      assertTrue("Edit Access should be granted",
                  xwiki.getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, context));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      xwiki.flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  xwiki.getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, context));
  }

}
