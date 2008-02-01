package org.xwiki.plugin.spacemanager.impl;

import java.util.*;

import org.jmock.Mock;
import org.jmock.core.stub.CustomStub;
import org.jmock.core.Invocation;
import org.xwiki.plugin.spacemanager.api.Space;
import org.xwiki.plugin.spacemanager.api.SpaceManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

public class SpaceManagerImplTest extends org.jmock.cglib.MockObjectTestCase {
	
	private XWikiContext context;
	private SpaceManagerImpl spaceManager;
	private XWiki xwiki;
	private Mock mockXWikiStore;
	private Mock mockXWikiVersioningStore;

    private Map docs = new HashMap();
    private List spaces = new ArrayList();

    private String displayTitle = "my nice space name";
    private String spaceName = "mynicespacename";

    /**
	 * Set up unit testing 
	 */
	protected void setUp() throws XWikiException {
		this.context = new XWikiContext();
        this.context.setUser("XWiki.TestUser");
        this.xwiki = new XWiki(new XWikiConfig(), this.context);

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {this.xwiki, this.context});
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
                          type = document.getStringValue(spaceManager.getSpaceClassName(), SpaceImpl.SPACE_TYPE);
                        } catch (Exception e) {}
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
        this.mockXWikiStore.stubs().method("search").will(
                  returnValue(spaces));

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
        this.xwiki.setDatabase("xwiki");

        this.context.setWiki(xwiki);
        this.context.setUser("XWiki.NotAdmin");
        this.context.setDatabase("xwiki");
        this.context.setMainXWiki("xwiki");

        this.spaceManager = new SpaceManagerImpl("spacemanager",SpaceManagerImpl.class.toString(),context);
        this.spaceManager.setMailNotification(false);
        this.spaceManager.init(context);

        XWikiDocument prefdoc = new XWikiDocument("XWiki", "XWikiPreferences");
        BaseObject obj = new BaseObject();
        obj.setName("XWiki.XWikiPreferences");
        obj.setClassName("XWiki.XWikiGlobalRights");
        obj.setStringValue("users", "XWiki.Admin");
        obj.setStringValue("groups", "");
        obj.setStringValue("levels", "admin,programming");
        obj.setIntValue("allow", 1);
        prefdoc.addObject("XWiki.XWikiGlobalRights", obj);
        this.xwiki.saveDocument(prefdoc, context);

        XWikiDocument doc = new XWikiDocument("Main", "WebHome");
        doc.setAuthor("xwiki:XWiki.Admin");
        context.setDoc(doc);
    }
	
	/**
	 * Tests the correct functioning of createSpace() and getSpace() methods
	 */
	public void testCreateGetSpace() throws SpaceManagerException {
		this.spaceManager.createSpace(displayTitle, context);

        Space space = this.spaceManager.getSpace(spaceName, context);
		assertFalse( "Space should not be new", space.isNew());
        assertEquals( "Space name is incorrect", spaceName, space.getSpaceName());
        assertEquals( "Space title is incorrect", displayTitle, space.getDisplayTitle());
        assertFalse("Space should not be marked deleted", space.isDeleted());

        List l = (List) this.spaceManager.getMembers(spaceName, context);
        assertEquals(l.get(0),context.getUser());
	}
	
	/**
	 * Tests the correct functioning of deleteSpace() method (in a create-delete-get sequence)
	 */
	public void testCreateDeleteSpace() throws SpaceManagerException {
		this.spaceManager.createSpace(displayTitle, context);
		this.spaceManager.deleteSpace(spaceName, false, context);

        Space space = this.spaceManager.getSpace(spaceName, context);
        assertTrue("Space should not be new", !space.isNew());
        assertTrue("Space should be marked deleted", space.isDeleted());
	}

    /**
     * Tests the correct functioning of the undeleteSpace() method (in a create-delete-undelete-get sequence)
     */
    public void testCreateDeleteUndeleteSpace() throws SpaceManagerException {
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
	public void testCreateGetSpaceNames() throws SpaceManagerException {
        // we should not have spaces at the start of our test
        assertEquals( new ArrayList(), this.spaceManager.getSpaceNames(0,0,context));

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

		assertEquals( list1, this.spaceManager.getSpaceNames(0,100,context));
		assertEquals( list1, this.spaceManager.getSpaceNames(0,0,context));

		this.spaceManager.deleteSpace(sn3, context);
		assertEquals( list2, this.spaceManager.getSpaceNames(0,100,context));
	}

    /**
     * Tests the correct functioning of deleteSpace() method (in a create-delete-get sequence)
     */
    public void testCreateDeleteSpace2() throws SpaceManagerException {
        this.spaceManager.createSpace(displayTitle, context);
        this.spaceManager.deleteSpace(displayTitle, false, context);

        Space space = this.spaceManager.getSpace(displayTitle, context);
        assertTrue("Space should be marked deleted", space.isDeleted());
    }

    /**
	 * Tests the correct functioning of getSpaces() method
	 */
	public void testCreateGetSpaces() throws SpaceManagerException {
        // we should not have spaces at the start of our test
        assertEquals( new ArrayList(), this.spaceManager.getSpaceNames(0,0,context));

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


        assertEquals( list1, this.spaceManager.getSpaces(0,100,context));
		assertEquals( list1, this.spaceManager.getSpaces(0,0,context));

		this.spaceManager.deleteSpace(sn3, context);
		assertEquals( list2, this.spaceManager.getSpaces(0,100,context));
	}

    public void testAddMember() throws SpaceManagerException {
		this.spaceManager.createSpace(displayTitle, context);
		Space s1 = this.spaceManager.getSpace(displayTitle, context);
		this.spaceManager.addMember(s1.getSpaceName(), "XWiki.cristi", context);
		List l = (List) this.spaceManager.getMembers(s1.getSpaceName(), context);
		assertEquals(l.get(0),"XWiki.cristi");

		List newusers = new ArrayList();
		newusers.add("XWiki.testuser1");
		newusers.add("XWiki.testuser2");
		this.spaceManager.addMembers(s1.getSpaceName(), newusers, context);

		List testlist = new ArrayList();
		testlist.add("XWiki.cristi");
        testlist.add("XWiki.testuser1");
        testlist.add("XWiki.testuser2");
		l = (List) this.spaceManager.getMembers(s1.getSpaceName(), context);
		assertEquals(testlist, l);
	}

    public void testAddAdmin() throws SpaceManagerException {
		this.spaceManager.createSpace(displayTitle, context);
		Space s1 = this.spaceManager.getSpace(displayTitle, context);
		this.spaceManager.addAdmin(s1.getSpaceName(), "XWiki.cristi", context);
		List l = (List) this.spaceManager.getAdmins(s1.getSpaceName(), context);
		assertEquals(l.get(0),"XWiki.cristi");

		List newusers = new ArrayList();
		newusers.add("XWiki.testuser1");
		newusers.add("XWiki.testuser2");
		this.spaceManager.addAdmins(s1.getSpaceName(), newusers, context);

		List testlist = new ArrayList();
        testlist.add("XWiki.cristi");
        testlist.add("XWiki.testuser1");
        testlist.add("XWiki.testuser2");
		l = (List) this.spaceManager.getAdmins(s1.getSpaceName(), context);
		assertEquals(testlist, l);
	}

    /**
	 * Tests the correct functioning of addUserToRole() and getUsertForRole()
	 */
    /*
    public void testAddUserToRole() throws SpaceManagerException {
		this.spaceManager.createSpace(displayTitle, context);
		Space s1 = this.spaceManager.getSpace(displayTitle, context);
		List roles = new ArrayList();
		roles.add("testrole");
		roles.add("testrole2");
		this.spaceManager.addUserToRole(s1, "cristi", roles, context);
		List l = this.spaceManager.getUsersForRole(s1, "testrole", context);
		assertEquals(l.get(0),"cristi");
		
		List newusers = new ArrayList();
		newusers.add("testuser1");
		newusers.add("testuser2");
		this.spaceManager.addUsersToRole(s1, newusers, roles, context);
		
		List testlist = new ArrayList();
		testlist.add("cristi");
		testlist.add(newusers);
		l = this.spaceManager.getUsersForRole(s1, "testrole", context);
		assertEquals(l, testlist);
	}
	
	public void testGetRoles() throws SpaceManagerException {
		List roles = new ArrayList();
		roles.add("testrole");
		roles.add("testrole2");
		
		List r1 = new ArrayList();
		r1.add("testrole");
		
		List r2 = new ArrayList();
		r1.add("testrole2");
		
		this.spaceManager.createSpace(displayTitle, context);
		Space s1 = this.spaceManager.getSpace(displayTitle, context);
		this.spaceManager.addUserToRole(s1, "cristi", r1, context);
		this.spaceManager.addUserToRole(s1, "cristi2", r2, context);
		
		assertEquals(this.spaceManager.getRoles(s1, context),roles);
	}
	*/
}
