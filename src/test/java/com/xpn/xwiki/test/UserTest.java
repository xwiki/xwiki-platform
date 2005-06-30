/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
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
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

public class UserTest extends HibernateTestCase {

    public static boolean inTest = false;

    public void prepareData() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("XWiki","LudovicDubost");
        BaseClass bclass = getXWiki().getUserClass(getXWikiContext());
        BaseObject bobj = new BaseObject();
        bobj.setName("XWiki.LudovicDubost");
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("fullname", "Ludovic Dubost");
        bobj.setStringValue("email", "ldubost@pobox.com");
        bobj.setStringValue("password", "toto");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ Ludovic Dubost HomePage");
        getXWiki().saveDocument(doc, getXWikiContext());

        doc = new XWikiDocument("XWiki","AdminGroup");
        bclass = getXWiki().getGroupClass(getXWikiContext());
        bobj = new BaseObject();
        bobj.setName("XWiki.AdminGroup");
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("member", "XWiki.LudovicDubost");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ AdminGroup");
        getXWiki().saveDocument(doc, getXWikiContext());

        doc = new XWikiDocument("Test","TestDoc");
        doc.setContent("---+ TestDoc");
        getXWiki().saveDocument(doc, getXWikiContext());

    }

    public void updateRight(String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        Utils.updateRight(getXWiki(), getXWikiContext(), fullname, user, group, level, allow, global);
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
        user.setFullName("John Doe", getXWikiContext());
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

    public void testUserAccessRead()  throws XWikiException {
        String docname = "Test.TestDoc";
        prepareData();
        getXWiki().flushCache();
        inTest = true;
        assertTrue("View Access should be allowed",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        getXWiki().flushCache();
        assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.Ludovic","","view", true, false);
        getXWiki().flushCache();
        assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
        getXWiki().flushCache();
        assertTrue("Edit Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
        getXWiki().flushCache();
        assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
    }

      public void testUserAccessReadWithAdmin()  throws XWikiException {
        String docname = "Test.TestDoc";
        prepareData();
        getXWiki().flushCache();
        inTest = true;

        // Give Admin right to LudovicDubost
        updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","admin", true, true);

        assertTrue("View Access should be allowed",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        getXWiki().flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                   getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
          updateRight(docname, "XWiki.Ludovic","","view", true, false);
          getXWiki().flushCache();
          assertTrue("View Access should be allowed even though doc is protected",
                     getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
        getXWiki().flushCache();
        assertTrue("Edit Access should be granted",
                   getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
        getXWiki().flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                   getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
    }

    public void testUserAccessReadWithWebGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give positive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", true, true);

      assertTrue("View Access should be allowed",
                 getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      assertTrue("Edit Access should be granted",
                 getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view", true,false);
      getXWiki().flushCache();
      assertTrue("View Access should be granted",
                 getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      assertTrue("Edit Access should be granted",
                 getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

    public void testUserAccessReadWithWebNegativeGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;

      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("Edit Access should be granted",
                 getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }



   public void testUserAccessReadWithXWikiGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", true, true);

      assertTrue("View Access should be allowed",
                 getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
       assertFalse("View Access should refused because there is a more restrictive right",
                   getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("View Access should be granted",
                 getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      assertTrue("Edit Access should be granted",
                 getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

    public void testUserAccessReadWithXWikiNegativeGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

  public void testUserAccessReadWithXWikiPositiveAndWebNegativeGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", true, true);
      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

    public void testUserAccessReadWithXWikiNegativeAndWebPositiveGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;

      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost","","view,edit", false, true);
      // Give postive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "XWiki.LudovicDubost","","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("View Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.LudovicDubost","","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }


    public void testGroupAccessRead()  throws XWikiException {
        String docname = "Test.TestDoc";
        prepareData();
        getXWiki().flushCache();
        inTest = true;
        assertTrue("View Access should be allowed",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));

        updateRight(docname, "", "XWiki.AdminGroup","view", true, false);
        getXWiki().flushCache();
        assertFalse("View Access should be refused",
                     getXWiki().getRightService().hasAccessLevel("view", "XWiki.JohnDoe", docname, getXWikiContext()));

        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        getXWiki().flushCache();
        assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));

        updateRight(docname, "XWiki.Ludovic","","view", true, false);
        getXWiki().flushCache();
        assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
        getXWiki().flushCache();
        assertTrue("Edit Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
        getXWiki().flushCache();
        assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
    }

      public void testGroupAccessReadWithAdmin()  throws XWikiException {
        String docname = "Test.TestDoc";
        prepareData();
        getXWiki().flushCache();
        inTest = true;

        // Give Admin right to LudovicDubost
        updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","admin", true, true);

        assertTrue("View Access should be allowed",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "XWiki.JohnDoe","","view", true, false);
        getXWiki().flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
          updateRight(docname, "XWiki.Ludovic","","view", true, false);
          getXWiki().flushCache();
          assertTrue("View Access should be allowed even though doc is protected",
                      getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
        getXWiki().flushCache();
        assertTrue("Edit Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
        updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
        getXWiki().flushCache();
        assertTrue("View Access should be allowed even though doc is protected",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
    }

    public void testGroupAccessReadWithWebGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give positive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("View Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

    public void testGroupAccessReadWithWebNegativeGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;

      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }



   public void testGroupAccessReadWithXWikiGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should refused because there is a more restrictive right",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("View Access should be granted",
                   getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

    public void testGroupAccessReadWithXWikiNegativeGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

  public void testGroupAccessReadWithXWikiPositiveAndWebNegativeGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;
      // Give positive XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", true, true);
      // Give negative Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", false, true);

      assertFalse("View Access should be refused because of global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

    public void testGroupAccessReadWithXWikiNegativeAndWebPositiveGlobalRight()  throws XWikiException {
      String docname = "Test.TestDoc";
      prepareData();
      getXWiki().flushCache();
      inTest = true;

      // Give negative XWiki Global right to LudovicDubost
      updateRight("XWiki.XWikiPreferences", "", "XWiki.AdminGroup","view,edit", false, true);
      // Give postive Web Global right to LudovicDubost
      updateRight("Test.WebPreferences", "", "XWiki.AdminGroup","view,edit", true, true);

      assertTrue("View Access should be allowed",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.JohnDoe","","view", true, false);
      getXWiki().flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "XWiki.Ludovic","","view", true, false);
      getXWiki().flushCache();
        assertFalse("View Access should refused because there is a more restrictive right",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view,edit", true,false);
      getXWiki().flushCache();
      assertTrue("View Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
      assertTrue("Edit Access should be granted",
                  getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
      updateRight(docname, "", "XWiki.AdminGroup","view", false, false);
      getXWiki().flushCache();
      assertFalse("View Access should be refused even though LudovicDubost has global right",
                  getXWiki().getRightService().hasAccessLevel("view", "XWiki.LudovicDubost", docname, getXWikiContext()));
  }

    public void testUserAccessReadInReadOnlyMode()  throws XWikiException {
         String docname = "Test.TestDoc";
         prepareData();
         updateRight(docname, "XWiki.LudovicDubost","","view,edit,comment,delete", true,false);
         getXWiki().flushCache();
         inTest = true;
        assertTrue("Edit Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
        assertTrue("Comment Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("comment", "XWiki.LudovicDubost", docname, getXWikiContext()));
        assertTrue("Delete Access should be granted",
                    getXWiki().getRightService().hasAccessLevel("delete", "XWiki.LudovicDubost", docname, getXWikiContext()));

        getXWiki().flushCache();
        // set read-only mode
        boolean ro = getXWiki().isReadOnly();
        getXWiki().setReadOnly(true);
        assertFalse("Edit Access should be denied",
                    getXWiki().getRightService().hasAccessLevel("edit", "XWiki.LudovicDubost", docname, getXWikiContext()));
        assertFalse("Comment Access should be denied",
                    getXWiki().getRightService().hasAccessLevel("comment", "XWiki.LudovicDubost", docname, getXWikiContext()));
        assertFalse("Delete Access should be denied",
                    getXWiki().getRightService().hasAccessLevel("delete", "XWiki.LudovicDubost", docname, getXWikiContext()));

        
        getXWiki().setReadOnly(ro);
     }

}
