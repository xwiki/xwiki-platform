package org.xwiki.plugin.spacemanager.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

public class SpaceImplTest extends org.jmock.cglib.MockObjectTestCase {
	
	private XWikiContext context;
	private SpaceManagerImpl spaceManager;
	private SpaceImpl space;
	private XWiki xwiki;
	private Mock mockXWikiStore;
	private Mock mockXWikiVersioningStore;
	
	private Map docs = new HashMap();
    private List spaces = new ArrayList();

    private String displayTitle = "my nice space name";
    private String spaceName = "mynicespacename";
	
	protected void setUp() throws XWikiException {
		this.context = new XWikiContext();
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
        this.mockXWikiStore.stubs().method("getTranslationList").will(
            returnValue(Collections.EMPTY_LIST));
        this.mockXWikiStore.stubs().method("search").will(
                  returnValue(spaces));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class,
            XWikiContext.class}, new Object[] {this.xwiki, this.context});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(
            returnValue(null));
        this.mockXWikiStore.stubs().method("searchDocumentsNames").will(
                  returnValue(new ArrayList()));
        

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore
            .proxy());
        this.context.setWiki(xwiki);
        this.context.setDatabase("xwiki");
        this.context.setMainXWiki("xwiki");
        this.context.setUser("XWiki.Admin");

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

        this.space = (SpaceImpl) this.spaceManager.createSpace(displayTitle, context);
	}
	
	public void testDisplayTitle(){
		assertEquals( displayTitle, this.space.getDisplayTitle() );
		String title = "Some Test Title";
		this.space.setDisplayTitle( title );
		assertEquals( title, this.space.getDisplayTitle() );
	}
	
	public void testDescription(){
		String desc = "Space description";
		this.space.setDescription(desc);
		assertEquals( desc, this.space.getDescription() );
	}
	
	public void testPreferences(){
		/**
		 * @todo we need to know who will save preferences first
		 */
	}
	
	public void testHomeShortcutURL(){
		String url = "http://groupname.domainname.org";
		this.space.setHomeShortcutURL(url);
		assertEquals( url, this.space.getHomeShortcutURL() );
	}
}