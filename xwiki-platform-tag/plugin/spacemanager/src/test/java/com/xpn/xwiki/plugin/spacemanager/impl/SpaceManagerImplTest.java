/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.spacemanager.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin;
import com.xpn.xwiki.plugin.spacemanager.api.Space;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManagerException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

/**
 * @version $Id: $
 */
public class SpaceManagerImplTest extends AbstractBridgedXWikiComponentTestCase
{

    private XWikiContext context;

    private SpaceManagerImpl spaceManager;

    private XWiki xwiki;

    private XWikiConfig config;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Mock mockGroupService;
    
    private Mock mockQueryManager;

    private Map docs = new HashMap();

    private List spaces = new ArrayList();

    private String displayTitle = "my nice space name";

    private String spaceName = "mynicespacename";

    private Map listGroupsForUserReturnValues = new HashMap();

    /**
     * Set up unit testing
     * 
     * @throws Exception
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        this.xwiki = new XWiki();
        this.context = getContext();
        this.context.setUser("XWiki.TestUser");
        this.config = new XWikiConfig();

        this.xwiki.setDatabase("xwiki");

        this.context.setWiki(xwiki);
        this.context.setUser("XWiki.NotAdmin");
        this.context.setDatabase("xwiki");
        this.context.setMainXWiki("xwiki");
        
        // for protected spaces test
        config.put("xwiki.spacemanager.protectedsubspaces", "Documentation");

        xwiki.setConfig(config);
        xwiki.setNotificationManager(new XWikiNotificationManager());
        
        xwiki.setPluginManager(new XWikiPluginManager());
        xwiki.getPluginManager().addPlugin("rightsmanager",RightsManagerPlugin.class.getName(),context);

        this.mockQueryManager = mock(QueryManager.class);
        
        Mock mockNamedQuery = mock(Query.class);
        mockNamedQuery.stubs().method("bindValue").will(returnValue((Query)mockNamedQuery.proxy()));
        mockNamedQuery.stubs().method("execute").will(returnValue(new ArrayList()));
        
        this.mockQueryManager.stubs().method("getNamedQuery").will(returnValue((Query) mockNamedQuery.proxy()));
        
        Mock mockCreatedQuery = mock(Query.class);
        mockCreatedQuery.stubs().method("bindValue").will(returnValue((Query)mockCreatedQuery.proxy()));
        mockCreatedQuery.stubs().method("execute").will(returnValue(spaces));
        mockCreatedQuery.stubs().method("setOffset");
        mockCreatedQuery.stubs().method("setLimit");
        
        this.mockQueryManager.stubs().method("createQuery").will(returnValue((Query) mockCreatedQuery.proxy()));
        
        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {this.xwiki, this.context});
        this.mockXWikiStore.expects(once()).method("executeWrite");
        this.mockXWikiStore.stubs().method("getQueryManager").will(
        		returnValue((QueryManager) this.mockQueryManager.proxy()));
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);

                    if (docs.containsKey(shallowDoc.getFullName())) {
                        return (XWikiDocument) docs.get(shallowDoc.getFullName());
                    } else {
                        return shallowDoc;
                    }
                }
            });
        this.mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    docs.put(document.getFullName(), document);
                    if ((document.getName().equals("WebPreferences"))) {
                        String type = null;
                        try {
                            type =
                                document.getStringValue(spaceManager.getSpaceClassName(),
                                    SpaceImpl.SPACE_TYPE);
                        } catch (Exception e) {
                        }
                        if (spaceManager.getSpaceTypeName().equals(type)) {
                            if (!spaces.contains("" + document.getSpace()))
                                spaces.add("" + document.getSpace());
                        }
                        if ("deleted".equals(type))
                            spaces.remove("" + document.getSpace());
                    }
                    return null;
                }
            });

        this.mockXWikiStore.stubs().method("saveXWikiObject");

        this.mockXWikiStore.stubs().method("getTranslationList").will(
            returnValue(Collections.EMPTY_LIST));
        this.mockXWikiStore.stubs().method("search").will(returnValue(spaces));

        this.mockXWikiStore.stubs().method("searchDocumentsNames").will(
            returnValue(new ArrayList()));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class,
            XWikiContext.class}, new Object[] {this.xwiki, this.context});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(
            returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore
            .proxy());

        this.spaceManager =
            new SpaceManagerImpl("spacemanager", SpaceManagerImpl.class.toString(), context);
        this.spaceManager.setMailNotification(false);
        this.spaceManager.init(context);

        XWikiDocument prefdoc = new XWikiDocument("XWiki", "XWikiPreferences");
        BaseObject obj = new BaseObject();
        obj.setName("XWiki.XWikiPreferences");
        obj.setClassName("XWiki.XWikiGlobalRights");
        obj.setLargeStringValue("users", "XWiki.Admin");
        obj.setLargeStringValue("groups", "");
        obj.setLargeStringValue("levels", "admin,programming");
        obj.setIntValue("allow", 1);
        prefdoc.addObject("XWiki.XWikiGlobalRights", obj);
        this.xwiki.saveDocument(prefdoc, context);

        XWikiDocument doc = new XWikiDocument("Main", "WebHome");
        doc.setAuthor("xwiki:XWiki.Admin");
        doc.setContentAuthor("xwiki:XWiki.Admin");
        context.setDoc(doc);
    }

    /**
     * Tests the correct functioning of createSpace() and getSpace() methods
     */
    public void testCreateGetSpace() throws SpaceManagerException
    {        
     	
        this.spaceManager.createSpace(displayTitle, context);

        Space space = this.spaceManager.getSpace(spaceName, context);
        assertFalse("Space should not be new", space.isNew());
        assertEquals("Space name is incorrect", spaceName, space.getSpaceName());
        assertEquals("Space title is incorrect", displayTitle, space.getDisplayTitle());
        assertFalse("Space should not be marked deleted", space.isDeleted());
    }

    /**
     * Tests the correct functioning of deleteSpace() method (in a create-delete-get sequence)
     */
    public void testCreateDeleteSpace() throws SpaceManagerException
    {
        this.spaceManager.createSpace(displayTitle, context);
        this.spaceManager.deleteSpace(spaceName, false, context);

        Space space = this.spaceManager.getSpace(spaceName, context);
        assertTrue("Space should not be new", !space.isNew());
        assertTrue("Space should be marked deleted", space.isDeleted());
    }

    /**
     * Tests the correct functioning of the undeleteSpace() method (in a create-delete-undelete-get
     * sequence)
     */
    public void testCreateDeleteUndeleteSpace() throws SpaceManagerException
    {
        this.spaceManager.createSpace(displayTitle, context);
        this.spaceManager.deleteSpace(spaceName, false, context);

        Space space = this.spaceManager.getSpace(spaceName, context);
        assertTrue("Space should not be new", !space.isNew());
        assertTrue("Space should be marked deleted", space.isDeleted());

        this.spaceManager.undeleteSpace(spaceName, context);

        space = this.spaceManager.getSpace(spaceName, context);
        assertFalse("Space should not be marked deleted", space.isDeleted());
    }

    /**
     * Tests the correct functioning of getSpaces() method
     */
    public void testCreateGetSpaceNames() throws SpaceManagerException
    {
        // we should not have spaces at the start of our test
        assertEquals(new ArrayList(), this.spaceManager.getSpaceNames(0, 0, context));

        String sn1 = "name1";
        String sn2 = "othername";
        String sn3 = "thirdspacename";
        this.spaceManager.createSpace(sn1, context);
        Space s1 = this.spaceManager.getSpace(sn1, context);
        this.spaceManager.createSpace(sn2, context);
        Space s2 = this.spaceManager.getSpace(sn2, context);
        this.spaceManager.createSpace(sn3, context);
        Space s3 = this.spaceManager.getSpace(sn3, context);

        List list1 = new ArrayList();
        list1.add(sn1);
        list1.add(sn2);
        list1.add(sn3);

        List list2 = new ArrayList();
        list2.add(sn1);
        list2.add(sn2);

        assertEquals(list1, this.spaceManager.getSpaceNames(0, 100, context));
        assertEquals(list1, this.spaceManager.getSpaceNames(0, 0, context));

        this.spaceManager.deleteSpace(sn3, context);
        assertEquals(list2, this.spaceManager.getSpaceNames(0, 100, context));
    }

    /**
     * Tests the correct functioning of deleteSpace() method (in a create-delete-get sequence)
     */
    public void testCreateDeleteSpace2() throws SpaceManagerException
    {
        this.spaceManager.createSpace(displayTitle, context);
        this.spaceManager.deleteSpace(spaceName, false, context);

        Space space = this.spaceManager.getSpace(spaceName, context);
        assertTrue("Space should be marked deleted", space.isDeleted());
    }

    /**
     * Tests the correct functioning of getSpaces() method
     */
    public void testCreateGetSpaces() throws SpaceManagerException
    {
        // we should not have spaces at the start of our test
        assertEquals(new ArrayList(), this.spaceManager.getSpaceNames(0, 0, context));

        String sn1 = "name1";
        String sn2 = "othername";
        String sn3 = "thirdspacename";
        this.spaceManager.createSpace(sn1, context);
        Space s1 = this.spaceManager.getSpace(sn1, context);
        this.spaceManager.createSpace(sn2, context);
        Space s2 = this.spaceManager.getSpace(sn2, context);
        this.spaceManager.createSpace(sn3, context);
        Space s3 = this.spaceManager.getSpace(sn3, context);

        List list1 = new ArrayList();
        list1.add(s1);
        list1.add(s2);
        list1.add(s3);

        List list2 = new ArrayList();
        list2.add(s1);
        list2.add(s2);

        assertEquals(list1, this.spaceManager.getSpaces(0, 100, context));
        assertEquals(list1, this.spaceManager.getSpaces(0, 0, context));

        this.spaceManager.deleteSpace(sn3, context);
        assertEquals(list2, this.spaceManager.getSpaces(0, 100, context));
    }

    public void testSubSpaceRights() throws SpaceManagerException, XWikiException
    {
        this.mockGroupService = mock(XWikiGroupService.class);
        this.mockGroupService.stubs().method("listGroupsForUser").will(
            new CustomStub("Implements XWikiGroupService.listGroupsForUser")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    String user = (String) invocation.parameterValues.get(0);
                    List list = (List) listGroupsForUserReturnValues.get(user);
                    if (list == null)
                        return new ArrayList();
                    else
                        return list;
                }
            });
        this.mockGroupService.stubs().method("getAllGroupsNamesForMember").will(
            new CustomStub("Implements XWikiGroupService.getAllGroupsNamesForMember")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    String user = (String) invocation.parameterValues.get(0);
                    List list = (List) listGroupsForUserReturnValues.get(user);
                    if (list == null)
                        return new ArrayList();
                    else
                        return list;
                }
            });
        this.xwiki.setGroupService((XWikiGroupService) this.mockGroupService.proxy());

        this.spaceManager.createSpace(displayTitle, context);
        Space s1 = this.spaceManager.getSpace(spaceName, context);
        this.spaceManager.addMember(s1.getSpaceName(), "XWiki.cristi", context);
        List cristiGroups = new ArrayList();
        cristiGroups.add("mynicespacename.MemberGroup");
        listGroupsForUserReturnValues.put("XWiki.cristi", cristiGroups);
        s1.setPolicy("open");
        this.spaceManager.setSpaceRights(s1, context);
        assertTrue("rights should be true for guest", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.XWikiGuest", "Documentation_mynicespacename.WebHome",
                context));
        assertTrue("rights should be true for cristi", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.cristi", "Documentation_mynicespacename.WebHome",
                context));
        this.spaceManager.updateSpaceRights(s1, "open", "closed", context);
        assertFalse("rights should be false for guest", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.XWikiGuest", "Documentation_mynicespacename.WebHome",
                context));
        assertTrue("rights should be true for cristi", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.cristi", "Documentation_mynicespacename.WebHome",
                context));
        this.spaceManager.updateSpaceRights(s1, "closed", "open", context);
        assertTrue("rights should be true for guest", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.XWikiGuest", "Documentation_mynicespacename.WebHome",
                context));
        assertTrue("rights should be true for cristi", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.cristi", "Documentation_mynicespacename.WebHome",
                context));
    }

    public void testSubSpaceRights2() throws SpaceManagerException, XWikiException
    {
        this.mockGroupService = mock(XWikiGroupService.class);
        this.mockGroupService.stubs().method("listGroupsForUser").will(
            new CustomStub("Implements XWikiGroupService.listGroupsForUser")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    String user = (String) invocation.parameterValues.get(0);
                    List list = (List) listGroupsForUserReturnValues.get(user);
                    if (list == null)
                        return new ArrayList();
                    else
                        return list;
                }
            });
        this.mockGroupService.stubs().method("getAllGroupsNamesForMember").will(
            new CustomStub("Implements XWikiGroupService.getAllGroupsNamesForMember")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    String user = (String) invocation.parameterValues.get(0);
                    List list = (List) listGroupsForUserReturnValues.get(user);
                    if (list == null)
                        return new ArrayList();
                    else
                        return list;
                }
            });
        this.xwiki.setGroupService((XWikiGroupService) this.mockGroupService.proxy());

        this.spaceManager.createSpace(displayTitle, context);
        Space s1 = this.spaceManager.getSpace(spaceName, context);
        this.spaceManager.addMember(s1.getSpaceName(), "XWiki.cristi", context);
        List cristiGroups = new ArrayList();
        cristiGroups.add("mynicespacename.MemberGroup");
        listGroupsForUserReturnValues.put("XWiki.cristi", cristiGroups);
        s1.setPolicy("closed");
        this.spaceManager.setSpaceRights(s1, context);
        assertFalse("rights should be false for guest", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.XWikiGuest", "Documentation_mynicespacename.WebHome",
                context));
        assertTrue("rights should be true for cristi", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.cristi", "Documentation_mynicespacename.WebHome",
                context));
        this.spaceManager.updateSpaceRights(s1, "closed", "open", context);
        assertTrue("rights should be true for guest", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.XWikiGuest", "Documentation_mynicespacename.WebHome",
                context));
        assertTrue("rights should be true for cristi", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.cristi", "Documentation_mynicespacename.WebHome",
                context));
        this.spaceManager.updateSpaceRights(s1, "open", "closed", context);
        assertFalse("rights should be false for guest", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.XWikiGuest", "Documentation_mynicespacename.WebHome",
                context));
        assertTrue("rights should be true for cristi", this.xwiki.getRightService()
            .hasAccessLevel("view", "XWiki.cristi", "Documentation_mynicespacename.WebHome",
                context));
    }

}
